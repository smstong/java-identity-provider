/*
 * Copyright 2011 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.authn;

/**
 * A authentication error which the current request can not recover from (e.g., a required authentication source being
 * down).
 */
public class UnrecoverableAuthenticationException extends AuthenticationException {

    /** Serial version UID. */
    private static final long serialVersionUID = -8576810879768090505L;

    /**
     * Constructor.
     * 
     * @param message exception message
     */
    public UnrecoverableAuthenticationException(final String message) {
        super(message);
    }

    /**
     * Constructor.
     * 
     * @param message exception message
     * @param wrappedException exception to be wrapped by this one
     */
    public UnrecoverableAuthenticationException(final String message, final Exception wrappedException) {
        super(message, wrappedException);
    }
}