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

package net.shibboleth.idp.profile.impl;

import java.util.function.Function;

import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;

/**
 * Returns the principal name from a {@link ResolverTestRequest} message in the inbound message context.
 */
public class ResolverTestPrincipalLookup implements Function<ProfileRequestContext,String> {

    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final ProfileRequestContext input) {
        
        final MessageContext messageContext = input != null ? input.getInboundMessageContext() : null;  
        if (messageContext != null) {
            final Object request = messageContext .getMessage();
            if (request != null && request instanceof ResolverTestRequest) {
                return ((ResolverTestRequest) request).getPrincipal();
            }
        }
        
        return null;
    }

}