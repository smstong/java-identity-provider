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

package net.shibboleth.idp.saml.nameid.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.saml.nameid.NameDecoderException;
import net.shibboleth.idp.saml.nameid.NameIdentifierDecoder;

import org.opensaml.saml.saml1.core.NameIdentifier;

/** Transform from a {@link NameIdentifier}. */
public class TransformingNameIdentifierDecoder extends BaseTransformingDecoder implements NameIdentifierDecoder {

    /** {@inheritDoc} */
    @Override
    @Nullable public String decode(@Nonnull final SubjectCanonicalizationContext c14nContext,
            @Nonnull final NameIdentifier nameIdentifier) throws NameDecoderException {
        
        final String value = nameIdentifier.getValue();
        assert value != null;
        return decode(value);
    }
    
}