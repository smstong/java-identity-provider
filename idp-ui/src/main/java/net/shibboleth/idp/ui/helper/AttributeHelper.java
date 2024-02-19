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
package net.shibboleth.idp.ui.helper;

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
            new ChildContextLookup<>(AttributeContext.class).compose(
                    new ChildContextLookup<>(RelyingPartyContext.class));

    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AttributeHelper.class);

    /** Set the way to get the {@link AttributeContext}.
     * @param strategy the strategy to use
     */
    public void setAttributeContextStrategy(@Nonnull final Function<ProfileRequestContext, AttributeContext> strategy) {
        attributeContextStrategy = Constraint.isNotNull(strategy, "Injected strategy must not be null");
    }

    /** Return the first (filtered) attribute Value from the attribute of that name as an {@link IdPAttributeValue}.
     * @param prc The ProfileRequestContext
     * @param attributeName The attribute name to look up
     * @return The attribute value or null
     */
    @Nullable public IdPAttributeValue getFirstAttributeValue(@Nonnull final ProfileRequestContext prc,
           @Nonnull @NotEmpty final String attributeName) {
        Constraint.isNotNull(attributeName, "Attribute Name must be non-niull");
        final AttributeContext context = attributeContextStrategy.apply(prc);
        if (context == null) {
            log.error("Attribute Context could not be located");
            return null;
        }
        return getFirstValue(context.getIdPAttributes().get(attributeName));
    }

    /** Return the first (filtered) attribute Value from the attribute of that name as a Display String.
     * @param prc The ProfileRequestContext
     * @param attributeName The attribute name to look up
     * @param defaultValue What to return if nothing found.
     * @return The default value or the attribute value
     */
    @Nonnull public String getFirstAttributeDisplayValue(@Nonnull final ProfileRequestContext prc,
                @Nonnull @NotEmpty final String attributeName,
            final @Nonnull  String defaultValue) {
        Constraint.isNotNull(defaultValue, "Default value must be non-null");
        final IdPAttributeValue value = getFirstAttributeValue(prc, attributeName);
        if (value == null) {
                log.error("No Attribute Value found, returning {}", defaultValue);
                return defaultValue;
        }
        return value.getDisplayValue();
    }

    /** Return the first (filtered) attribute Value from the attribute of that name as a Display String.
     * @param prc The ProfileRequestContext
     * @param attributeName The attribute name to look up
     * @return The attribute value or ""
     */
    @Nonnull public String getFirstAttributeDisplayValue(@Nonnull final ProfileRequestContext prc,
                @Nonnull @NotEmpty final String attributeName) {
        return getFirstAttributeDisplayValue(prc, attributeName, "");
    }

    /** Return the first (unfiltered) attribute Value from the attribute of that name as an {@link IdPAttributeValue}.
     * @param prc The ProfileRequestContext
     * @param attributeName The attribute name to look up
     * @return The attribute value or null
     */
    @Nullable public IdPAttributeValue getFirstUnfilteredAttributeValue(@Nonnull final ProfileRequestContext prc,
                         @Nonnull @NotEmpty final String attributeName) {

        Constraint.isNotNull(attributeName, "Attribute Name must be non-niull");
        final AttributeContext context = attributeContextStrategy.apply(prc);
        if (context == null) {
            log.error("Attribute Context could not be located");
            return null;
        }
        return getFirstValue(context.getUnfilteredIdPAttributes().get(attributeName));
    }


    /**
     * Return the first (unfiltered) attribute Value from the attribute of that name as a Display String.
     * 
     * @param prc The ProfileRequestContext
     * @param attributeName The attribute name to look up
     * @param defaultValue What to return if nothing found
     * 
     * @return The default value or the attribute value
     */
    @Nonnull public String getFirstUnfilteredAttributeDisplayValue(@Nonnull final ProfileRequestContext prc,
                         @Nonnull @NotEmpty final String attributeName,
             final @Nonnull String defaultValue) {

        Constraint.isNotNull(defaultValue, "Default value must be non-null");
        final IdPAttributeValue value = getFirstUnfilteredAttributeValue(prc, attributeName);
        if (value == null) {
            log.error("No Attribute Value found, returning {}", defaultValue);
            return defaultValue;
        }
        return value.getDisplayValue();
    }

    /** Return the first (unfiltered) attribute Value from the attribute of that name as a Display String.
     * @param prc The ProfileRequestContext
     * @param attributeName The attribute name to look up
     * @return The attribute value or ""
     */
    @Nonnull public String getFirstUnfilteredAttributeDisplayValue(@Nonnull final ProfileRequestContext prc,
            @Nonnull @NotEmpty final String attributeName) {
        return getFirstUnfilteredAttributeDisplayValue(prc, attributeName, "");
    }

    /**
     * Helper method to get the first attribute name from the attribute.
     * 
     * @param attribute the Attribute or null if there wasn't one
     * 
     * @return the first attribute valkue or null
     */
    @Nullable private IdPAttributeValue getFirstValue(@Nullable final IdPAttribute attribute) {
        if (attribute == null) {
            log.debug("No attribute found");
            return null;
        }
        final List<IdPAttributeValue> values = attribute.getValues();
        if (values == null|| values.size() < 1) {
            log.info("No attribute values associated with {}", attribute.getId());
            return null;
        }
        return values.get(0);
    }
}
