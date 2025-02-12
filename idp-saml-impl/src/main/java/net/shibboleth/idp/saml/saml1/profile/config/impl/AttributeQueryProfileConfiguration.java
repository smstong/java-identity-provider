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

package net.shibboleth.idp.saml.saml1.profile.config.impl;

import javax.annotation.Nonnull;

import org.opensaml.profile.logic.NoIntegrityMessageChannelPredicate;

import net.shibboleth.saml.profile.config.SAMLAssertionProducingProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

/** Configuration support for SAML 1 attribute query requests. */
public class AttributeQueryProfileConfiguration extends AbstractSAML1AssertionProducingProfileConfiguration
        implements net.shibboleth.saml.saml1.profile.config.AttributeQueryProfileConfiguration,
            SAMLAssertionProducingProfileConfiguration {
    
    /** Name of profile counter. */
    @Nonnull @NotEmpty public static final String PROFILE_COUNTER = "net.shibboleth.idp.profiles.saml1.query.attribute";
    
    /** Constructor. */
    public AttributeQueryProfileConfiguration() {
        this(PROFILE_ID);
    }

    /**
     * Constructor.
     * 
     * @param profileId unique ID for this profile
     */
    protected AttributeQueryProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);
        setSignResponsesPredicate(new NoIntegrityMessageChannelPredicate());
    }

}