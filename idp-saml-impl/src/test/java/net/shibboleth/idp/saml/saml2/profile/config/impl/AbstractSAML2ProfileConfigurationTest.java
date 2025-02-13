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

import net.shibboleth.shared.logic.PredicateSupport;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link AbstractSAML2ProfileConfiguration}. */
@SuppressWarnings("javadoc")
public class AbstractSAML2ProfileConfigurationTest {

    @Test
    public void testEncryptionOptional(){
        final MockSAML2ProfileConfiguration config = new MockSAML2ProfileConfiguration();
        Assert.assertFalse(config.isEncryptionOptional(null));

        config.setEncryptionOptional(true);
        Assert.assertTrue(config.isEncryptionOptional(null));
    }

    @Test
    public void testIndirectEncryptionOptional(){
        final MockSAML2ProfileConfiguration config = new MockSAML2ProfileConfiguration();

        config.setEncryptionOptionalPredicate(PredicateSupport.alwaysTrue());
        Assert.assertTrue(config.isEncryptionOptional(null));
    }

    @Test public void testEncryptNameIDsPredicate() {
        final MockSAML2ProfileConfiguration config = new MockSAML2ProfileConfiguration();

        config.setEncryptNameIDs(true);
        Assert.assertTrue(config.isEncryptNameIDs(null));
    }

    /** Mock class for testing {@link AbstractSAML2ProfileConfiguration}. */
    private static class MockSAML2ProfileConfiguration extends AbstractSAML2ProfileConfiguration {

        /** Constructor. */
        public MockSAML2ProfileConfiguration() {
            super("mock");
        }
    }
}