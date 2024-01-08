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

package net.shibboleth.idp.authn.principal;

import java.security.Principal;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.MultiFactorAuthenticationContext;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;

/**
 * Helper class for accessing {@link Principal} information.
 * 
 * @since 5.1.0
 */
public final class PrincipalSupport {
    
    /**
     * Private Constructor.
     */
    private PrincipalSupport() {
        
    }

    /**
     * Gets all of the names inside the {@link Principal} collection of a given type from a {@link Subject}.
     * 
     * @param subject input subject
     * @param claz type of Principal
     * 
     * @return set of names from matching Principals
     */
    @Nonnull @Unmodifiable @NotLive public static Set<String> getPrincipalNames(@Nonnull final Subject subject,
            @Nonnull final Class<? extends Principal> claz) {

        return subject.getPrincipals(claz).stream()
                .map(Principal::getName)
                .collect(CollectionSupport.nonnullCollector(Collectors.toUnmodifiableSet())).get();
    }

    /**
     * Gets all of the names inside the {@link Principal} collection of a given type from a {@link Subject}.
     * 
     * @param result authentication result
     * @param claz type of Principal
     * 
     * @return set of names from matching Principals
     */
    @Nonnull @Unmodifiable @NotLive public static Set<String> getPrincipalNames(
            @Nonnull final AuthenticationResult result, @Nonnull final Class<? extends Principal> claz) {
        return getPrincipalNames(result.getSubject(), claz);
    }
    
    /**
     * Gets all of the names inside the {@link Principal} collection of a given type from a {@link Subject}.
     * 
     * @param authenticationContext authentication context
     * @param flowId login flow Id
     * @param claz type of Principal
     * 
     * @return set of names from matching Principals
     */
    @Nonnull @Unmodifiable @NotLive public static Set<String> getPrincipalNames(
            @Nonnull final AuthenticationContext authenticationContext, @Nonnull final String flowId,
            @Nonnull final Class<? extends Principal> claz) {
        
        final AuthenticationResult result = authenticationContext.getActiveResults().get(flowId);
        return result != null ? getPrincipalNames(result.getSubject(), claz) : CollectionSupport.emptySet();
    }
    
    /**
     * Gets all of the names inside the {@link Principal} collection of a given type from a {@link Subject}.
     * 
     * @param mfaContext multi-factor authentication context
     * @param flowId login flow Id
     * @param claz type of Principal
     * 
     * @return set of names from matching Principals
     */
    @Nonnull @Unmodifiable @NotLive public static Set<String> getPrincipalNames(
            @Nonnull final MultiFactorAuthenticationContext mfaContext, @Nonnull final String flowId,
            @Nonnull final Class<? extends Principal> claz) {
        
        final AuthenticationResult result = mfaContext.getActiveResults().get(flowId);
        return result != null ? getPrincipalNames(result.getSubject(), claz) : CollectionSupport.emptySet();
    }

}