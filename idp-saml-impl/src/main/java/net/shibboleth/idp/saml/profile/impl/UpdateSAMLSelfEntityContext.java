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

import java.util.Objects;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLSelfEntityContext;
import org.slf4j.Logger;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.profile.context.navigate.IssuerLookupFunction;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Action that updates inbound and/or outbound instances of {@link SAMLSelfEntityContext}
 * based on the identity of a relying party accessed via a lookup strategy,
 * by default an immediate child of the profile request context.
 *
 * <p>This action handles mid-request updates to the IdP's own entityID in advanced
 * scenarios such as interceptors that cause the value to change, and updates one
 * of the persistent records of the value.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 */
public class UpdateSAMLSelfEntityContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(UpdateSAMLSelfEntityContext.class);

    /** Strategy used to obtain the self identity value. */
    @Nonnull private Function<ProfileRequestContext,String> selfIdentityLookupStrategy;

    /** Result of strategy function. */
    @Nullable private String selfIdentity;
    
    /** Constructor. */
    public UpdateSAMLSelfEntityContext() {
        selfIdentityLookupStrategy = new IssuerLookupFunction();
    }

    /**
     * Set the strategy used to locate the self identity value to use.
     * 
     * @param strategy lookup strategy
     */
    public void setSelfIdentityLookupStrategy(@Nonnull final Function<ProfileRequestContext, String> strategy) {
        checkSetterPreconditions();
        selfIdentityLookupStrategy = Constraint.isNotNull(strategy, "Self identity lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }

        selfIdentity = selfIdentityLookupStrategy.apply(profileRequestContext);
        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final MessageContext imc = profileRequestContext.getInboundMessageContext();
        if (imc != null) {
            final SAMLSelfEntityContext context =
                    imc.getSubcontext(SAMLSelfEntityContext.class);
            if (context != null && !Objects.equals(context.getEntityId(), selfIdentity)) {
                log.debug("{} Updating inbound SAMLSelfEntityContext, '{}' to '{}'", getLogPrefix(),
                        context.getEntityId(), selfIdentity);
                context.setEntityId(selfIdentity);
            }
        }
        final MessageContext omc = profileRequestContext.getOutboundMessageContext(); 
        if (omc != null) {
            final SAMLSelfEntityContext context = omc.getSubcontext(SAMLSelfEntityContext.class);
            if (context != null && !Objects.equals(context.getEntityId(), selfIdentity)) {
                log.debug("{} Updating outbound SAMLSelfEntityContext, '{}' to '{}'", getLogPrefix(),
                        context.getEntityId(), selfIdentity);
                context.setEntityId(selfIdentity);
            }
        }
    }
    
}