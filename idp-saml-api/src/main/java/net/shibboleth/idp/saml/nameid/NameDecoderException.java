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

package net.shibboleth.idp.saml.nameid;

import javax.annotation.Nullable;

import net.shibboleth.idp.authn.SubjectCanonicalizationException;

/** Error thrown if decoding of a SAML subject identifier fails. */
public class NameDecoderException extends SubjectCanonicalizationException {

    /**
     * serial ID.
     */
    private static final long serialVersionUID = 2733354405831675687L;

    /**
     * Constructor.
     *
     */
    public NameDecoderException() {
        
    }

    /**
     * Constructor.
     * 
     * @param message exception message
     */
    public NameDecoderException(@Nullable final String message) {
        super(message);
    }

    /**
     * Constructor.
     * 
     * @param wrappedException exception to be wrapped by this one
     */
    public NameDecoderException(@Nullable final Exception wrappedException) {
        super(wrappedException);
    }

    /**
     * Constructor.
     * 
     * @param message exception message
     * @param wrappedException exception to be wrapped by this one
     */
    public NameDecoderException(@Nullable final String message,
            @Nullable final Exception wrappedException) {
        super(message, wrappedException);
    }
    
}