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

package net.shibboleth.idp.saml.saml2.profile.config.impl;

import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.saml.profile.config.BasicSAMLArtifactConfiguration;
import net.shibboleth.saml.profile.config.SAMLArtifactConfiguration;
import net.shibboleth.shared.logic.FunctionSupport;

/** Unit test for {@link SingleLogoutProfileConfiguration}. */
@SuppressWarnings("javadoc")
public class SingleLogoutProfileConfigurationTest {

    @Test
    public void testProfileId() {
        final SingleLogoutProfileConfiguration config = new SingleLogoutProfileConfiguration();
        Assert.assertEquals(config.getId(), SingleLogoutProfileConfiguration.PROFILE_ID);
    }

    @Test
    public void testArtifactConfiguration() {
        final SingleLogoutProfileConfiguration config = new SingleLogoutProfileConfiguration();
        Assert.assertNull(config.getArtifactConfiguration(null));

        final SAMLArtifactConfiguration artifactConfiguration = new BasicSAMLArtifactConfiguration();
        config.setArtifactConfiguration(artifactConfiguration);

        Assert.assertSame(config.getArtifactConfiguration(null), artifactConfiguration);
    }

    @Test
    public void testIndirectArtifactConfiguration() {
        final SingleLogoutProfileConfiguration config = new SingleLogoutProfileConfiguration();

        final SAMLArtifactConfiguration artifactConfiguration = new BasicSAMLArtifactConfiguration();
        config.setArtifactConfigurationLookupStrategy(FunctionSupport.constant(artifactConfiguration));

        Assert.assertSame(config.getArtifactConfiguration(null), artifactConfiguration);
    }
    
    @Test
    public void testSignArtifactRequests() {
        final SingleLogoutProfileConfiguration config = new SingleLogoutProfileConfiguration();
        
        config.setSignArtifactRequests(true);
        Assert.assertTrue(config.isSignArtifactRequests(null));
    }
     
    @Test
    public void testClientTLSArtifactRequests() {
        final SingleLogoutProfileConfiguration config = new SingleLogoutProfileConfiguration();
        
        config.setClientTLSArtifactRequests(true);
        Assert.assertTrue(config.isClientTLSArtifactRequests(null));
    }

}