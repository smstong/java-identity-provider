/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.authn;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/** A base class for authentication related actions. */
public abstract class AbstractAuthenticationAction extends AbstractIdentityProviderAction {

    /**
     * Strategy used to extract, and create if necessary, the {@link AuthenticationRequestContext} from the
     * {@link ProfileRequestContext}.
     */
    private Function<ProfileRequestContext, AuthenticationRequestContext> authnCtxLookupStrategy;

    /** {@inheritDoc} */
    protected final Event doExecute(@Nonnull final HttpServletRequest httpRequest,
            @Nonnull final HttpServletResponse httpResponse, @Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        AuthenticationRequestContext authenticationContext = authnCtxLookupStrategy.apply(profileRequestContext);
        if (authenticationContext == null) {
            throw new NoAuthenticationContextException();
        }

        return doExecute(httpRequest, httpResponse, springRequestContext, profileRequestContext, authenticationContext);
    }

    /**
     * Performs this authentication action.
     * 
     * @param httpRequest current HTTP request
     * @param httpResponse current HTTP response
     * @param springRequestContext current WebFlow request context
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     * 
     * @return the result of this action
     * 
     * @throws AuthenticationException thrown if there is a problem performing the authentication action
     */
    protected abstract Event doExecute(@Nonnull final HttpServletRequest httpRequest,
            @Nonnull final HttpServletResponse httpResponse, @Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationRequestContext authenticationContext) throws AuthenticationException;

    /** Exception thrown if there is no authentication exception available. */
    public static final class NoAuthenticationContextException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = -3111452312531745371L;

    }
}