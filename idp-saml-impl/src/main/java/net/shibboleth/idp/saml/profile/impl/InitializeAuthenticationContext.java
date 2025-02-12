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

package net.shibboleth.idp.saml.profile.impl;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.IDPEntry;
import org.opensaml.saml.saml2.core.IDPList;
import org.opensaml.saml.saml2.core.Scoping;
import org.opensaml.saml.saml2.core.Subject;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.idp.authn.config.navigate.ForceAuthnProfileConfigPredicate;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.idp.saml.saml2.profile.config.logic.IgnoreScopingProfileConfigPredicate;
import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.saml.saml2.profile.config.navigate.ProxyCountLookupFunction;
import net.shibboleth.shared.logic.Constraint;

/**
 * An action that creates an {@link AuthenticationContext} and attaches it to the current {@link ProfileRequestContext}.
 * 
 * <p>If the incoming message is a SAML 2.0 {@link AuthnRequest}, then basic authentication policy (IsPassive,
 * ForceAuthn, Scoping) is copied into the context from the request.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#ACCESS_DENIED}
 * 
 * @post ProfileRequestContext.getSubcontext(AuthenticationContext.class) != true
 * @post SAML 2.0 AuthnRequest policy flags are (optionally) copied to the {@link AuthenticationContext}
 */
public class InitializeAuthenticationContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InitializeAuthenticationContext.class);

    /** Strategy used to look up a {@link RelyingPartyContext} for configuration options. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Extracts forceAuthn property from profile config. */
    @Nonnull private Predicate<ProfileRequestContext> forceAuthnPredicate;

    /** Extracts ignoreScoping property from profile config. */
    @Nonnull private Predicate<ProfileRequestContext> ignoreScopingPredicate;

    /** Strategy used to determine proxy count from configuration. */
    @Nonnull private Function<ProfileRequestContext,Integer> proxyCountLookupStrategy;
    
    /** Strategy used to locate the {@link AuthnRequest} to operate on, if any. */
    @Nonnull private Function<ProfileRequestContext,AuthnRequest> requestLookupStrategy;
    
    /** Incoming SAML 2.0 request, if present. */
    @Nullable private AuthnRequest authnRequest;

    /** Constructor. */
    public InitializeAuthenticationContext() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        forceAuthnPredicate = new ForceAuthnProfileConfigPredicate();
        ignoreScopingPredicate = new IgnoreScopingProfileConfigPredicate();
        proxyCountLookupStrategy = new ProxyCountLookupFunction();
        final Function<ProfileRequestContext,AuthnRequest> rls =
                new MessageLookup<>(AuthnRequest.class).compose(new InboundMessageContextLookup());
        assert rls != null;
        requestLookupStrategy = rls;
    }
    
    /**
     * Set the strategy used to return the {@link RelyingPartyContext} for configuration options.
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.0.0
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        checkSetterPreconditions();
        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }
    
    /**
     * Set the predicate to apply to derive the message-independent forced authn default. 
     * 
     * @param condition condition to set
     * 
     * @since 3.4.0
     */
    public void setForceAuthnPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        checkSetterPreconditions();
        forceAuthnPredicate = Constraint.isNotNull(condition, "Forced authentication predicate cannot be null");
    }

    /**
     * Set the predicate to apply to determine whether to ignore any inbound {@link Scoping} element. 
     * 
     * @param condition condition to set
     * 
     * @since 4.0.0
     */
    public void setIgnoreScopingPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        checkSetterPreconditions();
        ignoreScopingPredicate = Constraint.isNotNull(condition, "Ignore Scoping predicate cannot be null");
    }

    /**
     * Set the lookup function to apply to derive the proxy count from the configuration.
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.0.0
     */
    public void setProxyCountLookupStrategy(@Nonnull final Function<ProfileRequestContext,Integer> strategy) {
        checkSetterPreconditions();
        proxyCountLookupStrategy = Constraint.isNotNull(strategy, "Proxy count lookup strategy cannot be null");
    }
    
    /**
     * Set the strategy used to locate the {@link AuthnRequest} to examine, if any.
     * 
     * @param strategy strategy used to locate the {@link AuthnRequest}
     */
    public void setRequestLookupStrategy(@Nonnull final Function<ProfileRequestContext,AuthnRequest> strategy) {
        checkSetterPreconditions();
        requestLookupStrategy = Constraint.isNotNull(strategy, "AuthnRequest lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        authnRequest = requestLookupStrategy.apply(profileRequestContext);
        return true;
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final AuthenticationContext authnCtx = new AuthenticationContext();

        final AuthnRequest ar = authnRequest;
        if (ar != null) {
            if (!processScoping(profileRequestContext, authnCtx)) {
                return;
            }
            
            Boolean flag = ar.isForceAuthn();
            if (flag != null) {
                authnCtx.setForceAuthn(flag);
            }
            flag = ar.isPassive();
            if (flag != null) {
                authnCtx.setIsPassive(flag);
            }
            
            // On an inbound Subject, migrate the populated SubjectContext into the required name
            // field in the new AuthenticationContext.
            final Subject requestedSubject = ar.getSubject();
            if (requestedSubject != null && requestedSubject.getNameID() != null) {
                final SubjectContext subjectCtx = profileRequestContext.getSubcontext(SubjectContext.class);
                if (subjectCtx != null && subjectCtx.getPrincipalName() != null) {
                    authnCtx.setRequiredName(subjectCtx.getPrincipalName());
                    profileRequestContext.removeSubcontext(subjectCtx);
                }
            }
        }

        if (!authnCtx.isForceAuthn()) {
            authnCtx.setForceAuthn(forceAuthnPredicate.test(profileRequestContext));
        }
        
        // Merge requested and pre-configured proxy count.
        
        final Integer reqCount = authnCtx.getProxyCount();
        Integer configCount = proxyCountLookupStrategy.apply(profileRequestContext);
        if (configCount != null && configCount < 0) {
            configCount = 0;
        }
        
        if (reqCount != null) {
            if (configCount != null) {
                authnCtx.setProxyCount(Integer.min(configCount, reqCount));
                log.debug("{} Combined requested and configured proxy count: {}", getLogPrefix(),
                        authnCtx.getProxyCount());
            }
        } else {
            authnCtx.setProxyCount(configCount);
        }
        
        profileRequestContext.addSubcontext(authnCtx, true);

        log.debug("{} Created authentication context: {}", getLogPrefix(), authnCtx);
    }
// Checkstyle: CyclomaticComplexity OFF    
    
    /**
     * Check an inbound {@link AuthnRequest} for a {@link Scoping} element.
     * 
     * @param profileRequestContext current profile request context
     * @param authenticationContext the context to populate
     * 
     * @return true iff processing should continue
     */
    private boolean processScoping(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        assert authnRequest!=null;
        final Scoping scoping = authnRequest.getScoping();
        if (scoping == null) {
            log.debug("{} AuthnRequest did not contain Scoping, nothing to do", getLogPrefix());
            return true;
        }
        
        if (ignoreScopingPredicate.test(profileRequestContext)) {
            log.warn("{} Ignoring inbound Scoping element in AuthnRequest in violation of standard", getLogPrefix());
            return true;
        }
        
        // Check if permitted.
        final RelyingPartyContext rpContext = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        final ProfileConfiguration profileConfig = rpContext==null ? null : rpContext.getProfileConfig();
        if (profileConfig != null) {
            if (profileConfig.isFeatureDisallowed(
                    profileRequestContext, BrowserSSOProfileConfiguration.FEATURE_SCOPING)) {
                log.warn("{} Incoming Scoping disallowed by profile configuration", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, EventIds.ACCESS_DENIED);
                return false;
            }
        }
        
        // The IDPList doesn't have mandatory semantics, other than disallowing removal.
        
        final IDPList idpList = scoping.getIDPList();
        if (idpList != null && idpList.getIDPEntrys() != null) {
            final Set<String> requestedAuthorities = idpList.getIDPEntrys()
                    .stream()
                    .map(IDPEntry::getProviderID)
                    .filter(id -> id != null)
                    .collect(Collectors.toUnmodifiableSet());
            authenticationContext.getProxiableAuthorities().addAll(requestedAuthorities);
        }
        
        final Integer proxyCount = scoping.getProxyCount();
        if (proxyCount != null) {
            authenticationContext.setProxyCount(Integer.max(0, proxyCount));
        }
        return true;
    }
    
}