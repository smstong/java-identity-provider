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

package net.shibboleth.idp.authn.config.navigate;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nullable;

import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;
import net.shibboleth.idp.profile.context.navigate.AbstractRelyingPartyLookupFunction;
import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;

import org.opensaml.profile.context.ProfileRequestContext;

/**
 * A function that returns {@link AuthenticationProfileConfiguration#getAuthenticationFlows}()
 * if such a profile is available from a {@link RelyingPartyContext} obtained via a lookup function,
 * by default a child of the {@link ProfileRequestContext}.
 * 
 * <p>If a specific setting is unavailable, no values are returned.</p>
 */
public class AuthenticationFlowsLookupFunction extends AbstractRelyingPartyLookupFunction<Collection<String>> {

    /** {@inheritDoc} */
    @Override
    @Nullable @NonnullElements @NotLive @Unmodifiable public Collection<String> apply(
            @Nullable final ProfileRequestContext input) {
        final RelyingPartyContext rpc = getRelyingPartyContextLookupStrategy().apply(input);
        if (rpc != null) {
            final ProfileConfiguration pc = rpc.getProfileConfig();
            if (pc != null && pc instanceof AuthenticationProfileConfiguration) {
                return ((AuthenticationProfileConfiguration) pc).getAuthenticationFlows(input);
            }
        }
        
        return Collections.emptyList();
    }

}