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

package net.shibboleth.idp.profile.interceptor;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.IdentifiedComponent;

/**
 * Represents the result of a profile interceptor flow intended for storage by a
 * {@link org.opensaml.storage.StorageService}.
 */
public interface ProfileInterceptorResult extends IdentifiedComponent {

    /**
     * Get the storage context.
     * 
     * @return the storage context
     */
    @Nonnull @NotEmpty String getStorageContext();

    /**
     * Get the storage key.
     * 
     * @return the storage key
     */
    @Nonnull @NotEmpty String getStorageKey();

    /**
     * Get the storage value.
     * 
     * @return the storage value
     */
    @Nonnull @NotEmpty String getStorageValue();

    /**
     * Get the storage expiration.
     * 
     * @return the storage expiration
     */
    @Nullable Instant getStorageExpiration();
    
}