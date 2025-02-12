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

package net.shibboleth.idp.consent.logic.impl;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.context.ConsentContext;

/**
 * Predicate to determine whether global consent has been given by user.
 */
public class GlobalAttributeConsentPredicate implements Predicate<ProfileRequestContext> {

    /** Strategy used to find the {@link ConsentContext} from the {@link ProfileRequestContext}. */
    @Nonnull private Function<ProfileRequestContext, ConsentContext> consentContextlookupStrategy;

    /** Constructor. */
    public GlobalAttributeConsentPredicate() {
        consentContextlookupStrategy = new ChildContextLookup<>(ConsentContext.class);
    }

    /** {@inheritDoc} */
    public boolean test(@Nullable final ProfileRequestContext input) {
        if (input == null) {
            return false;
        }

        final ConsentContext consentContext = consentContextlookupStrategy.apply(input);
        if (consentContext == null) {
            return false;
        }

        final Map<String, Consent> previousConsents = consentContext.getPreviousConsents();
        for (final Consent consent : previousConsents.values()) {
            if (consent.ensureId().equals(Consent.WILDCARD) && consent.isApproved()) {
                return true;
            }
        }

        return false;
    }

}