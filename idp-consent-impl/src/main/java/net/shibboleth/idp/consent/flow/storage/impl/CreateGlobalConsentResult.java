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

package net.shibboleth.idp.consent.flow.storage.impl;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.annotation.Nonnull;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.flow.impl.ConsentFlowDescriptor;
import net.shibboleth.idp.consent.storage.impl.ConsentResult;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorResult;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Consent action to create a consent result representing global consent to be stored in a storage service. Global
 * consent is represented by a consent object whose ID is the wildcard character, {@link Consent#WILDCARD}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @post See above.
 */
public class CreateGlobalConsentResult extends AbstractConsentIndexedStorageAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CreateGlobalConsentResult.class);

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        try {
            final Consent globalConsent = new Consent();
            globalConsent.setId(Consent.WILDCARD);
            globalConsent.setApproved(true);
            final String value = getStorageSerializer().serialize(
                    CollectionSupport.singletonMap(globalConsent.ensureId(), globalConsent));
            final ConsentFlowDescriptor flowDescriptor = getConsentFlowDescriptor();
            final String storageContext = getStorageContext();
            final String storageKey = getStorageKey();
            assert flowDescriptor!=null && storageContext!= null &&storageKey!= null; 
            final Duration lifetime = flowDescriptor.getLifetime();
            final Instant expiration;
            if (lifetime == null) {
                expiration = null;
            } else {
                expiration = Instant.now().plus(lifetime);
            }

            final ProfileInterceptorResult result =
                    new ConsentResult(storageContext, storageKey, value, expiration);

            log.debug("{} Created global consent result '{}'", getLogPrefix(), result);

            storeResultWithIndex(profileRequestContext, result);

        } catch (final IOException e) {
            log.debug("{} Unable to serialize consent", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
        }
    }
}
