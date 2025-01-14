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

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.consent.context.AttributeReleaseContext;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Attribute consent action to populate the attribute consent context with the attributes for which consent should be
 * obtained. A predicate is used to determine whether consent should be obtained for each IdP attribute in the attribute
 * context.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @post See above.
 */
public class PopulateAttributeReleaseContext extends AbstractAttributeReleaseAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateAttributeReleaseContext.class);

    /** Predicate to determine whether consent should be obtained for an attribute. */
    @NonnullAfterInit private Predicate<IdPAttribute> attributePredicate;
    
    /** Comparator used to sort attributes displayed to user. */
    @Nullable private Comparator<String> attributeIdComparator;

    /**
     * Set the predicate to determine whether consent should be obtained for an attribute.
     * 
     * @param predicate predicate to determine whether consent should be obtained for an attribute
     */
    public void setAttributePredicate(@Nonnull final Predicate<IdPAttribute> predicate) {
        attributePredicate = Constraint.isNotNull(predicate, "Attribute predicate cannot be null");
    }

    /**
     * Set the comparator used to sort attributes displayed to user.
     * 
     * @param comparator comparator used to sort attributes displayed to user
     */
    public void setAttributeIdComparator(@Nullable final Comparator<String> comparator) {
        attributeIdComparator = comparator;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (attributePredicate == null) {
            throw new ComponentInitializationException("Attribute predicate cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        final AttributeContext attributeContext = getAttributeContext();
        assert attributeContext != null;
        final Map<String, IdPAttribute> attributes = attributeContext.getIdPAttributes();

        final Map<String, IdPAttribute> consentableAttributes = new TreeMap<>(attributeIdComparator);
        for (final IdPAttribute attribute : attributes.values()) {
            if (attributePredicate.test(attribute)) {
                consentableAttributes.put(attribute.getId(), attribute);
            }
        }

        final AttributeReleaseContext releaseContext = getAttributeReleaseContext();
        assert releaseContext != null;
        releaseContext.getConsentableAttributes().putAll(consentableAttributes);

        log.debug("{} Consentable attribute IDs '{}'", getLogPrefix(), consentableAttributes.keySet());

        log.trace("{} Consentable attributes '{}'", getLogPrefix(), consentableAttributes);
    }

}