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

package net.shibboleth.idp.spring;

import javax.annotation.Nonnull;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.spring.context.AbstractPropertiesApplicationContextInitializer;

/**
 * Specialization of context initializer for IdP use.
 */
public class IdPPropertiesApplicationContextInitializer
        extends AbstractPropertiesApplicationContextInitializer {

    /** IdP home property. */
    @Nonnull @NotEmpty public static final String IDP_HOME_PROPERTY = "idp.home";

    /** Property that points to more property sources. */
    @Nonnull @NotEmpty public static final String IDP_ADDITIONAL_PROPERTY = "idp.additionalProperties";

    /** Property that controls auto-search for property sources. */
    @Nonnull @NotEmpty public static final String IDP_AUTOSEARCH_PROPERTY = "idp.searchForProperties";

    /** Target resource to be searched for. */
    @Nonnull public static final String IDP_PROPERTIES = "/conf/idp.properties";

    /** Well known search location. */
    @Nonnull public static final String SEARCH_LOCATION = "/opt/shibboleth-idp";

    /** Property controlling whether to fail fast. */
    @Nonnull public static final String FAILFAST_PROPERTY = "idp.initializer.failFast";

    /** Property for tracking duplicates. */
    @Nonnull public static final String IDP_DUPLICATE_PROPERTY = "idp.duplicateProperties";
    
    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty protected String getHomePropertyName() {
        return IDP_HOME_PROPERTY;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty protected String getSearchTarget() {
        return IDP_PROPERTIES;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty protected String getSearchLocation() {
        return SEARCH_LOCATION;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty protected String getFailFastPropertyName() {
        return FAILFAST_PROPERTY;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty protected String getAdditionalPropertiesPropertyName() {
        return IDP_ADDITIONAL_PROPERTY;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty protected String getAutoSearchPropertyName() {
        return IDP_AUTOSEARCH_PROPERTY;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull
    protected String getDuplicateWarningPropertyName() {
        return IDP_DUPLICATE_PROPERTY;
    }

}