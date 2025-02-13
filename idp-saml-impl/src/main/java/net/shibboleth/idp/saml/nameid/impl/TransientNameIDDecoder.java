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
import net.shibboleth.idp.saml.nameid.NameIDDecoder;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

import org.opensaml.saml.saml2.core.NameID;

/**
 * Decodes {@link NameID#getValue()}  via the base class (reversing the work done by
 * {@link TransientSAML2NameIDGenerator}).
 */
public class TransientNameIDDecoder extends BaseTransientDecoder implements NameIDDecoder {

    /** {@inheritDoc} */
    @Override
    @Nullable @NotEmpty public String decode(@Nonnull final SubjectCanonicalizationContext c14nContext,
            @Nonnull final NameID nameID) throws NameDecoderException {

        final String value = nameID.getValue();
        final String id = c14nContext.getRequesterId();
        assert value != null && id != null;

        return super.decode(value, id);
    }

}