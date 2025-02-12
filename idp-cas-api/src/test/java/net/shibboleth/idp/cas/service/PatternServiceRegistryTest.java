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

package net.shibboleth.idp.cas.service;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class PatternServiceRegistryTest {

    @DataProvider(name = "services")
    public Object[][] getServices() {
        final ServiceDefinition d1 = new ServiceDefinition("https://([A-Za-z0-9_-]+\\.)*example\\.org(:\\d+)?/.*");
        d1.setGroup("example.org-plus-subdomains");
        d1.setAuthorizedToProxy(false);
        final ServiceDefinition d2 = new ServiceDefinition("https://trusted\\.example\\.org/.*");
        d2.setGroup("trusted-service");
        d2.setAuthorizedToProxy(true);
        return new Object[][] {
                {
                        Arrays.asList(d1, d2),
                        "https://trusted.example.org/landing",
                        new Service("https://trusted.example.org/landing", "example.org-plus-subdomains", false),
                },
                {
                        Arrays.asList(d2, d1),
                        "https://trusted.example.org/landing",
                        new Service("https://trusted.example.org/landing", "trusted-service", true),
                },
                {
                        Arrays.asList(d1, d2),
                        "https://service.untrusted.org/landing",
                        null,
                },
        };
    };

    @Test(dataProvider = "services")
    public void testLookup(
            final @Nonnull List<ServiceDefinition> services, @Nonnull final String serviceURL, final Service expected)
            throws Exception {
        final PatternServiceRegistry registry = new PatternServiceRegistry();
        registry.setDefinitions(services);
        final Service actual = registry.lookup(serviceURL);
        if (expected == null) {
            assert actual == null;
        } else {
            assert actual != null;
            assertEquals(actual.getName(), expected.getName());
            assertEquals(actual.getGroup(), expected.getGroup());
            assertEquals(actual.isAuthorizedToProxy(), expected.isAuthorizedToProxy());
        }
    }
}