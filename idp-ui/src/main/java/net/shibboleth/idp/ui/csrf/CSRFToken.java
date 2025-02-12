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

package net.shibboleth.idp.ui.csrf;

import java.io.Serializable;

import javax.annotation.Nonnull;

/**
 * An anti cross-site request forgery token. 
 */
public interface CSRFToken extends Serializable{

    /**
     * The name to be used in HTML form input elements to store the CSRF token value. 
     * Expected to be present as a parameter in a HTTP request.
     * 
     * @return the HTTP parameter name that contains the value of the token.
     */
    @Nonnull String getParameterName();
    
    /**
     * The anti-CSRF token value. The token should be cryptographically strong.
     * 
     * @return the anti-CSRF token.
     */
    @Nonnull String getToken();

}