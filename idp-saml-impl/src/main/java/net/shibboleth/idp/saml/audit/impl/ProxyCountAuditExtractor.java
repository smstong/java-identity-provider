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

package net.shibboleth.idp.saml.audit.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.ProxyRestriction;

/**
 * {@link Function} that returns {@link ProxyRestriction#getProxyCount()}.
 * 
 * @since 4.2.0
 */
public class ProxyCountAuditExtractor extends AbstractProxyRestrictionAuditExtractor<Integer> {
    
    /**
     * Constructor.
     *
     * @param strategy lookup strategy for message
     */
    public ProxyCountAuditExtractor(@Nonnull final Function<ProfileRequestContext,SAMLObject> strategy) {
        super(strategy);
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected Integer doApply(@Nullable final ProxyRestriction condition) {
        if (condition != null) {
            return condition.getProxyCount();
        }
        
        return null;
    }
    
}