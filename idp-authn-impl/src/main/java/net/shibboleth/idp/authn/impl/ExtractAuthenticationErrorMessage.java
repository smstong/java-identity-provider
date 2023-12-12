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

import java.util.Locale;
import java.util.function.Function;
import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.AuthenticationErrorContext;
import org.jetbrains.annotations.NotNull;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

/**
 * Responsible for producing a single error message to be displayed on the login form on authentication failure.
 * Supersedes the Velocity template login-error.vm from previous IdP versions.
 *
 * @since 5.1.0
 */
public class ExtractAuthenticationErrorMessage extends AbstractAuthenticationAction {

    private static final Object[] NO_ARGS = new Object[0];

    private Function<AuthenticationContext, AuthenticationErrorContext> errorContextLookup =
            new ChildContextLookup<>(AuthenticationErrorContext.class);

    @Override
    protected boolean doPreExecute(@NotNull ProfileRequestContext profileRequestContext, @NotNull AuthenticationContext authenticationContext) {
        return errorContextLookup.apply(authenticationContext) != null;
    }

    @Override
    protected void doExecute(@NotNull ProfileRequestContext profileRequestContext, @NotNull AuthenticationContext authenticationContext) {
        final AuthenticationErrorContext errorContext = errorContextLookup.apply(authenticationContext);
        final String eventId = errorContext.getClassifiedErrors().iterator().next();
        final Locale locale = Locale.getDefault();
        String message = "Authentication failure";
        if (!"ReselectFlow".equals(eventId)) {
            final String eventKey = getMessage(eventId, NO_ARGS, "authn", locale);
            message = getMessage(eventKey + ".message", NO_ARGS, "Login Failure: " + eventId, locale);
        } else if (!errorContext.getExceptions().isEmpty()) {
           final Exception ex = errorContext.getExceptions().get(0);
           message = ex.getMessage() != null ? "Login Failure: " + ex.getMessage() : ex.toString();
        }
        errorContext.setDisplayErrorMessage(message);
    }
}
