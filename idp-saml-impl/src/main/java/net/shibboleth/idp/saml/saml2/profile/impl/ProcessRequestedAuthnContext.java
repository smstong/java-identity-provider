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

package net.shibboleth.idp.saml.saml2.profile.impl;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnContextDeclRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.slf4j.Logger;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthnContextDeclRefPrincipal;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * An authentication action that processes the {@link RequestedAuthnContext} in a SAML 2 {@link AuthnRequest},
 * and populates a {@link RequestedPrincipalContext} with the corresponding information.
 * 
 * <p>If this feature is disallowed by profile configuration, then an error event is signaled.</p>
 * 
 * <p>Each requested context class or declaration reference is translated into a custom {@link Principal}
 * for use by the authentication subsystem to drive flow selection.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link EventIds#ACCESS_DENIED}
 */
public class ProcessRequestedAuthnContext extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ProcessRequestedAuthnContext.class);

    /** Strategy used to look up a {@link RelyingPartyContext} for configuration options. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Lookup strategy function for obtaining {@link AuthnRequest}. */
    @Nonnull private Function<ProfileRequestContext,AuthnRequest> authnRequestLookupStrategy;

    /** Context URIs to ignore in a request. */
    @Nonnull private Set<String> ignoredContexts;
    
    /** The request message to read from. */
    @NonnullBeforeExec private AuthnRequest authnRequest;
    
    /** Constructor. */
    public ProcessRequestedAuthnContext() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        final Function<ProfileRequestContext,AuthnRequest> arls =
                new MessageLookup<>(AuthnRequest.class).compose(new InboundMessageContextLookup());
        assert arls != null;
        authnRequestLookupStrategy = arls;
        ignoredContexts = CollectionSupport.singleton(AuthnContext.UNSPECIFIED_AUTHN_CTX);
    }

    /**
     * Set the strategy used to return the {@link RelyingPartyContext} for configuration options.
     * 
     * @param strategy lookup strategy
     * 
     * @since 3.3.0
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        checkSetterPreconditions();
        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link AuthnRequest} to read from.
     * 
     * @param strategy lookup strategy
     */
    public void setAuthnRequestLookupStrategy(@Nonnull final Function<ProfileRequestContext,AuthnRequest> strategy) {
        checkSetterPreconditions();
        authnRequestLookupStrategy = Constraint.isNotNull(strategy, "AuthnRequest lookup strategy cannot be null");
    }
    
    /**
     * Set the context class or declaration URIs to ignore if found in a request.
     * 
     * <p>This defaults to only {@link AuthnContext#UNSPECIFIED_AUTHN_CTX}.</p>
     * 
     * @param contexts  contexts to ignore
     */
    public void setIgnoredContexts(@Nonnull final Collection<String> contexts) {
        checkSetterPreconditions();
        final Collection<String> trimmed = StringSupport.normalizeStringCollection(contexts);
        
        if (trimmed.isEmpty()) {
            ignoredContexts = CollectionSupport.emptySet();
        } else {
            ignoredContexts = CollectionSupport.copyToSet(trimmed);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        checkComponentActive();
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }
        
        authnRequest = authnRequestLookupStrategy.apply(profileRequestContext);
        if (authnRequest == null) {
            log.debug("{} AuthnRequest message was not returned by lookup strategy", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        }
        
        return true;
    }

// Checkstyle: CyclomaticComplexity|ReturnCount OFF
    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        final RequestedAuthnContext requestedCtx = authnRequest.getRequestedAuthnContext();
        if (requestedCtx == null) {
            log.debug("{} AuthnRequest did not contain a RequestedAuthnContext, nothing to do", getLogPrefix());
            return;
        }
        
        final List<Principal> principals = new ArrayList<>();
        
        if (!requestedCtx.getAuthnContextClassRefs().isEmpty()) {
            for (final AuthnContextClassRef ref : requestedCtx.getAuthnContextClassRefs()) {
                final String uri = ref.getURI();
                if (uri!= null) {
                    if (!ignoredContexts.contains(uri)) {
                        principals.add(new AuthnContextClassRefPrincipal(uri));
                    } else {
                        log.info("{} Ignoring AuthnContextClassRef: {}", getLogPrefix(), ref.getURI());
                    }
                }
            }
        } else if (!requestedCtx.getAuthnContextDeclRefs().isEmpty()) {
            for (final AuthnContextDeclRef ref : requestedCtx.getAuthnContextDeclRefs()) {
                final String uri = ref.getURI();
                if (uri != null) {
                    if (!ignoredContexts.contains(uri)) {
                        principals.add(new AuthnContextDeclRefPrincipal(uri));
                    } else {
                        log.info("{} Ignoring AuthnContextDeclRef: {}", getLogPrefix(), ref.getURI());
                    }
                }
            }
        }
        
        if (principals.isEmpty()) {
            log.debug("{} RequestedAuthnContext did not contain any requested contexts, nothing to do", getLogPrefix());
            return;
        }

        // Check if permitted.
        final RelyingPartyContext rpContext = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        final ProfileConfiguration profileConfig = rpContext==null ? null : rpContext.getProfileConfig();
        if (profileConfig != null) {            
            if (profileConfig.isFeatureDisallowed(
                    profileRequestContext, BrowserSSOProfileConfiguration.FEATURE_AUTHNCONTEXT)) {
                log.warn("{} Incoming RequestedAuthnContext disallowed by profile configuration", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, EventIds.ACCESS_DENIED);
                return;
            }
        }
        
        final RequestedPrincipalContext rpCtx = new RequestedPrincipalContext();
        final var operator = requestedCtx.getComparison();
        if (operator != null) {
            rpCtx.setOperator(operator.toString());
        } else {
            rpCtx.setOperator(AuthnContextComparisonTypeEnumeration.EXACT.toString());
        }
        rpCtx.setRequestedPrincipals(principals);
        
        authenticationContext.addSubcontext(rpCtx, true);
        log.debug("{} RequestedPrincipalContext created with operator {} and {} custom principal(s)",
                getLogPrefix(), rpCtx.getOperator(), principals.size());
    }
// Checkstyle: CyclomaticComplexity|ReturnCount ON
    
}