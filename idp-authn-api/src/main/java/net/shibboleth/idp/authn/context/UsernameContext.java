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

package net.shibboleth.idp.authn.context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


import org.opensaml.messaging.context.BaseContext;

/**
 * Context that carries a username (without a password) to be validated.
 * 
 * @parent {@link AuthenticationContext}
 * @added After extracting a username during authentication
 */
public final class UsernameContext extends BaseContext {

    /** The username. */
    @Nullable private String username;

    /**
     * Gets the username.
     * 
     * @return the username
     */
    @Nullable public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     * 
     * @param name the username
     * 
     * @return this context
     */
    @Nonnull public UsernameContext setUsername(@Nullable final String name) {
        username = name;
        return this;
    }
    
}