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

package net.shibboleth.idp.consent.flow.ar.impl;

import java.util.Map;

import javax.annotation.Nonnull;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Consent action which validates extracted user input when per-attribute consent is not enabled.
 * 
 * If {@link Consent#isApproved()} is false, log a warning and set {@link Consent#isApproved()} to true.
 * 
 * When per-attribute consent is not enabled, every extracted consent should be true.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link org.opensaml.profile.action.EventIds#INVALID_PROFILE_CTX}
 * @post See above.
 */
public class ValidatedExtractedConsent extends AbstractAttributeReleaseAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidatedExtractedConsent.class);

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        final AttributeReleaseFlowDescriptor flow = getAttributeReleaseFlowDescriptor();
        if (flow == null || !flow.isPerAttributeConsentEnabled()) {
            final ConsentContext consentContext = getConsentContext();
            assert consentContext != null;
            final Map<String, Consent> currentConsents = consentContext.getCurrentConsents();
            for (final Consent consent : currentConsents.values()) {
                if (!consent.isApproved()) {
                    log.warn("{} Consent should have been approved for '{}'", getLogPrefix(), consent);
                    consent.setApproved(Boolean.TRUE);
                }
            }
            log.debug("{} Consent context '{}'", getLogPrefix(), consentContext);
        }
    }

}