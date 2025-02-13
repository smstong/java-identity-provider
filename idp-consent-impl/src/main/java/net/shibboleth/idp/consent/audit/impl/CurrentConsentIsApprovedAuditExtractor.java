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

package net.shibboleth.idp.consent.audit.impl;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

/**
 * {@link Function} that returns whether the current consents are approved from an {@link ConsentContext}.
 */
public class CurrentConsentIsApprovedAuditExtractor implements Function<ProfileRequestContext, Collection<Boolean>> {

    /** Strategy used to find the {@link ConsentContext} from the {@link ProfileRequestContext}. */
    @Nonnull private Function<ProfileRequestContext,ConsentContext> consentContextLookupStrategy;

    /** Constructor. */
    public CurrentConsentIsApprovedAuditExtractor() {
        consentContextLookupStrategy = new ChildContextLookup<>(ConsentContext.class);
    }

    /**
     * 
     * Constructor.
     *
     * @param strategy the consent context lookup strategy
     */
    public CurrentConsentIsApprovedAuditExtractor(
            @Nonnull final Function<ProfileRequestContext,ConsentContext> strategy) {
        consentContextLookupStrategy = Constraint.isNotNull(strategy, "Consent context lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable public Collection<Boolean> apply(@Nullable final ProfileRequestContext input) {
        
        final ConsentContext consentContext = consentContextLookupStrategy.apply(input);
        if (consentContext != null && !consentContext.getCurrentConsents().isEmpty()) {
            return consentContext.
                    getCurrentConsents().
                    values().
                    stream().
                    map(Consent::isApproved).
                    collect(Collectors.toList());
        }
        return CollectionSupport.emptyList();
    }

}