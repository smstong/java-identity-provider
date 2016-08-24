/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import net.shibboleth.utilities.java.support.logic.Constraint;
import org.joda.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * CAS proxy ticket.
 *
 * @author Marvin S. Addison
 */
public class ProxyTicket extends Ticket {

    /** Proxy-granting ticket used to create ticket. */
    @Nonnull
    private final String pgtId;

    /**
     * Deprecated. Session IDs are now optional and should be specified via {@link TicketState#setSessionId(String)}
     * and {@link #setTicketState(TicketState)}.
     *
     * @param id Ticket ID.
     * @param sessionId IdP session ID used to create ticket.
     * @param service Service that requested the ticket.
     * @param expiration Expiration instant.
     * @param pgtId Proxy-granting ticket ID used to create ticket.
     */
    @Deprecated
    public ProxyTicket(
            @Nonnull final String id,
            @Nullable final String sessionId,
            @Nonnull final String service,
            @Nonnull final Instant expiration,
            @Nonnull final String pgtId) {
        super(id, sessionId, service, expiration);
        this.pgtId = Constraint.isNotNull(pgtId, "PgtId cannot be null");
    }

    /**
     * Creates a new authenticated ticket with an identifier, service, and expiration date.
     *
     * @param id Ticket ID.
     * @param service Service that requested the ticket.
     * @param expiration Expiration instant.
     * @param pgtId Proxy-granting ticket ID used to create ticket.
     */
    public ProxyTicket(
            @Nonnull final String id,
            @Nonnull final String service,
            @Nonnull final Instant expiration,
            @Nonnull final String pgtId) {
        super(id, service, expiration);
        this.pgtId = Constraint.isNotNull(pgtId, "PgtId cannot be null");
    }

    @Nonnull
    public String getPgtId() {
        return pgtId;
    }

    @Override
    protected Ticket newInstance(final String newId) {
        return new ProxyTicket(newId, getService(), getExpirationInstant(), pgtId);
    }
}
