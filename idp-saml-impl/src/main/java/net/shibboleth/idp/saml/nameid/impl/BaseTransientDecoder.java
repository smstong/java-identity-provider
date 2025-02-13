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

package net.shibboleth.idp.saml.nameid.impl;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;

import net.shibboleth.idp.saml.nameid.NameDecoderException;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * An abstract action which contains the logic to do transient decoding matching (shared between SAML2 and SAML1).
 */
public abstract class BaseTransientDecoder extends AbstractIdentifiableInitializableComponent {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BaseTransientDecoder.class);

    /** Store used to map identifiers to principals. */
    @NonnullAfterInit private StorageService idStore;

    /** cache for the log prefix - to save multiple recalculations. */
    @Nullable private String logPrefix;

    /**
     * Gets the ID store we are using.
     * 
     * @return the ID store we are using.
     */
    @NonnullAfterInit public StorageService getIdStore() {
        return idStore;
    }

    /**
     * Sets the ID store we should use.
     * 
     * @param store the store to use.
     */
    public void setIdStore(@Nonnull final StorageService store) {
        checkSetterPreconditions();
        idStore = Constraint.isNotNull(store, "StorageService cannot be null");
    }

    /**
     * Convert the transient Id into the principal.
     * 
     * @param transientId the transientID
     * @param requesterId the requested (SP)
     * 
     * @return the decoded principal
     * @throws NameDecoderException if a decode error occurs
     */
    @Nullable public String decode(@Nonnull final String transientId, @Nonnull @NotEmpty final String requesterId)
            throws NameDecoderException {
        checkComponentActive();

        try {
            final StorageRecord<?> record = idStore.read(TransientIdParameters.CONTEXT, transientId);
            if (null == record) {
                log.info("{} Could not find transient identifier", getLogPrefix());
                return null;
            }

            final TransientIdParameters param = new TransientIdParameters(record.getValue());

            if (!requesterId.equals(param.getAttributeRecipient())) {
                log.warn("{} Transient identifier issued to {} but requested by {}", getLogPrefix(),
                        param.getAttributeRecipient(), requesterId);
                throw new NameDecoderException("Misuse of identifier by an improper relying party");
            }

            return param.getPrincipal();
        } catch (final IOException e) {
            log.error("{} I/O error looking up transient identifier", getLogPrefix(), e);
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == idStore) {
            throw new ComponentInitializationException(getLogPrefix() + " No Id store set");
        }
        log.debug("{} using the store '{}'", getLogPrefix(), idStore.getId());
    }

    /**
     * Return a prefix for logging messages for this component.
     * 
     * @return a string for insertion at the beginning of any log messages
     */
    @Nonnull @NotEmpty protected String getLogPrefix() {
        // local cache of cached entry to allow unsynchronised clearing.
        String prefix = logPrefix;
        if (null == prefix) {
            final StringBuilder builder = new StringBuilder("Transient Decoder '").append(getId()).append("':");
            prefix = builder.toString();
            assert prefix !=null;
            if (null == logPrefix) {
                logPrefix = prefix;
            }
        }
        return prefix;
    }

}