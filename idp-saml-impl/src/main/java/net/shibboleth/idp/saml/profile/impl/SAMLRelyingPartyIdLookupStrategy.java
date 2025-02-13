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

package net.shibboleth.idp.saml.profile.impl;

import javax.annotation.Nullable;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLSelfEntityContext;

import net.shibboleth.profile.context.RelyingPartyContext;

/**
 * A lookup strategy that returns a SAML entityID if the {@link RelyingPartyContext} contains a reference
 * to a {@link SAMLPeerEntityContext} or {@link SAMLSelfEntityContext}.
 */
public class SAMLRelyingPartyIdLookupStrategy implements ContextDataLookupFunction<RelyingPartyContext, String> {

    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final RelyingPartyContext input) {
        
        final BaseContext ctx = input != null ? input.getRelyingPartyIdContextTree() : null;
        if (ctx != null) {
            if (ctx instanceof SAMLPeerEntityContext) {
                return ((SAMLPeerEntityContext) ctx).getEntityId();
            } else if (ctx instanceof SAMLSelfEntityContext) {
                return ((SAMLSelfEntityContext) ctx).getEntityId();
            }
        }
        
        return null;
    }

}