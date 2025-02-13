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

package net.shibboleth.idp.cas.ticket;

import java.time.Instant;

import javax.annotation.Nonnull;

/**
 * CAS service ticket.
 *
 * @author Marvin S. Addison
 */
public class ServiceTicket extends Ticket {

    /** Forced authentication flag. */
    private final boolean forceAuthn;

    /**
     * Creates a new authenticated ticket with an identifier, service, and expiration date.
     *
     * @param id Ticket ID.
     * @param service Service that requested the ticket.
     * @param expiration Expiration instant.
     * @param renew True if ticket was issued from forced authentication, false otherwise.
     */
    public ServiceTicket(
            @Nonnull final String id,
            @Nonnull final String service,
            @Nonnull final Instant expiration,
            final boolean renew) {
        super(id, service, expiration);
        forceAuthn = renew;
    }

    /**
     * Get whether ticket was issued from forced authentication.
     * 
     * @return true if ticket was issued from forced authentication, false otherwise
     */
    public boolean isRenew() {
        return forceAuthn;
    }

    /** {@inheritDoc} */
    @Override
    protected Ticket newInstance(@Nonnull final String newId) {
        return new ServiceTicket(newId, getService(), getExpirationInstant(), forceAuthn);
    }

}