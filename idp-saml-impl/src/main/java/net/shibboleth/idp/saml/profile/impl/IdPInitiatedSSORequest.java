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

package net.shibboleth.idp.saml.profile.impl;

import java.time.Instant;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

import com.google.common.base.MoreObjects;

/**
 * Object representing a Shibboleth Authentication Request message.
 * 
 * This message is used for IdP-initiated authentication requests and is defined by the <a
 * href="http://shibboleth.internet2.edu/docs/internet2-mace-shibboleth-arch-protocols-200509.pdf">Shibboleth
 * Architecture Protocol and Profiles</a> specification. Note, this document was written prior to the creation of
 * SAML 2 and so only mentioned version 1 but this message may be used with either version. The SAML 2
 * authentication request should be used by SAML 2 service providers wishing to initiate authentication.
 */
@ThreadSafe
public class IdPInitiatedSSORequest {
    
    /** The entityID of the requesting service provider. */
    @Nonnull @NotEmpty private final String entityId;

    /**
     * The assertion consumer service endpoint, at the service provider, to which to deliver the authentication
     * response.
     */
    @Nullable private final String acsURL;
    
    /** An opaque value to be returned to the service provider with the authentication response. */
    @Nullable private final String relayState;

    /** The current time, at the service provider. */
    @Nonnull private final Instant time;

    /**
     * Constructor.
     * 
     * <p>If no message time is supplied, then the current time at the IdP is used.</p>
     * 
     * @param newEntityId entity ID of the requesting SP
     * @param url assertion consumer service endpoint at the SP to which to deliver the response
     * @param target opaque value to be returned to the SP with the response
     * @param newTime current time at the SP
     */
    public IdPInitiatedSSORequest(@Nonnull @NotEmpty final String newEntityId, @Nullable final String url,
            @Nullable final String target, @Nullable final Instant newTime) {
        
        entityId = Constraint.isNotNull(StringSupport.trimOrNull(newEntityId),
                "Service provider ID cannot be null or empty");

        acsURL = StringSupport.trimOrNull(url);
        relayState = StringSupport.trimOrNull(target);
        
        if (newTime != null) {
            time = newTime;
        } else {
            final Instant now = Instant.now();
            assert now != null;
            time = now;
        }
    }

    /**
     * Get the entityID of the requesting relying party.
     * 
     * @return entityID of the requesting relying party
     */
    @Nonnull @NotEmpty public String getEntityId() {
        return entityId;
    }

    /**
     * Get the assertion consumer service endpoint at the SP to which to deliver the response.
     * 
     * @return assertion consumer service endpoint at the SP to which to deliver the response
     */
    @Nullable public String getAssertionConsumerServiceURL() {
        return acsURL;
    }

    /**
     * Get the opaque value to be returned to the SP with the response.
     * 
     * @return opaque value to be returned to the SP with the response
     */
    @Nullable public String getRelayState() {
        return relayState;
    }

    /**
     * Get the current time at the SP.
     * 
     * @return current time at the SP 
     */
    @Nonnull public Instant getTime() {
        return time;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("entityId", entityId)
            .add("acsURL", acsURL)
            .add("relayState", relayState)
            .add("time", time)
            .toString();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = prime * result + entityId.hashCode();
        final String url = acsURL;  
        if (url != null) {
            result = prime * result + url.hashCode();
        } else {
            result = prime * result + 0;
        }
        final String state = relayState;
        if (state != null) {
            result = prime * result + state.hashCode();
        } else {
            result = prime * result + 0;
        }

        result = prime * result + (int) (time.toEpochMilli() ^ (time.toEpochMilli() >>> 32));

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof IdPInitiatedSSORequest)) {
            return false;
        }

        final IdPInitiatedSSORequest other = (IdPInitiatedSSORequest) obj;
        return Objects.equals(entityId, other.entityId) && Objects.equals(acsURL, other.acsURL)
                && Objects.equals(relayState, other.relayState) && time.equals(other.time);
    }
    
}