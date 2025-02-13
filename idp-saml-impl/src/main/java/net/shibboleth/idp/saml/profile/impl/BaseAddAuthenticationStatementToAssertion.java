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

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.shared.primitive.LoggerFactory;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.profile.config.navigate.IdentifierGenerationStrategyLookupFunction;
import net.shibboleth.profile.context.navigate.IssuerLookupFunction;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;
import net.shibboleth.shared.servlet.HttpServletSupport;

/**
 * Base class for actions that encode authentication information into a SAML 1 or SAML 2 statement.
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link AuthnEventIds#INVALID_AUTHN_CTX}
 */
public abstract class BaseAddAuthenticationStatementToAssertion extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BaseAddAuthenticationStatementToAssertion.class);

    /**
     * Whether the generated authentication statement should be placed in its own assertion or added to one if it
     * exists.
     */
    private boolean statementInOwnAssertion;

    /** Strategy used to locate the {@link IdentifierGenerationStrategy} to use. */
    @Nonnull private Function<ProfileRequestContext,IdentifierGenerationStrategy> idGeneratorLookupStrategy;

    /** Strategy used to obtain the assertion issuer value. */
    @Nonnull private Function<ProfileRequestContext,String> issuerLookupStrategy;
    
    /** Strategy used to obtain the client Address to insert. */
    @NonnullAfterInit private Function<ProfileRequestContext,String> addressLookupStrategy;
    
    /** AuthenticationResult basis of statement. */
    @NonnullBeforeExec private AuthenticationResult authenticationResult;

    /** The generator to use. */
    @NonnullBeforeExec private IdentifierGenerationStrategy idGenerator;
    
    /** EntityID to populate as assertion issuer. */
    @NonnullBeforeExec private String issuerId;
    
    /** Constructor. */
    public BaseAddAuthenticationStatementToAssertion() {
        statementInOwnAssertion = false;

        idGeneratorLookupStrategy = new IdentifierGenerationStrategyLookupFunction();
        issuerLookupStrategy = new IssuerLookupFunction();
    }

    /**
     * Set whether the generated statement should be placed in its own assertion or added to one if it exists.
     * 
     * @return whether the generated statement should be placed in its own assertion or added to one if it exists
     */
    public boolean isStatementInOwnAssertion() {
        return statementInOwnAssertion;
    }
    
    /**
     * Set whether the generated authentication statement should be placed in its own assertion or added to one if it
     * exists.
     * 
     * @param inOwnAssertion whether the generated authentication statement should be placed in its own assertion or
     *            added to one if it exists
     */
    public void setStatementInOwnAssertion(final boolean inOwnAssertion) {
        checkSetterPreconditions();
        statementInOwnAssertion = inOwnAssertion;
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
    public void setIssuerLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        issuerLookupStrategy = Constraint.isNotNull(strategy, "Issuer lookup strategy cannot be null");
    }

    /**
     * Get the strategy used to obtain the client IP address to insert into the statement.
     * 
     * @return lookup strategy
     * 
     * @since 4.0.0
     */
    @NonnullAfterInit public Function<ProfileRequestContext,String> getAddressLookupStrategy() {
        return addressLookupStrategy;
    }
    
    /**
     * Set the strategy used to obtain the client IP address to insert into the statement.
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.0.0
     */
    public void setAddressLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        addressLookupStrategy = strategy;
    }
    
    /**
     * Get the {@link AuthenticationResult} to encode.
     * 
     * @return the result to encode
     */
    @Nonnull public AuthenticationResult getAuthenticationResult() {
        assert isPreExecuteCalled() && authenticationResult!=null;
        return authenticationResult;
    }

    /**
     * Get the {@link IdentifierGenerationStrategy} to use if an assertion must be created.
     * 
     * @return the ID generation strategy
     */
    @Nonnull public IdentifierGenerationStrategy getIdGenerator() {
        assert isPreExecuteCalled() && idGenerator!=null;
        return idGenerator;
    }

    /**
     * Get the issuer name to use if an assertion must be created.   
     *
     * @return the issuer name
     */
    @Nonnull public String getIssuerId() {
        assert isPreExecuteCalled() && issuerId!=null;
        return issuerId;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (addressLookupStrategy == null) {
            addressLookupStrategy = new RemoteAddressStrategy();
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }

        log.debug("{} Attempting to add an AuthenticationStatement to outgoing Assertion", getLogPrefix());
        
        idGenerator = idGeneratorLookupStrategy.apply(profileRequestContext);
        if (idGenerator == null) {
            log.debug("{} No identifier generation strategy", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        issuerId = issuerLookupStrategy.apply(profileRequestContext);
        if (issuerId == null) {
            log.debug("{} No assertion issuer value", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        authenticationResult = authenticationContext.getAuthenticationResult();
        if (authenticationResult == null) {
            log.debug("{} No AuthenticationResult in current authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_AUTHN_CTX);
            return false;
        }
        
        return true;
    }
    
    /**
     * Default strategy for obtaining client address from servlet layer.
     * 
     * @since 4.0.0
     */
    private class RemoteAddressStrategy implements Function<ProfileRequestContext,String> {

        /** {@inheritDoc} */
        @Nullable public String apply(@Nullable final ProfileRequestContext t) {
            final HttpServletRequest req = getHttpServletRequest();
            if (req != null) {
                return HttpServletSupport.getRemoteAddr(req);
            }
            
            return null;
        }
        
    }
}