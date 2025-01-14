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

import java.util.Collection;
import java.util.function.Function;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.consent.flow.impl.ConsentFlowDescriptor;
import net.shibboleth.idp.consent.logic.impl.AttributeValuesHashFunction;
import net.shibboleth.shared.logic.Constraint;

/**
 * Descriptor for an attribute release flow.
 */
public class AttributeReleaseFlowDescriptor extends ConsentFlowDescriptor {

    /** Whether not remembering consent is allowed. */
    private boolean doNotRememberConsentAllowed;

    /** Whether consent to any attribute and to any relying party is allowed. */
    private boolean globalConsentAllowed;

    /** Whether per-attribute consent is enabled. */
    private boolean perAttributeConsentEnabled;

    /** Function to create hash of all attribute values. */
    @Nonnull private Function<Collection<IdPAttributeValue>, String> attributeValuesHashFunction;

    /** Constructor. */
    public AttributeReleaseFlowDescriptor() {
        attributeValuesHashFunction = new AttributeValuesHashFunction();
    }

    /**
     * Get whether not remembering consent is allowed.
     * 
     * @return true if consent should not be remembered
     */
    public boolean isDoNotRememberConsentAllowed() {
        return doNotRememberConsentAllowed;
    }

    /**
     * Get whether consent to any attribute and to any relying party is allowed.
     * 
     * @return true iff consent to any attribute and to any relying party is allowed
     */
    public boolean isGlobalConsentAllowed() {
        return globalConsentAllowed;
    }

    /**
     * Get whether per-attribute consent is enabled.
     * 
     * @return true iff per-attribute consent is enabled
     */
    public boolean isPerAttributeConsentEnabled() {
        return perAttributeConsentEnabled;
    }

    /**
     * Get the function to create hash of all attribute values.
     * 
     * @return function to create hash of all attribute values
     */
    @Nonnull public Function<Collection<IdPAttributeValue>, String> getAttributeValuesHashFunction() {
        return attributeValuesHashFunction;
    }

    /**
     * Set whether not remembering consent is allowed.
     * 
     * @param flag true if consent should not be remembered
     */
    public void setDoNotRememberConsentAllowed(final boolean flag) {
        checkSetterPreconditions();
        doNotRememberConsentAllowed = flag;
    }

    /**
     * Set whether consent to any attribute and to any relying party is allowed.
     * 
     * @param flag true iff consent to any attribute and to any relying party is allowed
     */
    public void setGlobalConsentAllowed(final boolean flag) {
        checkSetterPreconditions();
        globalConsentAllowed = flag;
    }

    /**
     * Set whether per-attribute consent is enabled.
     * 
     * @param flag true iff per-attribute consent is enabled
     */
    public void setPerAttributeConsentEnabled(final boolean flag) {
        checkSetterPreconditions();
        perAttributeConsentEnabled = flag;
    }

    /**
     * Set the function to create hash of all attribute values.
     * 
     * @param function function to create hash of all attribute values
     */
    public void setAttributeValuesHashFunction(
            @Nonnull final Function<Collection<IdPAttributeValue>, String> function) {
        checkSetterPreconditions();
                attributeValuesHashFunction = Constraint.isNotNull(function, 
                        "Attribute values hash function cannot be null");
    }

}
