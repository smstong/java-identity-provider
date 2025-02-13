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

package net.shibboleth.idp.cas.config;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.logic.Constraint;

/**
 * Lookup function for extracting CAS profile configuration from the profile request context.
 *
 *@param <T> type of profile configuration
 *
 * @author Marvin S. Addison
 */
public class ConfigLookupFunction<T extends AbstractProtocolConfiguration>
    implements Function<ProfileRequestContext, T> {

    /** Type of profile configuration class. */
    @Nonnull private final Class<T> configClass;

    /**
     * Creates a new instance.
     *
     * @param clazz Profile configuration class.
     */
    public ConfigLookupFunction(@Nonnull final Class<T> clazz) {
        configClass = Constraint.isNotNull(clazz, "Configuration class cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable public T apply(@Nullable final ProfileRequestContext profileRequestContext) {
        if (profileRequestContext != null) {
            final RelyingPartyContext rpContext = profileRequestContext.getSubcontext(RelyingPartyContext.class);
            if (rpContext != null && configClass.isInstance(rpContext.getProfileConfig())) {
                return configClass.cast(rpContext.getProfileConfig());
            }
        }
        
        return null;
    }

}