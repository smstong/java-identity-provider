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
package net.shibboleth.idp.ui.impl;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/** Class to help Attribute Extraction in views. */
public final class AttributeHelper extends AbstractIdentifiableInitializableComponent {

    /** How to get the AttributeContext we are looking at. */
    @SuppressWarnings("null")
	@Nonnull private Function<ProfileRequestContext,AttributeContext> attributeContextStrategy =
            new ChildContextLookup<>(AttributeContext.class).compose(new ChildContextLookup<>(RelyingPartyContext.class));

    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AttributeHelper.class);

    /** Set the way to get the {@link AttributeContext}.
     * @param strategy the strategy to use
     */
    public void setAttributeContextStrategy(@Nonnull
            Function<ProfileRequestContext, AttributeContext> strategy) {
        attributeContextStrategy = Constraint.isNotNull(strategy, "Injected strategy must not be null");
    }

    /** Return the first (filtered) attribute Value from the attribute of that name.
     * @param prc the ProfileRequestContext
     * @param attributeName the attribute name to look up
     * @param defaultValue what to return if nothing found.
     * @return the default value or the attribute value
     */
    @Nonnull public String getFirstAttributeValue(final ProfileRequestContext prc,
    		final @Nonnull @NotEmpty String attributeName,
            final @Nonnull  String defaultValue) {
        if (prc == null) {
            log.error("Provided ProfileRequestContext was null, returning {}", defaultValue);
            return defaultValue;
        }
        Constraint.isNotNull(defaultValue, "Default value must be non-null");
        Constraint.isNotNull(attributeName, "Attribute Name must be non-niull");
        final AttributeContext context = attributeContextStrategy.apply(prc);
        if (context == null) {
            log.error("Attribute Context could not be located, returning {}", defaultValue);
            return defaultValue;
        }
        return getFirstValue(context.getIdPAttributes().get(attributeName), defaultValue);
    }

    /** Return the first (filtered) attribute Value from the attribute of that name.
     * @param prc the ProfileRequestContext
     * @param attributeName the attribute name to look up
     * @return the attribute value or ""
     */
    @Nonnull public String getFirstAttributeValue(final ProfileRequestContext prc,
    		final @Nonnull @NotEmpty String attributeName) {
        return getFirstAttributeValue(prc, attributeName, "");
    }

    /** Return the first (unfiltered) attribute Value from the attribute of that name.
     * @param prc the ProfileRequestContext
     * @param attributeName the attribute name to look up
     * @param defaultValue what to return if nothing found.
     * @return the default value or the attribute value
     */
    @Nonnull public String getFirstUnfilteredAttributeValue(final ProfileRequestContext prc,
			 final @Nonnull @NotEmpty String attributeName,
             final @Nonnull String defaultValue) {

        if (prc == null) {
            log.error("Provided ProfileRequestContext was null, returning {}", defaultValue);
            return defaultValue;
        }
        Constraint.isNotNull(defaultValue, "Default value must be non-null");
        Constraint.isNotNull(attributeName, "Attribute Name must be non-niull");
        final AttributeContext context = attributeContextStrategy.apply(prc);
        if (context == null) {
            log.error("Attribute Context could not be located, returning {}", defaultValue);
            return defaultValue;
        }
        return getFirstValue(context.getUnfilteredIdPAttributes().get(attributeName), defaultValue);
    }

    /** Return the first (unfiltered) attribute Value from the attribute of that name.
     * @param prc the ProfileRequestContext
     * @param attributeName the attribute name to look up
     * @param defaultValue what to return if nothing found.
     * @return the attribute value or ""
     */
    @Nonnull public String getFirstUnfilteredAttributeValue(final ProfileRequestContext prc,
            @Nonnull @NotEmpty final String attributeName) {
        return getFirstUnfilteredAttributeValue(prc, attributeName, "");
    }

    /** Helper method to get the first attribute name from the attribute.
     * @param Attribute the Attribute or null if there wasn't one
     * @param defaultValue the value to return if no values are found
     * @return defaultValue or the display string of the first attribute
     */
    @Nonnull private String getFirstValue(final @Nullable IdPAttribute attribute, @Nonnull String defaultValue) {
        if (attribute == null) {
            log.info("No attribute found, returning {}", defaultValue);
            return defaultValue;
        }
        List<IdPAttributeValue> values = attribute.getValues();
        if (values == null|| values.size() < 1) {
            log.info("No attribute values associated with {}, returning {}", attribute.getId(), defaultValue);
            return defaultValue;
        }
        return values.get(0).getDisplayValue();
    }
}
