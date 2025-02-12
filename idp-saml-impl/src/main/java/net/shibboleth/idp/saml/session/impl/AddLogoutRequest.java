/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.saml.session.impl;

import java.time.Instant;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.SessionIndex;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.idp.saml.session.SAML2SPSession;
import net.shibboleth.idp.session.context.LogoutPropagationContext;
import net.shibboleth.profile.config.navigate.IdentifierGenerationStrategyLookupFunction;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;

/**
 * Action that creates a {@link LogoutRequest} based on an {@link SAML2SPSession} in a
 * {@link LogoutPropagationContext} and sets it as the message returned by
 * {@link ProfileRequestContext#getOutboundMessageContext()}.
 * 
 * <p>If an issuer value is returned via a lookup strategy, then it's set as the Issuer of the message.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#MESSAGE_PROC_ERROR}
 * 
 * @post ProfileRequestContext.getOutboundMessageContext().getMessage() != null
 */
public class AddLogoutRequest extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(AddLogoutRequest.class);
    
    /** Overwrite an existing message? */
    private boolean overwriteExisting;

    /** Include SessionIndex in the request? */
    private boolean includeSessionIndex;

    /** Strategy used to locate the {@link IdentifierGenerationStrategy} to use. */
    @Nonnull private Function<ProfileRequestContext,IdentifierGenerationStrategy> idGeneratorLookupStrategy;

    /** Strategy used to obtain the response issuer value. */
    @Nullable private Function<ProfileRequestContext,String> issuerLookupStrategy;

    /** Logout propagation context lookup strategy. */
    @Nonnull private Function<ProfileRequestContext,LogoutPropagationContext> logoutPropContextLookupStrategy;
    
    /** The generator to use. */
    @NonnullBeforeExec private IdentifierGenerationStrategy idGenerator;

    /** The {@link SAML2SPSession} to base the inbound context on. */
    @NonnullBeforeExec private SAML2SPSession saml2Session;

    /** EntityID to populate into Issuer element. */
    @Nullable private String issuerId;
    
    /** Constructor. */
    public AddLogoutRequest() {
        // Default strategy is a 16-byte secure random source.
        idGeneratorLookupStrategy = new IdentifierGenerationStrategyLookupFunction();
        
        logoutPropContextLookupStrategy = new ChildContextLookup<>(LogoutPropagationContext.class);
        includeSessionIndex = true;
    }
    
    /**
     * Set whether to overwrite an existing message.
     * 
     * @param flag flag to set
     */
    public void setOverwriteExisting(final boolean flag) {
        checkSetterPreconditions();
        overwriteExisting = flag;
    }

    /**
     * Set whether to include a SessionIndex in the request.
     * 
     * @param flag flag to set
     */
    public void setIncludeSessionIndex(final boolean flag) {
        checkSetterPreconditions();
        includeSessionIndex = flag;
    }

    /**
     * Set the strategy used to locate the {@link IdentifierGenerationStrategy} to use.
     * 
     * @param strategy lookup strategy
     */
    public void setIdentifierGeneratorLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,IdentifierGenerationStrategy> strategy) {
        checkSetterPreconditions();
        idGeneratorLookupStrategy =
                Constraint.isNotNull(strategy, "IdentifierGenerationStrategy lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the issuer value to use.
     * 
     * @param strategy lookup strategy
     */
    public void setIssuerLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        issuerLookupStrategy = strategy;
    }

    /**
     * Set the logout propagation context lookup strategy.
     * 
     * @param strategy lookup strategy
     */
    public void setLogoutPropagationContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,LogoutPropagationContext> strategy) {
        checkSetterPreconditions();
        logoutPropContextLookupStrategy =
                Constraint.isNotNull(strategy, "LogoutPropagationContext lookup strategy cannot be null");
    }

// Checkstyle: CyclomaticComplexity OFF 
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        final MessageContext outboundMessageCtx = profileRequestContext.getOutboundMessageContext();
        if (outboundMessageCtx == null) {
            log.debug("{} No outbound message context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        } else if (!overwriteExisting && outboundMessageCtx.getMessage() != null) {
            log.debug("{} Outbound message context already contains a message", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        }

        idGenerator = idGeneratorLookupStrategy.apply(profileRequestContext);
        if (idGenerator == null) {
            log.debug("{} No identifier generation strategy", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        if (issuerLookupStrategy != null) {
            issuerId = issuerLookupStrategy.apply(profileRequestContext);
        }

        final LogoutPropagationContext logoutPropCtx = logoutPropContextLookupStrategy.apply(profileRequestContext);
        if (logoutPropCtx == null) {
            log.debug("{} No logout propagation context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        } else if (logoutPropCtx.getSession() == null || !(logoutPropCtx.getSession() instanceof SAML2SPSession)) {
            log.debug("{} Logout propgation context did not contain a SAML2SPSession", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        saml2Session = (SAML2SPSession) logoutPropCtx.getSession();
        assert saml2Session!=null;
        
        outboundMessageCtx.setMessage(null);
        
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final XMLObjectBuilderFactory bf = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final SAMLObjectBuilder<LogoutRequest> requestBuilder =
                (SAMLObjectBuilder<LogoutRequest>) bf.<LogoutRequest>ensureBuilder(
                        LogoutRequest.DEFAULT_ELEMENT_NAME);

        final LogoutRequest object = requestBuilder.buildObject();
        
        object.setID(idGenerator.generateIdentifier());
        object.setIssueInstant(Instant.now());
        object.setVersion(SAMLVersion.VERSION_20);
        final SAML2SPSession s2Session = saml2Session;
        assert s2Session!=null;
        try {
            final NameID nameId = XMLObjectSupport.cloneXMLObject(s2Session.getNameID());
            object.setNameID(nameId);
        } catch (final MarshallingException|UnmarshallingException e) {
            log.error("{} Error cloning NameID for use in LogoutRequest for {}", getLogPrefix(),
                    s2Session.getId(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.MESSAGE_PROC_ERROR);
            return;
        }

        if (issuerId != null) {
            log.debug("{} Setting Issuer to {}", getLogPrefix(), issuerId);
            final SAMLObjectBuilder<Issuer> issuerBuilder =
                    (SAMLObjectBuilder<Issuer>) bf.<Issuer>ensureBuilder(Issuer.DEFAULT_ELEMENT_NAME);
            final Issuer issuer = issuerBuilder.buildObject();
            issuer.setValue(issuerId);
            object.setIssuer(issuer);
        } else {
            log.debug("{} No issuer value available, leaving Issuer unset", getLogPrefix());
        }
        
        if (includeSessionIndex) {
            final SAMLObjectBuilder<SessionIndex> indexBuilder =
                    (SAMLObjectBuilder<SessionIndex>) bf.<SessionIndex>ensureBuilder(
                            SessionIndex.DEFAULT_ELEMENT_NAME);
            final SessionIndex index = indexBuilder.buildObject();
            index.setValue(s2Session.getSessionIndex());
            object.getSessionIndexes().add(index);
        }
        
        final MessageContext outboundMessageCtx = profileRequestContext.getOutboundMessageContext();
        assert outboundMessageCtx!=null;
        outboundMessageCtx.setMessage(object);
    }
// Checkstyle: CyclomaticComplexity ON
    
}