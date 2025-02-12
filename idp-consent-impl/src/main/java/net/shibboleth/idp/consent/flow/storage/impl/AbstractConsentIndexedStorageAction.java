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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageSerializer;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;

import net.shibboleth.idp.consent.flow.impl.ConsentFlowDescriptor;
import net.shibboleth.idp.consent.storage.impl.CollectionSerializer;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorResult;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Base class for consent actions which write to a {@link org.opensaml.storage.StorageService}.
 * 
 * To facilitate lookup of all storage keys for a storage context, an index record is maintained containing the storage
 * keys for the context. Because storage records may expire, the index record may contain keys which no longer exist in
 * the storage service.
 */
public class AbstractConsentIndexedStorageAction extends AbstractConsentStorageAction {

    /** Default storage key for the storage index record. */
    @Nonnull @NotEmpty public static final String DEFAULT_STORAGE_INDEX_KEY = "_key_idx";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractConsentIndexedStorageAction.class);

    /** Storage key of index record. */
    @Nullable private String storageIndexKey;

    /** Strategy used to determine the storage key of the index record. */
    @NonnullAfterInit private Function<ProfileRequestContext, String> storageIndexKeyLookupStrategy;

    /** Strategy used to manipulate the storage keys when pruning storage records. */
    @Nullable private Function<Pair<ProfileRequestContext, List<String>>, List<String>> storageKeysStrategy;

    /** Storage keys serializer used to serialize the value of the storage key index record. */
    @Nonnull private StorageSerializer<Collection<String>> storageKeysSerializer;

    /** Constructor. */
    public AbstractConsentIndexedStorageAction() {
        storageKeysSerializer = new CollectionSerializer();
    }

    /**
     * Get the storage keys serializer used to serialize the value of the storage key index record.
     * 
     * @return the storage keys serializer
     */
    @Nonnull public StorageSerializer<Collection<String>> getStorageKeysSerializer() {
        return storageKeysSerializer;
    }

    /**
     * Set the storage index key lookup strategy.
     * 
     * @param strategy the storage index key lookup strategy
     */
    public void setStorageIndexKeyLookupStrategy(@Nonnull final Function<ProfileRequestContext, String> strategy) {
        checkSetterPreconditions();
        storageIndexKeyLookupStrategy =
                Constraint.isNotNull(strategy, "Storage index key lookup strategy cannot be null");
    }

    /**
     * Set the storage keys serializer used to serialize the value of the storage key index record.
     * 
     * @param serializer the storage keys serializer
     */
    public void setStorageKeysSerializer(@Nonnull final StorageSerializer<Collection<String>> serializer) {
        checkSetterPreconditions();
        storageKeysSerializer = Constraint.isNotNull(serializer, "Storage keys serializer cannot be null");
    }

    /**
     * Set the storage keys strategy used to manipulate the storage keys when pruning storage records.
     * 
     * @param strategy the storage keys strategy
     */
    public void setStorageKeysStrategy(
            @Nonnull final Function<Pair<ProfileRequestContext, List<String>>, List<String>> strategy) {
        checkSetterPreconditions();
        storageKeysStrategy = Constraint.isNotNull(strategy, "Storage keys strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (storageIndexKeyLookupStrategy == null) {
            throw new ComponentInitializationException("Storage key lookup strategy cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        if (!super.doPreExecute(profileRequestContext, interceptorContext)) {
            return false;
        }

        storageIndexKey = storageIndexKeyLookupStrategy.apply(profileRequestContext);
        log.trace("{} Storage index key '{}'", getLogPrefix(), storageIndexKey);
        if (storageIndexKey == null) {
            log.debug("{} No storage index key", getLogPrefix());
            return false;
        }

        return true;
    }

    /**
     * Get the storage key resulting from applying the storage key lookup strategy.
     * 
     * @return the storage key
     */
    @Nullable protected String getStorageIndexKey() {
        return storageIndexKey;
    }

    /**
     * Get the storage keys from the storage index record.
     * 
     * @return the storage keys from the storage index record
     * @throws IOException if errors occur in the read process
     */
    @Nonnull @Unmodifiable @NotLive protected List<String> getStorageKeysFromIndex() throws IOException {

        final StorageService service = getStorageService();
        final String context = getStorageContext();
        final String indexKey = getStorageIndexKey();
        assert service != null && indexKey != null && context!= null;
        final StorageRecord<Collection<String>> storageRecord = service.read(context, indexKey);

        log.debug("{} Read storage record '{}' with context '{}' and key '{}'", getLogPrefix(), storageRecord,
                getStorageContext(), getStorageIndexKey());

        if (storageRecord == null) {
            return CollectionSupport.emptyList();
        }

        return new ArrayList<>(storageRecord.getValue(getStorageKeysSerializer(), context, indexKey));
    }

    /**
     * Add a storage key to the index storage record.
     * 
     * @param keyToAdd storage key to add to the index storage record
     * @return true if key addition succeeded, false otherwise
     * @throws IOException if an error occurs writing to the storage service
     */
    protected boolean addKeyToStorageIndex(@Nonnull final String keyToAdd) throws IOException {

        final StorageService service = getStorageService();
        final String storageContext = getStorageContext();
        final String indexKey = getStorageIndexKey();
        assert service != null && indexKey != null && storageContext!= null;
        final StorageRecord<?> storageRecord = service.read(storageContext, indexKey);
        log.debug("{} Read storage record '{}' with context '{}' and key '{}'", getLogPrefix(), storageRecord,
                getStorageContext(), getStorageIndexKey());

        if (storageRecord == null) {
            log.debug("{} Creating storage index with key '{}'", getLogPrefix(), keyToAdd);
            return service.create(storageContext, indexKey, CollectionSupport.singletonList(keyToAdd),
                    storageKeysSerializer, null);
        }
        
        final LinkedHashSet<String> keys = new LinkedHashSet<>(getStorageKeysFromIndex());
        if (keys.add(keyToAdd)) {
            log.debug("{} Updating storage index by adding key '{}'", getLogPrefix(), keyToAdd);
            return service.update(storageContext, indexKey, keys, storageKeysSerializer, null);
        }
        
        log.debug("{} Storage key '{}' already indexed, nothing to do", getLogPrefix(), keyToAdd);
        return false;
    }

    /**
     * Remove a storage key from the index storage record.
     * 
     * @param keyToRemove storage key to remove from the index storage record
     * @return true if key removal succeeded, false otherwise
     * @throws IOException if an error occurs writing to the storage service
     */
    protected boolean removeKeyFromStorageIndex(@Nonnull final String keyToRemove) throws IOException {

        final StorageService service = getStorageService();
        final String storageContext = getStorageContext();
        final String indexKey = getStorageIndexKey();
        assert service != null && indexKey != null && storageContext!= null;

        final StorageRecord<?> storageRecord = service.read(storageContext, indexKey);
        log.debug("{} Read storage record '{}' with context '{}' and key '{}'", getLogPrefix(), storageRecord,
                getStorageContext(), getStorageIndexKey());

        if (storageRecord == null) {
            log.debug("{} No storage record exists with context '{}' and key '{}', nothing to do", getLogPrefix(),
                    getStorageContext(), getStorageIndexKey());
            return false;
        }
        
        final LinkedHashSet<String> keys = new LinkedHashSet<>(getStorageKeysFromIndex());
        if (keys.remove(keyToRemove)) {
            log.debug("{} Updating storage index by removing key '{}'", getLogPrefix(), keyToRemove);
            assert storageIndexKey != null;
            return service.update(storageContext, storageIndexKey, keys, storageKeysSerializer,
                    null);
        }
        
        log.debug("{} Storage key '{}' not indexed, nothing to do", getLogPrefix(), keyToRemove);
        return false;
    }

// CheckStyle: CyclomaticComplexity OFF
    /**
     * Storage records will be pruned based on the record maximums set on the flow descriptor,
     * and the storage service value size. Below a defined threshold, the basic maximum is applied, while at
     * that storage size, an expanded maximum is applied.
     * 
     * <p>The function used to determine the records to be deleted may be set by calling
     * {@link #setStorageKeysStrategy(Function)}. By default, records are deleted on a first-in-first-out basis,
     * meaning the oldest storage records are deleted first.</p>
     * 
     * @param profileRequestContext the profile request context
     * 
     * @throws IOException if an error occurs writing to the storage service
     */
    protected void pruneStorageRecords(@Nonnull final ProfileRequestContext profileRequestContext) throws IOException {

        final ConsentFlowDescriptor flowDescriptor = getConsentFlowDescriptor();
        final StorageService service = getStorageService();
        final String storageContext = getStorageContext();
        assert service != null && flowDescriptor!=null &&storageContext!= null;
        int maxStoredRecords = flowDescriptor.getMaximumNumberOfStoredRecords();
        if (service.getCapabilities().getValueSize() >= flowDescriptor.getExpandedStorageThreshold()) {
            maxStoredRecords = flowDescriptor.getExpandedNumberOfStoredRecords();
        }
        
        if (maxStoredRecords <= 0) {
            log.trace("{} Will not prune storage records, maximum number of records is not greater than zero",
                    getLogPrefix());
            return;
        }

        List<String> keys = getStorageKeysFromIndex();

        if (keys.size() < maxStoredRecords) {
            log.debug("{} Will not prune storage records, number of keys '{}' is less than max number of records '{}'",
                    getLogPrefix(), keys.size(), maxStoredRecords);
            return;
        }

        if (storageKeysStrategy != null) {
            final List<String> sortedKeys = storageKeysStrategy.apply(new Pair<>(profileRequestContext, keys));
            if (sortedKeys != null) {
                keys = sortedKeys;
            }
        }

        int numberOfKeys = keys.size();

        final Iterator<String> keysIterator = keys.iterator();

        while (keysIterator.hasNext() && numberOfKeys >= maxStoredRecords) {

            final String keyToDelete = keysIterator.next();
            assert keyToDelete!=null;
            log.debug("{} Pruning storage record with key '{}'. There are '{}' records of max '{}' ", getLogPrefix(),
                    keyToDelete, numberOfKeys, maxStoredRecords);

            log.debug("{} Deleting storage record with context '{}' and key '{}'", getLogPrefix(), getStorageContext(),
                    keyToDelete);
            final boolean success = service.delete(storageContext, keyToDelete);

            if (success) {
                numberOfKeys--;
            }

            log.debug("{} Removing key '{}' from storage index", getLogPrefix(), keyToDelete);
            removeKeyFromStorageIndex(keyToDelete);
        }
    }
// CheckStyle: CyclomaticComplexity ON

    /**
     * Store a profile interceptor result.
     * 
     * @param result the profile interceptor result to be stored
     * @throws IOException if an error occurs
     * @return boolean whether the record was stored successfully
     */
    protected boolean storeResult(@Nonnull final ProfileInterceptorResult result) throws IOException {

        final String context = result.getStorageContext();
        final String key = result.getStorageKey();
        final String value = result.getStorageValue();
        final Instant expiration = result.getStorageExpiration();

        // Create / update loop until we succeed or exhaust attempts.
        int attempts = 10;
        boolean success = false;
        do {
            final StorageService service = getStorageService();
            assert service != null;
            success = service.create(context, key, value,
                    expiration != null ? expiration.toEpochMilli() : null);
            if (!success) {
                // The record already exists, so we need to overwrite via an update.
                success = service .update(context, key, value,
                        expiration != null ? expiration.toEpochMilli() : null);
            }
        } while (!success && attempts-- > 0);

        if (!success) {
            log.error("{} Exhausted retry attempts storing result '{}'", getLogPrefix(), result);
        }

        return success;
    }

    /**
     * Store a profile interceptor result and maintain an index record containing the storage keys for the storage
     * context. Storage records are pruned so that the number of records stored is less than or equal to
     * {@link net.shibboleth.idp.consent.flow.impl.ConsentFlowDescriptor#getMaximumNumberOfStoredRecords()}.
     * 
     * @param profileRequestContext the profile request context
     * @param result the profile interceptor result to be stored
     * @throws IOException if an error occurs
     */
    protected void storeResultWithIndex(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorResult result) throws IOException {

        pruneStorageRecords(profileRequestContext);

        storeResult(result);

        addKeyToStorageIndex(result.getStorageKey());
    }

}