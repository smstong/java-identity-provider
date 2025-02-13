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

package net.shibboleth.idp.saml.audit.impl;

import java.security.NoSuchAlgorithmException;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.OutboundMessageContextLookup;
import org.opensaml.saml.common.SAMLObject;

import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.context.navigate.SubjectContextPrincipalLookupFunction;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.profile.context.navigate.IssuerLookupFunction;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.codec.StringDigester;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Action that produces F-TICKS log entries for successful SAML SSO responses. 
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 */
public class WriteFTICKSLog extends AbstractProfileAction {

    /** Logging category to use. */
    @Nonnull @NotEmpty public static final String FTICKS_LOG_CATEGORY = "Shibboleth-FTICKS";

    /** Federation ID for log. */
    @NonnullAfterInit @NotEmpty private String federationId;
    
    /** Digest algorithm for username hashing. */
    @NonnullAfterInit @NotEmpty private String digestAlgorithm;

    /** Salt for username hashing. */
    @Nullable private String salt;

    /** Lookup strategy for relying party ID. */
    @Nonnull private Function<ProfileRequestContext,String> relyingPartyLookupStrategy;

    /** Lookup strategy for responder ID. */
    @Nonnull private Function<ProfileRequestContext,String> responderLookupStrategy;
    
    /** Lookup strategy for username. */
    @Nonnull private Function<ProfileRequestContext,String> usernameLookupStrategy;

    /** Lookup strategy for authentication method. */
    @Nonnull private Function<ProfileRequestContext,String> authenticationMethodLookupStrategy;

    /** Lookup strategy for StatusCode. */
    @Nonnull private Function<ProfileRequestContext,String> statusCodeLookupStrategy;

    /** Username hasher. */
    @NonnullAfterInit private StringDigester digester;

    /** Constructor. */
    public WriteFTICKSLog() {
        relyingPartyLookupStrategy = new RelyingPartyIdLookupFunction();
        responderLookupStrategy = new IssuerLookupFunction();
        final Function<ProfileRequestContext,String>  unls = new SubjectContextPrincipalLookupFunction().compose(
                new ChildContextLookup<>(SubjectContext.class));
        assert unls != null;
        usernameLookupStrategy = unls;
        
        Function<ProfileRequestContext, SAMLObject> para =
                new MessageLookup<>(SAMLObject.class).compose(new OutboundMessageContextLookup());
        assert para != null;
        authenticationMethodLookupStrategy = new AuthnContextAuditExtractor(para);
        para = new MessageLookup<>(SAMLObject.class).compose(new OutboundMessageContextLookup());
        assert para != null;
        statusCodeLookupStrategy = new StatusCodeAuditExtractor(para);
    }
    
    /**
     * Set the federation identifier for the log.
     * 
     * @param id federation identifier
     */
    public void setFederationId(@Nonnull @NotEmpty final String id) {
        federationId = Constraint.isNotNull(StringSupport.trimOrNull(id), "Federation ID cannot be null or empty");
    }

    /**
     * Set the digest algorithm for username hashing.
     * 
     * @param alg digest algorithm
     */
    public void setDigestAlgorithm(@Nonnull @NotEmpty final String alg) {
        digestAlgorithm = Constraint.isNotNull(StringSupport.trimOrNull(alg),
                "Digest algorithm cannot be null or empty");
    }

    /**
     * Set the salt for username hashing.
     * 
     * @param s salt
     */
    public void setSalt(@Nullable final String s) {
        if (s != null && !s.isEmpty()) {
            salt = s;
        } else {
            salt = null;
        }
    }

    /**
     * Set the strategy used to locate the relying party ID.
     * 
     * @param strategy lookup strategy
     */
    public void setRelyingPartyLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        relyingPartyLookupStrategy = Constraint.isNotNull(strategy, "Relying Party ID lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the responder ID.
     * 
     * @param strategy lookup strategy
     */
    public void setResponderLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        responderLookupStrategy = Constraint.isNotNull(strategy, "Responder ID lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the username.
     * 
     * @param strategy lookup strategy
     */
    public void setUsernameLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        usernameLookupStrategy = Constraint.isNotNull(strategy, "Username lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the authentication method.
     * 
     * @param strategy lookup strategy
     */
    public void setAuthenticationMethodLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        authenticationMethodLookupStrategy = Constraint.isNotNull(strategy,
                "Authentication method lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the status code.
     * 
     * @param strategy lookup strategy
     */
    public void setStatusCodeLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        statusCodeLookupStrategy = Constraint.isNotNull(strategy, "StatusCode lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (federationId == null) {
            throw new ComponentInitializationException("Federation ID cannot be null or empty.");
        }
        
        if (digestAlgorithm == null) {
            throw new ComponentInitializationException("Digest Algorithm cannot be null or empty.");
        }

        try {
            assert digestAlgorithm != null;
            digester = new StringDigester(digestAlgorithm, StringDigester.OutputFormat.HEX_LOWER);
            digester.setSalt(salt);
            digester.setRequireSalt(true);
        } catch (final NoSuchAlgorithmException e) {
            throw new ComponentInitializationException(e);
        }
        
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        //"F-TICKS/%{idp.fticks.federation:Undefined}/1.0#TS=%T#RP=%SP#AP=%IDP #PN=%HASHEDu#AM=%ac#"
        
        final StringBuilder record = new StringBuilder("F-TICKS/");
        record.append(federationId).append("/1.0#TS=").append(System.currentTimeMillis() / 1000);
        
        String field = relyingPartyLookupStrategy.apply(profileRequestContext);
        if (field != null && !field.isEmpty()) {
            record.append("#RP=").append(field);
        }

        field = responderLookupStrategy.apply(profileRequestContext);
        if (field != null && !field.isEmpty()) {
            record.append("#AP=").append(field);
        }
        
        field = usernameLookupStrategy.apply(profileRequestContext);
        if (field != null && !field.isEmpty()) {
            field = digester.apply(field);
            if (field != null && !field.isEmpty()) {
                record.append("#PN=").append(field);
            }
        }
        
        field = authenticationMethodLookupStrategy.apply(profileRequestContext);
        if (field != null && !field.isEmpty()) {
            record.append("#AM=").append(field);
        }
        
        field = statusCodeLookupStrategy.apply(profileRequestContext);
        if (field != null && (org.opensaml.saml.saml1.core.StatusCode.SUCCESS.getLocalPart().equals(field)
                || org.opensaml.saml.saml2.core.StatusCode.SUCCESS.equals(field))) {
            record.append("#RESULT=OK");
        } else {
            record.append("#RESULT=FAIL");
        }

        record.append("#");
        LoggerFactory.getLogger(FTICKS_LOG_CATEGORY).info(record.toString());
    }
// Checkstyle: CyclomaticComplexity ON
    
}