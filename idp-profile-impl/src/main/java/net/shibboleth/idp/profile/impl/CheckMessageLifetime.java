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

package net.shibboleth.idp.profile.impl;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.InvalidProfileRequestContextStateException;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.joda.time.DateTime;
import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/** An action that checks that the inbound message should be considered valid based upon when it was issued. */
public final class CheckMessageLifetime extends AbstractProfileAction {

    /** Amount of time, in milliseconds, for which a message is valid. Default value: 5 minutes */
    private long messageLifetime;

    /**
     * Strategy used to look up the {@link RelyingPartyContext} associated with the given {@link ProfileRequestContext}.
     */
    private Function<ProfileRequestContext, RelyingPartyContext> rpContextLookupStrategy;

    /**
     * Strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound message context.
     */
    private Function<MessageContext, BasicMessageMetadataContext> messageMetadataContextLookupStrategy;

    /**
     * Constructor.
     * 
     * Initializes {@link #messageLifetime} to 5 minutes. Initializes {@link #rpContextLookupStrategy} to
     * {@link ChildContextLookup}. Initializes {@link #messageMetadataContextLookupStrategy} to
     * {@link ChildContextLookup}.
     */
    public CheckMessageLifetime() {
        super();

        messageLifetime = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

        rpContextLookupStrategy =
                new ChildContextLookup<ProfileRequestContext, RelyingPartyContext>(RelyingPartyContext.class, false);

        messageMetadataContextLookupStrategy =
                new ChildContextLookup<MessageContext, BasicMessageMetadataContext>(BasicMessageMetadataContext.class,
                        false);
    }

    /**
     * Gets the strategy used to look up the {@link RelyingPartyContext} associated with the given
     * {@link ProfileRequestContext}.
     * 
     * @return strategy used to look up the {@link RelyingPartyContext} associated with the given
     *         {@link ProfileRequestContext}
     */
    @Nonnull public Function<ProfileRequestContext, RelyingPartyContext> getRelyingPartContextLookupStrategy() {
        return rpContextLookupStrategy;
    }

    /**
     * Sets the strategy used to look up the {@link RelyingPartyContext} associated with the given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to look up the {@link RelyingPartyContext} associated with the given
     *            {@link ProfileRequestContext}
     */
    public synchronized void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        rpContextLookupStrategy = Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy can not be null");
    }

    /**
     * Gets the strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound message
     * context.
     * 
     * @return strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound message
     *         context
     */
    public Function<MessageContext, BasicMessageMetadataContext> getMessageMetadataContextLookupStrategy() {
        return messageMetadataContextLookupStrategy;
    }

    /**
     * Sets the strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound message
     * context.
     * 
     * @param strategy strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound
     *            message context
     */
    public synchronized void setMessageMetadataContextLookupStrategy(
            @Nonnull final Function<MessageContext, BasicMessageMetadataContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        messageMetadataContextLookupStrategy =
                Constraint.isNotNull(strategy, "Message metadata context lookup strategy can not be null");
    }

    /**
     * Gets the amount of time, in milliseconds, for which a message is valid.
     * 
     * @return amount of time, in milliseconds, for which a message is valid
     */
    public long getMessageLifetime() {
        return messageLifetime;
    }

    /**
     * Sets the amount of time, in milliseconds, for which a message is valid.
     * 
     * @param lifetime amount of time, in milliseconds, for which a message is valid
     */
    public synchronized void setMessageLifetime(long lifetime) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Action " + getId()
                    + ": Message liftime can not be changed after action has been initialized");
        }

        messageLifetime = lifetime;
    }

    /** {@inheritDoc} */
    protected Event doExecute(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            RequestContext springRequestContext, ProfileRequestContext profileRequestContext) throws ProfileException {

        final RelyingPartyContext relyingPartyCtx = rpContextLookupStrategy.apply(profileRequestContext);
        // TODO check for null

        final BasicMessageMetadataContext messageSubcontext =
                messageMetadataContextLookupStrategy.apply(profileRequestContext.getInboundMessageContext());

        if (messageSubcontext.getMessageIssueInstant() <= 0) {
            throw new InvalidProfileRequestContextStateException(
                    "Basic message metadata subcontext does not contain a message issue instant");
        }

        final long clockskew = relyingPartyCtx.getProfileConfig().getSecurityConfiguration().getClockSkew();
        final long issueInstant = messageSubcontext.getMessageIssueInstant();
        final long currentTime = System.currentTimeMillis();

        if (issueInstant < currentTime - clockskew) {
            throw new PastMessageException(messageSubcontext.getMessageId(), issueInstant);
        }

        if (issueInstant > currentTime + messageLifetime + clockskew) {
            throw new FutureMessageException(messageSubcontext.getMessageId(), issueInstant);
        }

        return ActionSupport.buildProceedEvent(this);
    }

    /**
     * A profile processing exception that occurs when the inbound message was issued from a point in time to far in the
     * future.
     */
    public class FutureMessageException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = -6474772810189615621L;

        /**
         * Constructor.
         * 
         * @param messageId the ID of the message, never null
         * @param instant the issue instant of the message in milliseconds since the epoch
         */
        public FutureMessageException(String messageId, long instant) {
            super("Action " + getId() + ": Message " + messageId + " was issued on " + new DateTime(instant).toString()
                    + " and is not yet valid.");
        }
    }

    /**
     * A profile processing exception that occurs when the inbound message was issued from a point in time to far in the
     * past.
     */
    public class PastMessageException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = 18935109782906635L;

        /**
         * Constructor.
         * 
         * @param messageId the ID of the message, never null
         * @param instant the issue instant of the message in milliseconds since the epoch
         */
        public PastMessageException(String messageId, long instant) {
            super("Action " + getId() + ": Message " + messageId + " was issued on " + new DateTime(instant).toString()
                    + " is now considered expired.");
        }
    }
}