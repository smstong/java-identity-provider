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
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthenticatingAuthority;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.SubjectLocality;
import org.opensaml.saml.saml2.profile.SAML2ActionSupport;
import org.slf4j.Logger;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.impl.DefaultPrincipalDeterminationStrategy;
import net.shibboleth.idp.authn.principal.ProxyAuthenticationPrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthnContextDeclRefPrincipal;
import net.shibboleth.idp.saml.profile.config.navigate.SessionLifetimeLookupFunction;
import net.shibboleth.idp.saml.profile.impl.BaseAddAuthenticationStatementToAssertion;
import net.shibboleth.idp.saml.saml2.profile.config.logic.SuppressAuthenticatingAuthorityPredicate;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Action that builds an {@link AuthnStatement} and adds it to an {@link Assertion} returned by a lookup
 * strategy, by default in the {@link ProfileRequestContext#getOutboundMessageContext()}.
 * 
 * <p>If no {@link Response} exists, then an {@link Assertion} directly in the outbound message context will
 * be used or created</p>
 * 
 * <p>A constructed {@link Assertion} will have its ID, IssueInstant, Issuer, and Version properties set.
 * The issuer is based on
 * {@link net.shibboleth.profile.relyingparty.RelyingPartyConfiguration#getIssuer(ProfileRequestContext)}.</p>
 * 
 * <p>The {@link AuthnStatement} will have its authentication instant set, based on
 * {@link net.shibboleth.idp.authn.AuthenticationResult#getAuthenticationInstant()}
 * via {@link AuthenticationContext#getAuthenticationResult()}.
 * The {@link AuthnContext} will be set via {@link RequestedPrincipalContext#getMatchingPrincipal()}, or via an injected
 * or defaulted function that obtains a custom principal from the profile context.</p>
 * 
 * <p>The SessionIndex and optionally SessionNotOnOrAfter attributes will also be set.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link net.shibboleth.idp.authn.AuthnEventIds#INVALID_AUTHN_CTX}
 */
public class AddAuthnStatementToAssertion extends BaseAddAuthenticationStatementToAssertion {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AddAuthnStatementToAssertion.class);
    
    /** Strategy used to locate the {@link Assertion} to operate on. */
    @NonnullAfterInit private Function<ProfileRequestContext,Assertion> assertionLookupStrategy;
    
    /** Strategy used to determine the AuthnContextClassRef. */
    @NonnullAfterInit private Function<ProfileRequestContext,AuthnContextClassRefPrincipal> classRefLookupStrategy;

    /** Strategy used to determine SessionNotOnOrAfter value to set. */
    @Nullable private Function<ProfileRequestContext,Duration> sessionLifetimeLookupStrategy;
    
    /** Strategy used to determine whether to suppress AuthenticatingAuthority. */
    @Nonnull private Predicate<ProfileRequestContext> suppressAuthenticatingAuthorityPredicate;
        
    /** Constructor. */
    public AddAuthnStatementToAssertion() {
        sessionLifetimeLookupStrategy = new SessionLifetimeLookupFunction();
        suppressAuthenticatingAuthorityPredicate = new SuppressAuthenticatingAuthorityPredicate();
    }
    
    /**
     * Set the strategy used to locate the {@link Assertion} to operate on.
     * 
     * @param strategy strategy used to locate the {@link Assertion} to operate on
     */
    public void setAssertionLookupStrategy(@Nonnull final Function<ProfileRequestContext,Assertion> strategy) {
        checkSetterPreconditions();
        assertionLookupStrategy = Constraint.isNotNull(strategy, "Assertion lookup strategy cannot be null");
    }
    
    /**
     * Set the strategy function to use to obtain the authentication context class reference to use.
     * 
     * @param strategy  authentication context class reference lookup strategy
     */
    public void setClassRefLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,AuthnContextClassRefPrincipal> strategy) {
        checkSetterPreconditions();
        classRefLookupStrategy = Constraint.isNotNull(strategy,
                "Authentication context class reference strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the SessionNotOnOrAfter value to use.
     * 
     * @param strategy lookup strategy
     */
    public void setSessionLifetimeLookupStrategy(@Nullable final Function<ProfileRequestContext,Duration> strategy) {
        checkSetterPreconditions();
        sessionLifetimeLookupStrategy = strategy;
    }
    
    /**
     * Set the condition used to determine whether to suppress inclusion of AuthenticatingAuthority.
     * 
     * @param condition condition to set
     */
    public void setSuppressAuthenticatingAuthorityPredicate(
            @Nonnull final Predicate<ProfileRequestContext> condition) {
        checkSetterPreconditions();
        suppressAuthenticatingAuthorityPredicate = Constraint.isNotNull(condition, "Condition cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (classRefLookupStrategy == null) {
            classRefLookupStrategy = new DefaultPrincipalDeterminationStrategy<>(AuthnContextClassRefPrincipal.class,
                    new AuthnContextClassRefPrincipal(AuthnContext.UNSPECIFIED_AUTHN_CTX));
        }

        if (assertionLookupStrategy == null) {
            assertionLookupStrategy = new AssertionStrategy();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final Assertion assertion = assertionLookupStrategy.apply(profileRequestContext);
        if (assertion == null) {
            log.error("Unable to obtain Assertion to modify");
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return;
        }

        final AuthnStatement statement = buildAuthnStatement(profileRequestContext,
                authenticationContext.getSubcontext(RequestedPrincipalContext.class)); 
        assertion.getAuthnStatements().add(statement);

        log.debug("{} Added AuthenticationStatement to Assertion {}", getLogPrefix(), assertion.getID());
    }

    /**
     * Build the {@link AuthnStatement} to be added to the {@link Response}.
     * 
     * @param profileRequestContext current request context
     * @param requestedPrincipalContext context specifying request requirements for authn context
     * 
     * @return the authentication statement
     */
    @Nonnull private AuthnStatement buildAuthnStatement(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nullable final RequestedPrincipalContext requestedPrincipalContext) {

        final XMLObjectBuilderFactory bf = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final SAMLObjectBuilder<AuthnStatement> statementBuilder = (SAMLObjectBuilder<AuthnStatement>)
                bf.<AuthnStatement>ensureBuilder(AuthnStatement.TYPE_NAME);
        final SAMLObjectBuilder<AuthnContext> authnContextBuilder = (SAMLObjectBuilder<AuthnContext>)
                bf.<AuthnContext>ensureBuilder(AuthnContext.TYPE_NAME);
        final SAMLObjectBuilder<SubjectLocality> localityBuilder = (SAMLObjectBuilder<SubjectLocality>)
                bf.<SubjectLocality>ensureBuilder(SubjectLocality.TYPE_NAME);

        final AuthnStatement statement = statementBuilder.buildObject();
        statement.setAuthnInstant(getAuthenticationResult().getAuthenticationInstant());
        
        final AuthnContext authnContext = authnContextBuilder.buildObject();
        statement.setAuthnContext(authnContext);
        
        if (requestedPrincipalContext != null && requestedPrincipalContext.getMatchingPrincipal() != null) {
            final Principal matchingPrincipal = requestedPrincipalContext.getMatchingPrincipal();
            if (matchingPrincipal instanceof AuthnContextClassRefPrincipal) {
                authnContext.setAuthnContextClassRef(
                        ((AuthnContextClassRefPrincipal) matchingPrincipal).getAuthnContextClassRef());
            } else if (matchingPrincipal instanceof AuthnContextDeclRefPrincipal) {
                authnContext.setAuthnContextDeclRef(
                        ((AuthnContextDeclRefPrincipal) matchingPrincipal).getAuthnContextDeclRef());
            } else {
                authnContext.setAuthnContextClassRef(
                        classRefLookupStrategy.apply(profileRequestContext).getAuthnContextClassRef());
            }
        } else {
            authnContext.setAuthnContextClassRef(
                    classRefLookupStrategy.apply(profileRequestContext).getAuthnContextClassRef());
        }
        
        addAuthenticatingAuthorities(profileRequestContext, authnContext);
        
        final Duration lifetime = sessionLifetimeLookupStrategy != null ?
                sessionLifetimeLookupStrategy.apply(profileRequestContext) : null;
        if (lifetime != null && lifetime.toMillis() > 0) {
            statement.setSessionNotOnOrAfter(Instant.now().plus(lifetime));
        }
        
        statement.setSessionIndex(getIdGenerator().generateIdentifier());

        final String address = getAddressLookupStrategy().apply(profileRequestContext);
        if (address != null) {
            final SubjectLocality locality = localityBuilder.buildObject();
            locality.setAddress(address);
            statement.setSubjectLocality(locality);
        } else {
            log.debug("{} Address not available, omitting SubjectLocality element", getLogPrefix());
        }
        
        return statement;
    }
    
    /** Add Authenticating Authorities.
     * @param profileRequestContext the prc
     * @param authnContext the authnContext
     */
    private void addAuthenticatingAuthorities(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthnContext authnContext) {
        
        final Set<ProxyAuthenticationPrincipal> proxyPrincipals =
                getAuthenticationResult().getSubject().getPrincipals(ProxyAuthenticationPrincipal.class);
        if (proxyPrincipals != null && !proxyPrincipals.isEmpty()) {
            if (proxyPrincipals.size() == 1) {
                final ProxyAuthenticationPrincipal proxyPrincipal = proxyPrincipals.iterator().next();
                
                final boolean suppress = suppressAuthenticatingAuthorityPredicate.test(profileRequestContext);
                if (!suppress) {
                    final SAMLObjectBuilder<AuthenticatingAuthority> authorityBuilder =
                            (SAMLObjectBuilder<AuthenticatingAuthority>) XMLObjectProviderRegistrySupport
                                .getBuilderFactory().<AuthenticatingAuthority>ensureBuilder(
                                    AuthenticatingAuthority.DEFAULT_ELEMENT_NAME);
                    for (final String authority : proxyPrincipal.getAuthorities()) {
                        final AuthenticatingAuthority aa = authorityBuilder.buildObject();
                        aa.setURI(authority);
                        authnContext.getAuthenticatingAuthorities().add(aa);
                    }
                } else {
                    log.debug("{} Suppressing AuthenticatingAuthority population", getLogPrefix());
                }
            } else {
                log.warn("{} Multiple ProxyAuthenticationPrincipals, skipping AuthenticatingAuthority population",
                        getLogPrefix());
            }
        }
    }
    
    /**
     * Default strategy for obtaining assertion to modify.
     * 
     * <p>If the outbound context is empty, a new assertion is created and stored there. If the outbound
     * message is already an assertion, it's returned. If the outbound message is a response, then either
     * an existing or new assertion in the response is returned, depending on the action setting. If the
     * outbound message is anything else, null is returned.</p>
     */
    private class AssertionStrategy implements Function<ProfileRequestContext,Assertion> {

        /** {@inheritDoc} */
        @Nullable public Assertion apply(@Nullable final ProfileRequestContext input) {
            final MessageContext omc = input != null ? input.getOutboundMessageContext() : null;  

            if (omc != null) {
                final Object outboundMessage = omc.getMessage();
                if (outboundMessage == null) {
                    final Assertion ret = SAML2ActionSupport.buildAssertion(AddAuthnStatementToAssertion.this,
                            getIdGenerator(), getIssuerId());
                    omc.setMessage(ret);
                    return ret;
                } else if (outboundMessage instanceof Assertion) {
                    return (Assertion) outboundMessage;
                } else if (outboundMessage instanceof Response) {
                    if (isStatementInOwnAssertion() || ((Response) outboundMessage).getAssertions().isEmpty()) {
                        return SAML2ActionSupport.addAssertionToResponse(AddAuthnStatementToAssertion.this,
                                (Response) outboundMessage, getIdGenerator(), getIssuerId());
                    }
                    return ((Response) outboundMessage).getAssertions().get(0); 
                }
            }
            
            return null;
        }
    }

}