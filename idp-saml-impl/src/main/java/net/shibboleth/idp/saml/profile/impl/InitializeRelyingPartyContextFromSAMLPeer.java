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

import java.util.function.Function;

import javax.annotation.Nonnull;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.slf4j.Logger;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Action that adds a {@link RelyingPartyContext} to the current {@link ProfileRequestContext} tree
 * via a creation function. The context is populated via a lookup strategy to locate a {@link SAMLPeerEntityContext},
 * by default via {@link ProfileRequestContext#getInboundMessageContext()}.
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CTX}
 * @post ProfileRequestContext.getSubcontext(RelyingPartyContext.class) != null and populated as above.
 */
public class InitializeRelyingPartyContextFromSAMLPeer extends AbstractProfileAction {

    /** The relying party ID lookup function to inject. */
    @Nonnull private static final Function<RelyingPartyContext,String> RPID_LOOKUP
        = new SAMLRelyingPartyIdLookupStrategy();

    /** The verification lookup function to inject. */
    @Nonnull private static final Function<RelyingPartyContext,Boolean> VERIFY_LOOKUP
        = new SAMLVerificationLookupStrategy();
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InitializeRelyingPartyContextFromSAMLPeer.class);

    /** Strategy that will return or create a {@link RelyingPartyContext}. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextCreationStrategy;
    
    /** Strategy used to look up the {@link SAMLPeerEntityContext} to draw from. */
    @Nonnull private Function<ProfileRequestContext,SAMLPeerEntityContext> peerEntityContextLookupStrategy;

    /** SAML peer entity context to populate from. */
    @NonnullBeforeExec private SAMLPeerEntityContext peerEntityCtx;
    
    /** Constructor. */
    public InitializeRelyingPartyContextFromSAMLPeer() {
        relyingPartyContextCreationStrategy = new ChildContextLookup<>(RelyingPartyContext.class, true);
        final Function<ProfileRequestContext,SAMLPeerEntityContext> pecs =  
                new ChildContextLookup<>(SAMLPeerEntityContext.class).compose(new InboundMessageContextLookup());
        assert pecs != null; 
        peerEntityContextLookupStrategy = pecs;
    }

    /**
     * Set the strategy used to return or create the {@link RelyingPartyContext}.
     * 
     * @param strategy creation strategy
     */
    public void setRelyingPartyContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        checkSetterPreconditions();
        relyingPartyContextCreationStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext creation strategy cannot be null");
    }
    
    /**
     * Set the strategy used to look up the {@link SAMLPeerEntityContext} to draw from.
     * 
     * @param strategy strategy used to look up the {@link SAMLPeerEntityContext}
     */
    public void setPeerEntityContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SAMLPeerEntityContext> strategy) {
        checkSetterPreconditions();
        peerEntityContextLookupStrategy =
                Constraint.isNotNull(strategy, "SAMLPeerEntityContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        peerEntityCtx = peerEntityContextLookupStrategy.apply(profileRequestContext);
        if (peerEntityCtx == null) {
            log.debug("{} Unable to locate SAMLPeerEntityContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        return super.doPreExecute(profileRequestContext);
    }
        
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final RelyingPartyContext rpContext = relyingPartyContextCreationStrategy.apply(profileRequestContext);
        if (rpContext == null) {
            log.debug("{} Unable to locate or create RelyingPartyContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return;
        }

        log.debug("{} Attaching RelyingPartyContext based on SAML peer {}", getLogPrefix(),
                peerEntityCtx.getEntityId());
        rpContext.setRelyingPartyIdContextTree(peerEntityCtx);
        rpContext.setRelyingPartyIdLookupStrategy(RPID_LOOKUP);
        rpContext.setVerificationLookupStrategy(VERIFY_LOOKUP);
    }
    
}