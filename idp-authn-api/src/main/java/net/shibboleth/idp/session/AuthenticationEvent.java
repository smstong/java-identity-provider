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

package net.shibboleth.idp.session;

import java.security.Principal;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.messaging.context.AbstractSubcontextContainer;
import org.opensaml.util.Assert;
import org.opensaml.util.ObjectSupport;
import org.opensaml.util.StringSupport;

/** Describes an authentication event that took place within the scope of an {@link IdPSession}. */
@ThreadSafe
public final class AuthenticationEvent extends AbstractSubcontextContainer {

    /** The principal established by the authentication event. */
    private Principal authenticatedPrincipal;

    /** The identifier of the method used to authenticate the principal. */
    private String authenticationWorkflow;

    /** The time, in milliseconds since the epoch, that the authentication completed. */
    private long authenticationInstant;

    /** The last activity instant, in milliseconds since the epoch, for this event. */
    private long lastActivityInstant;

    /**
     * Constructor. Initializes authentication instant time to the current time.
     * 
     * @param workflow the workflow used to authenticate the principal, can not be null or empty
     * @param principal the principal that was authenticated, can not be null
     */
    public AuthenticationEvent(String workflow, Principal principal) {
        authenticationWorkflow = StringSupport.trimOrNull(workflow);
        Assert.isNotNull(authenticationWorkflow, "Authentication method can not be null nor empty");

        Assert.isNotNull(principal, "Authenticationed princpal can not be null");
        authenticatedPrincipal = principal;

        authenticationInstant = System.currentTimeMillis();
        lastActivityInstant = authenticationInstant;
    }

    /**
     * Gets the principal established by the authentication event.
     * 
     * @return principal established by the authentication event, never null
     */
    public Principal getPrincipal() {
        return authenticatedPrincipal;
    }

    /**
     * Gets the workflow used to authenticate the principal.
     * 
     * @return workflow used to authenticate the principal, never null
     */
    public String getAuthenticationWorkflow() {
        return authenticationWorkflow;
    }

    /**
     * Gets the time, in milliseconds since the epoch, that the authentication completed.
     * 
     * @return time, in milliseconds since the epoch, that the authentication completed, never less than 0
     */
    public long getAuthenticationInstant() {
        return authenticationInstant;
    }

    /**
     * Gets the last activity instant, in milliseconds since the epoch, for this event.
     * 
     * @return last activity instant, in milliseconds since the epoch, for this event, never less than 0
     */
    public long getLastActivityInstant() {
        return lastActivityInstant;
    }

    /**
     * Sets the last activity instant, in milliseconds since the epoch, for this event.
     * 
     * @param instant last activity instant, in milliseconds since the epoch, for this event, must be greater than 0
     */
    public void setLastActivityInstant(long instant) {
        Assert.isGreaterThan(0, instant, "Last activity instant must be greater than 0");
        lastActivityInstant = instant;
    }

    /**
     * Sets the last activity instant, in milliseconds since the epoch, for this event to the current time.
     */
    public void setLastActivityInstantToNow() {
        lastActivityInstant = System.currentTimeMillis();
    }

    /** {@inheritDoc} */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + authenticationWorkflow.hashCode();
        return result;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj instanceof AuthenticationEvent) {
            AuthenticationEvent other = (AuthenticationEvent) obj;
            return ObjectSupport.equals(getAuthenticationWorkflow(), other.getAuthenticationWorkflow());
        }

        return false;
    }
}