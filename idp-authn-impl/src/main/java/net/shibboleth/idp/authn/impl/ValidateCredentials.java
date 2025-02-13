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

package net.shibboleth.idp.authn.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import com.google.common.net.UrlEscapers;

import net.shibboleth.idp.authn.AccountLockoutManager;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnAuditFields;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.CredentialValidator;
import net.shibboleth.idp.authn.CredentialValidator.ErrorHandler;
import net.shibboleth.idp.authn.CredentialValidator.WarningHandler;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.net.CookieManager;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.shared.security.DataSealer;
import net.shibboleth.shared.security.DataSealerException;

/**
 * An action that processes a list of {@link CredentialValidator} objects to produce an {@link AuthenticationResult}.
 *  
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event others on error
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class).getAttemptedFlow() != null</pre>
 * 
 * @since 4.0.0
 */
public class ValidateCredentials extends AbstractAuditingValidationAction implements WarningHandler, ErrorHandler {

    /** Default prefix for metrics. */
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp.authn";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidateCredentials.class);
    
    /** Ordered list of validators. */
    @Nonnull private List<CredentialValidator> credentialValidators;
    
    /** Whether all validators must succeed. */
    private boolean requireAll;

    /** Optional lockout management interface. */
    @Nullable private AccountLockoutManager lockoutManager;
    
    /** Results from successful validators. */
    @Nonnull private Collection<Subject> results;
    
    /** Currently executing validator. */
    @Nullable private CredentialValidator currentValidator;

    /** Tracks whether a warning event was signaled. */
    private boolean warningSignaled;

    /** Tracks whether an error event was signaled. */
    private boolean errorSignaled;
    
    /** Constructor. */
    public ValidateCredentials() {
        setMetricName(DEFAULT_METRIC_NAME);
        credentialValidators = CollectionSupport.emptyList();
        results = new ArrayList<>(1);
    }
    
    /**
     * Set an account lockout management component.
     * 
     * @param manager lockout manager
     */
    public void setLockoutManager(@Nullable final AccountLockoutManager manager) {
        checkSetterPreconditions();
        lockoutManager = manager;
    }
    
    /**
     * Set the list of validators to use.
     * 
     * @param validators validators to use
     */
    public void setValidators(@Nullable final List<CredentialValidator> validators) {
        checkSetterPreconditions();
        if (validators != null) {
            credentialValidators = CollectionSupport.copyToList(validators);
        } else {
            credentialValidators = CollectionSupport.emptyList();
        }
    }
    
    /**
     * Set whether to execute and require success from all configured validators,
     * or stop at the first successful result.
     * 
     * @param flag flag to set
     */
    public void setRequireAll(final boolean flag) {
        checkSetterPreconditions();
        requireAll = flag;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty public String getMetricName() {
        // only called in execute when we know the field is non-null
        assert currentValidator != null;
        final String cvId = currentValidator.getId();
        return super.getMetricName() + '.' + cvId;
    }
       
    /** {@inheritDoc} */
    @Override
    public void handleWarning(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext, @Nullable final String message,
            @Nonnull @NotEmpty final String eventId) {
        warningSignaled = true;
        super.handleWarning(profileRequestContext, authenticationContext, message, eventId);
    }

    /** {@inheritDoc} */
    @Override
    public void handleError(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext, @Nullable final String message,
            @Nonnull @NotEmpty final String eventId) {
        errorSignaled = true;
        super.handleError(profileRequestContext, authenticationContext, message, eventId);
    }
    
    /** {@inheritDoc} */
    @Override
    public void handleError(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext, @Nonnull final Exception e,
            @Nonnull @NotEmpty final String eventId) {
        errorSignaled = true;
        super.handleError(profileRequestContext, authenticationContext, e, eventId);
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (lockoutManager != null && lockoutManager.check(profileRequestContext)) {
            log.info("{} Account locked out, aborting authentication", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, AuthnEventIds.ACCOUNT_LOCKED,
                    AuthnEventIds.ACCOUNT_LOCKED);
            return;
        }
        
        for (final CredentialValidator validator : credentialValidators) {
            log.trace("{} Attempting credential validation via {}", getLogPrefix(), validator.getId());
            
            currentValidator = validator;
            
            try {
                final Subject subject =
                        currentValidator.validate(profileRequestContext, authenticationContext, this, this);
                if (subject == null) {
                    // Ignored, so try next one.
                    continue;
                }
                
                // Add the result to the list and record it.
                results.add(subject);
                
                if (!requireAll) {
                    recordSuccess(profileRequestContext);
                    buildAuthenticationResult(profileRequestContext, authenticationContext);
                    if (!warningSignaled) {
                        ActionSupport.buildProceedEvent(profileRequestContext);
                    }
                    return;
                }
            } catch (final Exception e) {
                if (requireAll || !errorSignaled) {
                    super.handleError(profileRequestContext, authenticationContext, e, AuthnEventIds.AUTHN_EXCEPTION);
                    errorSignaled = true;
                }
                
                recordFailure(profileRequestContext);
                
                if (requireAll) {
                    break;
                }
            }
        }
        
        // If all must pass, and all passed, and at least one did something, then that's also success.
        if (requireAll && !errorSignaled && !results.isEmpty()) {
            recordSuccess(profileRequestContext);
            buildAuthenticationResult(profileRequestContext, authenticationContext);
            if (!warningSignaled) {
                ActionSupport.buildProceedEvent(profileRequestContext);
            }
            return;
        }

        // If failure, then we may need to bump a lockout count if one of them outright
        // failed. Failure could also just mean nothing was attempted.
        
        if (errorSignaled) {
            if (lockoutManager != null) {
                lockoutManager.increment(profileRequestContext);
            }
        } else {
            log.warn("{} No validators were available or usable", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, AuthnEventIds.REQUEST_UNSUPPORTED,
                    AuthnEventIds.REQUEST_UNSUPPORTED);
        }
    }
// Checkstyle: CyclomaticComplexity ON
    
    /** {@inheritDoc} */
    @Override
    @Nonnull protected Subject populateSubject(@Nonnull final Subject subject) {
        
        for (final Subject s : results) {
            subject.getPrincipals().addAll(s.getPrincipals());
            subject.getPublicCredentials().addAll(s.getPublicCredentials());
            subject.getPrivateCredentials().addAll(s.getPrivateCredentials());
        }
        
        return subject;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Also optionally clears account lockout state.</p>
     */
    @Override
    protected void recordSuccess(@Nonnull final ProfileRequestContext profileRequestContext) {
        // Need to do this first because the superclass's method will call the cleanup hook.
        if (lockoutManager != null) {
            if (!lockoutManager.clear(profileRequestContext)) {
                log.warn("{} Failed to clear lockout state", getLogPrefix());
            }
        }
        
        super.recordSuccess(profileRequestContext);
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull @Unmodifiable @NotLive protected Map<String,String> getAuditFields(
            @Nonnull final ProfileRequestContext profileRequestContext) {
        // only called in execute when we know the field is non-null
        assert currentValidator!=null;
        return CollectionSupport.singletonMap(AuthnAuditFields.CREDENTIAL_VALIDATOR, currentValidator.getId());
    }
    
    /**
     * A default cleanup hook that removes the {@link UsernamePasswordContext} from the tree.
     * 
     * It also "clears" the password field, but this won't be useful until we get off the String type.
     * 
     * @since 4.1.0
     */
    public static class UsernamePasswordCleanupHook implements Consumer<ProfileRequestContext> {

        /** Class logger. */
        @Nonnull private final Logger log = LoggerFactory.getLogger(UsernamePasswordCleanupHook.class);
        
        /** Username cookie name. */
        @Nullable @NotEmpty private String cookieName;

        /** Optional cookie manager to use. */
        @Nullable private CookieManager cookieManager;

        /** Optional data sealer to use. */
        @Nullable private DataSealer dataSealer;

        /**
         * Set cookie name to use for cached username.
         * 
         * @param name cookie name
         * 
         * @since 5.1.0
         */
        public void setCookieName(@Nullable final String name) {
            cookieName = StringSupport.trimOrNull(name);
        }
        
        /**
         * Sets optional {@link CookieManager} to use.
         * 
         * @param manager cookie manager
         * 
         * @since 5.1.0
         */
        public void setCookieManager(@Nullable final CookieManager manager) {
            cookieManager = manager;
        }

        /**
         * Sets optional {@link DataSealer} to use.
         * 
         * @param sealer data sealer
         * 
         * @since 5.1.0
         */
        public void setDataSealer(@Nullable final DataSealer sealer) {
            dataSealer = sealer;
        }
        
// Checkstyle: CyclomaticComplexity OFF
        /** {@inheritDoc} */
        public void accept(@Nullable final ProfileRequestContext input) {
            
            final AuthenticationContext authnCtx =
                    input != null ? input.getSubcontext(AuthenticationContext.class) : null;
            if (authnCtx == null) {
                return;
            }

            final UsernamePasswordContext upCtx = authnCtx.getSubcontext(UsernamePasswordContext.class);
            if (upCtx == null) {
                return;
            }
            
            final String localCookieName = cookieName;
            if (authnCtx.isResultCacheable()) {
                if (cookieManager != null && dataSealer != null && localCookieName != null) {
                    String wrapped = upCtx.getUsername();
                    if (wrapped != null) {
                        try {
                            assert dataSealer != null;
                            wrapped = dataSealer.wrap(wrapped);
                            assert cookieManager != null;
                            cookieManager.addCookie(localCookieName,
                                    UrlEscapers.urlFormParameterEscaper().escape(wrapped));
                        } catch (final DataSealerException e) {
                            wrapped = null;
                            log.warn("Error sealing username cookie", e);
                        }
                    }
                }
            } else if (cookieManager != null && localCookieName != null) {
                cookieManager.unsetCookie(localCookieName);
            }

            upCtx.setPassword(null);
            authnCtx.removeSubcontext(upCtx);
        }
    }
// Checkstyle: CyclomaticComplexity ON

}