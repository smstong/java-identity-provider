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
import java.util.Map;

import javax.annotation.Nonnull;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageSerializer;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.shared.primitive.LoggerFactory;
/**
 * Consent action which reads consent records from storage and adds the serialized consent records to the consent
 * context as previous consents.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 */
public class ReadConsentFromStorage extends AbstractConsentStorageAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ReadConsentFromStorage.class);

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        final String storageContext = getStorageContext();
        final ConsentContext consentContext = getConsentContext(); 
        final String key = getStorageKey();
        final StorageService service = getStorageService();
        final StorageSerializer<Map<String, Consent>> storageSerializer = getStorageSerializer();
        assert consentContext != null && service != null && key != null && storageContext!= null
                && storageSerializer!=null;

        try {
            final StorageRecord<Map<String,Consent>> storageRecord = service.read(storageContext, key);
            log.debug("{} Read storage record '{}' with context '{}' and key '{}'", getLogPrefix(), storageRecord,
                    storageContext, key);

            if (storageRecord == null) {
                log.debug("{} No storage record for context '{}' and key '{}'", getLogPrefix(), storageContext, key);
                return;
            }

            final Map<String,Consent> consents = storageRecord.getValue(storageSerializer, storageContext, key);

            consentContext.getPreviousConsents().putAll(consents);

        } catch (final IOException e) {
            log.error("{} Unable to read consent from storage", getLogPrefix(), e);
        }
    }

}