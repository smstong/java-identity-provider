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

package net.shibboleth.idp.authn.impl;

import java.io.File;
import java.net.URISyntaxException;

import javax.annotation.Nonnull;

import net.shibboleth.shared.resource.Resource;
import net.shibboleth.shared.spring.resource.ResourceHelper;

import org.ldaptive.ssl.SSLContextInitializer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test for {@link X509ResourceCredentialConfig}.
 */
@SuppressWarnings("javadoc")
public class X509ResourceCredentialConfigTest {

    private static final String DATAPATH = "/net/shibboleth/idp/authn/impl/";

    @DataProvider(name = "resources")
    public Object[][] getResources() throws Exception {
        return new Object[][] {
          new Object[] {
              getFileSystemResource(DATAPATH + "trust-certs.pem"),
              getFileSystemResource(DATAPATH + "auth-cert.pem"),
              getFileSystemResource(DATAPATH + "private-key.pem"),
          },
          new Object[] {
                  ResourceHelper.of(new ClassPathResource(DATAPATH + "trust-certs.pem")),
              ResourceHelper.of(new ClassPathResource(DATAPATH + "auth-cert.pem")),
              ResourceHelper.of(new ClassPathResource(DATAPATH + "private-key.pem")),
          },
        };
    }

    @Test(dataProvider = "resources") public void createSSLContextInitializer(@Nonnull final Resource trustCertificates,
            @Nonnull final Resource authenticationCertificate, @Nonnull final Resource authenticationKey) throws Exception {
        final X509ResourceCredentialConfig config = new X509ResourceCredentialConfig();
        config.setTrustCertificates(trustCertificates);
        config.setAuthenticationCertificate(authenticationCertificate);
        config.setAuthenticationKey(authenticationKey);
        config.setAuthenticationKeyPassword("changeit");

        final SSLContextInitializer init = config.createSSLContextInitializer();
        Assert.assertNotNull(init.getTrustManagers()[0]);
        Assert.assertNotNull(init.getKeyManagers()[0]);
    }

    private static Resource getFileSystemResource(final String path) throws URISyntaxException {
        return ResourceHelper.of(new FileSystemResource(new File(X509ResourceCredentialConfigTest.class.getResource(
                path).toURI())));
    }
}
