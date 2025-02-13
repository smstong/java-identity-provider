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

package net.shibboleth.idp.saml.saml2.profile.impl;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.relyingparty.BasicRelyingPartyConfiguration;
import net.shibboleth.profile.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.saml.saml2.profile.config.impl.ArtifactResolutionProfileConfiguration;
import net.shibboleth.idp.saml.saml2.profile.config.impl.AttributeQueryProfileConfiguration;
import net.shibboleth.idp.saml.saml2.profile.config.impl.BrowserSSOProfileConfiguration;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.primitive.StringSupport;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.security.config.SecurityConfiguration;
import org.opensaml.xmlsec.config.BasicXMLSecurityConfiguration;

/**
 * Helper methods for creating/testing SAML 2 objects within profile action tests. When methods herein refer to mock
 * objects they are always objects that have been created via Mockito unless otherwise noted.
 */
public final class SAML2ActionTestingSupport extends org.opensaml.saml.saml2.testing.SAML2ActionTestingSupport {

    /**
     * Builds a {@link RelyingPartyContext} that is a child of the given parent context. The build subcontext contains:
     * <ul>
     * <li>a {@link RelyingPartyConfiguration} whose ID is the given relying party ID or
     * {@link ActionTestingSupport#INBOUND_MSG_ISSUER} if none is given</li>
     * <li>the set of {@link ProfileConfiguration} created by {@link #buildProfileConfigurations()}</li>
     * <li>the {@link BrowserSSOProfileConfiguration} set as the active profile configuration</li>
     * </ul>
     * 
     * @param parent the parent of the created subcontext
     * @param relyingPartyId the ID of the relying party
     * 
     * @return the constructed subcontext
     * 
     * @throws ComponentInitializationException ...
     */
    public static RelyingPartyContext buildRelyingPartySubcontext(@Nonnull final BaseContext parent,
            @Nullable final String relyingPartyId) throws ComponentInitializationException {

        String id = StringSupport.trimOrNull(relyingPartyId);
        if (id == null) {
            id = ActionTestingSupport.INBOUND_MSG_ISSUER;
        }

        final BasicRelyingPartyConfiguration rpConfig = new BasicRelyingPartyConfiguration();
        rpConfig.setId(id);
        rpConfig.setIssuer(ActionTestingSupport.OUTBOUND_MSG_ISSUER);
        rpConfig.setDetailedErrors(true);
        rpConfig.setProfileConfigurations(buildProfileConfigurations());
        rpConfig.initialize();

        final RelyingPartyContext subcontext = parent.ensureSubcontext(RelyingPartyContext.class);
        subcontext.setRelyingPartyId(id);
        subcontext.setProfileConfig(rpConfig.getProfileConfiguration(null, BrowserSSOProfileConfiguration.PROFILE_ID));
        subcontext.setConfiguration(rpConfig);

        return subcontext;
    }

    /**
     * Builds a {@link ProfileConfiguration} collection containing a {@link ArtifactResolutionProfileConfiguration},
     * {@link AttributeQueryProfileConfiguration}, and {@link ArtifactResolutionProfileConfiguration}.
     * 
     * @return the constructed {@link ProfileConfiguration}
     */
    public static Collection<ProfileConfiguration> buildProfileConfigurations() {
        final ArrayList<ProfileConfiguration> profileConfigs = new ArrayList<>();

        final SecurityConfiguration securityConfig = new BasicXMLSecurityConfiguration();

        final ArtifactResolutionProfileConfiguration artifactConfig = new ArtifactResolutionProfileConfiguration();
        artifactConfig.setSecurityConfiguration(securityConfig);
        profileConfigs.add(artifactConfig);

        final AttributeQueryProfileConfiguration attributeConfig = new AttributeQueryProfileConfiguration();
        attributeConfig.setSecurityConfiguration(securityConfig);
        profileConfigs.add(attributeConfig);

        final BrowserSSOProfileConfiguration ssoConfig = new BrowserSSOProfileConfiguration();
        ssoConfig.setSecurityConfiguration(securityConfig);
        profileConfigs.add(ssoConfig);

        return profileConfigs;
    }

}