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

package net.shibboleth.idp.authn.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractSubjectCanonicalizationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.SubjectCanonicalizationFlowDescriptor;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;


/**
 * A canonicalization action that selects a canonicalization flow to invoke.
 * 
 * <p>This is the heart of the c14n processing sequence, and runs after the
 * {@link SubjectCanonicalizationContext} has been fully populated. It uses the potential flows,
 * and their associated activation conditions to decide how to proceed.</p>
 * 
 * <p>This is a rare case in that the standard default event,
 * {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}, cannot be returned,
 * because the action must either dispatch to a flow by name, or signal an error.</p>
 * 
 * @event {@link AuthnEventIds#NO_POTENTIAL_FLOW}
 * @event Selected flow ID to execute
 * @pre <pre>ProfileRequestContext.getSubcontext(SubjectCanonicalizationContext.class, false) != null</pre>
 */
public class SelectSubjectCanonicalizationFlow extends AbstractSubjectCanonicalizationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SelectSubjectCanonicalizationFlow.class);

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final SubjectCanonicalizationContext c14nContext) {
        
        // Detect a previous attempted flow, and move it to the intermediate collection.
        // This will prevent re-selecting the same (probably failed) flow again.
        final SubjectCanonicalizationFlowDescriptor flow = c14nContext.getAttemptedFlow(); 
        if (flow != null) {
            log.info("{} Moving incomplete flow {} to intermediate set, reselecting a different one", getLogPrefix(),
                    flow.getId());
            c14nContext.getIntermediateFlows().put(
                    flow.ensureId(), c14nContext.getAttemptedFlow());
        }
        
        return super.doPreExecute(profileRequestContext, c14nContext);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final SubjectCanonicalizationContext c14nContext) {
        
        final SubjectCanonicalizationFlowDescriptor flow = selectUnattemptedFlow(profileRequestContext, c14nContext);
        if (flow == null) {
            log.error("{} No potential flows left to choose from, canonicalization will fail", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_POTENTIAL_FLOW);
            return;
        }
        log.debug("{} Selecting canonicalization flow {}", getLogPrefix(), flow.ensureId());
        ActionSupport.buildEvent(profileRequestContext, flow.ensureId());
    }

    /**
     * Select the first potential flow not found in the intermediate flows collection,
     * and that is applicable to the context.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param c14nContext the current c14n context
     * @return an eligible flow, or null
     */
    @Nullable private SubjectCanonicalizationFlowDescriptor selectUnattemptedFlow(
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final SubjectCanonicalizationContext c14nContext) {
        for (final SubjectCanonicalizationFlowDescriptor flow : c14nContext.getPotentialFlows().values()) {
            if (!c14nContext.getIntermediateFlows().containsKey(flow.ensureId())) {
                log.debug("{} Checking canonicalization flow {} for applicability...", getLogPrefix(),
                        flow.getId());
                c14nContext.setAttemptedFlow(flow);
                if (flow.test(profileRequestContext)) {
                    return flow;
                }
                final Exception ctxException = c14nContext.getException();
                log.debug("{} Canonicalization flow {} was not applicable: {}", getLogPrefix(), flow.getId(),
                        ctxException!= null ? ctxException.getMessage()
                                : "reason unknown");
                c14nContext.setException(null);
                
                // Note that we don't exclude this flow from possible future selection, since one flow
                // could in theory do partial work and change the context such that this flow then applies.
            }
        }
        
        return null;
    }
        
}
