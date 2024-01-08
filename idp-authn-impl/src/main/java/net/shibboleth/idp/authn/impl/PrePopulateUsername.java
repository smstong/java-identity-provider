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

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.authn.AbstractExtractionAction;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.net.CookieManager;
import net.shibboleth.shared.net.URISupport;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.shared.security.DataSealer;
import net.shibboleth.shared.security.DataSealerException;

/**
 * An action to populate a username into a cleared {@link UsernamePasswordContext}, either from a form
 * submission, a cookie, or an existing session to "prime" the login view.
 * 
 * <p>Because this action is essentially a UI optimization, it's forgiving of errors or problems
 * it encounters and will only warn.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @post {@link UsernamePasswordContext#setUsername(String)} is called with an existing value if found
 * 
 * @since 5.1.0
 */
public class PrePopulateUsername extends AbstractExtractionAction {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PrePopulateUsername.class);
    
    /** Strategy used to create/locate the {@link UsernamePasswordContext} to operate on. */
    @Nonnull private Function<ProfileRequestContext,UsernamePasswordContext> usernamePasswordContextCreationStrategy;

    /** Form parameter name to carry username. */
    @Nonnull @NotEmpty private String usernameFieldName;

    /** Username cookie name. */
    @Nullable @NotEmpty private String cookieName;
    
    /** Optional cookie manager to use. */
    @Nullable private CookieManager cookieManager;

    /** Optional data sealer to use. */
    @Nullable private DataSealer dataSealer;
    
    /** Whether to pull username from existing session or not. */
    private boolean checkSession;
    
    /** Context to operate on. */
    @NonnullBeforeExec private UsernamePasswordContext usernameContext;
    
    /** Constructor.*/
    public PrePopulateUsername() {
        usernamePasswordContextCreationStrategy =
                new ChildContextLookup<>(UsernamePasswordContext.class).compose(
                        new ChildContextLookup<>(AuthenticationContext.class));
            
        usernameFieldName = "j_username";
    }
    
    /**
     * Set the strategy used to create/locate the {@link UsernamePasswordContext} to operate on.
     * 
     * @param strategy creation strategy
     */
    public void setUsernamePasswordContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext,UsernamePasswordContext> strategy) {
        checkSetterPreconditions();

        usernamePasswordContextCreationStrategy =
                Constraint.isNotNull(strategy, "DuoPasswordlessContext lookup strategy cannot be null");
    }
    
    /**
     * Sets the name of the form field to carry the username.
     * 
     * @param name field name
     */
    public void setUsernameFieldName(@Nonnull final String name) {
        checkSetterPreconditions();
        
        usernameFieldName = Constraint.isNotNull(StringSupport.trimOrNull(name) ,
                "Username form field name cannot be null or empty");
    }

    /**
     * Set cookie name to use for cached username.
     * 
     * @param name cookie name
     */
    public void setCookieName(@Nullable final String name) {
        checkSetterPreconditions();

        cookieName = StringSupport.trimOrNull(name);
    }

    /**
     * Sets optional {@link CookieManager} to use.
     * 
     * @param manager cookie manager
     */
    public void setCookieManager(@Nullable final CookieManager manager) {
        checkSetterPreconditions();
        
        cookieManager = manager;
    }

    /**
     * Sets optional {@link DataSealer} to use.
     * 
     * @param sealer data sealer
     */
    public void setDataSealer(@Nullable final DataSealer sealer) {
        checkSetterPreconditions();
        
        dataSealer = sealer;
    }
    
    /**
     * Sets whether tp pull username from existing session as a fallback.
     * 
     * <p>Defaults to false.</p>
     * 
     * @param flag flag to set
     */
    public void setCheckSession(final boolean flag) {
        checkSetterPreconditions();

        checkSession = flag;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }
        
        usernameContext = usernamePasswordContextCreationStrategy.apply(profileRequestContext);
        if (usernameContext == null) {
            log.warn("{} Unable to create UsernamePasswordContext, skipping action", getLogPrefix());
            return false;
        }
        
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        usernameContext.setUsername(null);
        usernameContext.setPassword(null);

        String username = getUsernameFromForm(profileRequestContext);
        if (username != null && !username.isEmpty()) {
            log.debug("{} Populating username '{}' from form submission into UsernamePasswordContext",
                    getLogPrefix(), username);
            usernameContext.setUsername(username);
            return;
        }

        username = getUsernameFromCookie(profileRequestContext);
        if (username != null && !username.isEmpty()) {
            log.debug("{} Populating cached username '{}' from cookie into UsernamePasswordContext",
                    getLogPrefix(), username);
            usernameContext.setUsername(username);
        }

        username = getUsernameFromSession(profileRequestContext, authenticationContext);
        if (username != null && !username.isEmpty()) {
            log.debug("{} Populating username '{}' from session into UsernamePasswordContext", getLogPrefix(),
                    username);
            usernameContext.setUsername(username);
        }
    }
    
    /**
     * Gets the username from a form submission.
     * 
     * <p>Also processes do-not-cache instruction.</p>
     * 
     * @param profileRequestContext profile request context
     * 
     * @return submitted username, after applying any configured transforms
     */
    @Nullable private String getUsernameFromForm(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        final HttpServletRequest request = getHttpServletRequest();
        if (request != null) {
            return applyTransforms(profileRequestContext, request.getParameter(usernameFieldName));
        }
        
        return null;
    }
    
    /**
     * Gets the username from an existing sealed cookie, if any.
     * 
     * @param profileRequestContext profile request context
     * 
     * @return username from existing sealed cookie, or null
     */
    @Nullable private String getUsernameFromCookie(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (cookieManager != null && dataSealer != null && cookieName != null) {
            final String cookie = URISupport.doURLDecode(cookieManager.getCookieValue(cookieName, null));
            if (cookie != null) {
                try {
                    assert dataSealer != null;
                    return dataSealer.unwrap(cookie);
                } catch (final DataSealerException e) {
                    log.warn("{} Unable to unwrap sealed username cookie", getLogPrefix(), e);
                    assert cookieName != null;
                    assert cookieManager != null;
                    cookieManager.unsetCookie(cookieName);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Gets the username from an existing {@link IdPSession}, if any.
     * 
     * @param profileRequestContext profile request context
     * @param authenticationContext authentication context
     * 
     * @return username from existing session, or null
     */
    @Nullable private String getUsernameFromSession(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (checkSession && !authenticationContext.getActiveResults().isEmpty()) {
            final SessionContext sessionContext = profileRequestContext.getSubcontext(SessionContext.class);
            if (sessionContext != null) {
                final IdPSession idpSession = sessionContext.getIdPSession();
                if (idpSession != null) {
                    return idpSession.getPrincipalName();
                }
            }
        }
        
        return null;
    }

}