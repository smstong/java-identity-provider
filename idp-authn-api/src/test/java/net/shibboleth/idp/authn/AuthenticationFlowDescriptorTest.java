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

package net.shibboleth.idp.authn;

import java.time.Duration;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.ConstraintViolationException;

/** {@link AuthenticationFlowDescriptor} unit test. */
public class AuthenticationFlowDescriptorTest {

    private AuthenticationFlowDescriptor descriptor;
    
    @BeforeMethod public void setUp() {
        descriptor = new AuthenticationFlowDescriptor();
        descriptor.setId("test");
    }

    
    /** Tests that everything is properly initialized during object construction. */
    @Test public void testInstantation() {
        Assert.assertEquals(descriptor.getId(), "test");
        Assert.assertFalse(descriptor.isForcedAuthenticationSupported());
        Assert.assertFalse(descriptor.isPassiveAuthenticationSupported());
    }

    /** Tests mutating lifetime. */
    @Test public void testLifetime() {
        descriptor.setLifetime(Duration.ofMillis(10));
        Assert.assertEquals(descriptor.getLifetime(), Duration.ofMillis(10));

        try {
            descriptor.setLifetime(Duration.ofMillis(-10));
            Assert.fail();
        } catch (ConstraintViolationException e) {
            Assert.assertEquals(descriptor.getLifetime(), Duration.ofMillis(10));
        }
    }

    /** Tests mutating inactivity timeout. */
    @Test public void testInactivityTimeout() {
        final Duration tenms = Duration.ofMillis(10);
        assert tenms!=null;

        descriptor.setInactivityTimeout(tenms);
        Assert.assertEquals(descriptor.getInactivityTimeout(), Duration.ofMillis(10));

        try {
            final Duration negTenms = Duration.ofMillis(-10);
            assert negTenms!=null;
            descriptor.setInactivityTimeout(negTenms);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            Assert.assertEquals(descriptor.getInactivityTimeout(), Duration.ofMillis(10));
        }
    }
    
    /** Tests mutating forced authentication support. */
    @Test public void testSupportedForcedAuthentication() {
        descriptor.setForcedAuthenticationSupported(true);
        Assert.assertTrue(descriptor.isForcedAuthenticationSupported());

        descriptor.setForcedAuthenticationSupported(false);
        Assert.assertFalse(descriptor.isForcedAuthenticationSupported());
    }

    /** Tests mutating passive authentication support. */
    @Test public void testSupportedPassiveAuthentication() {
        descriptor.setPassiveAuthenticationSupported(true);
        Assert.assertTrue(descriptor.isPassiveAuthenticationSupported());

        descriptor.setPassiveAuthenticationSupported(false);
        Assert.assertFalse(descriptor.isPassiveAuthenticationSupported());
    }
    
    /** Tests mutating principal set. */
    @Test public void testSupportedPrincipals() {
        Assert.assertTrue(descriptor.getSupportedPrincipals(UsernamePrincipal.class).isEmpty());
        
        // Wouldn't use this principal type for real, but it's fine for testing.
        UsernamePrincipal foo = new UsernamePrincipal("foo");
        UsernamePrincipal bar = new UsernamePrincipal("bar");
        UsernamePrincipal baz = new UsernamePrincipal("baz");
        
        descriptor.setSupportedPrincipals(CollectionSupport.arrayAsList(foo));
        Assert.assertEquals(descriptor.getSupportedPrincipals(UsernamePrincipal.class).size(), 1);
        Assert.assertTrue(descriptor.getSupportedPrincipals(UsernamePrincipal.class).contains(foo));

        descriptor.setSupportedPrincipals(CollectionSupport.arrayAsList(foo, bar));
        Assert.assertEquals(descriptor.getSupportedPrincipals(UsernamePrincipal.class).size(), 2);
        Assert.assertTrue(descriptor.getSupportedPrincipals(UsernamePrincipal.class).contains(foo));
        Assert.assertTrue(descriptor.getSupportedPrincipals(UsernamePrincipal.class).contains(bar));
        Assert.assertFalse(descriptor.getSupportedPrincipals(UsernamePrincipal.class).contains(baz));
        
        descriptor.getSupportedPrincipals().add(baz);
        Assert.assertEquals(descriptor.getSupportedPrincipals(UsernamePrincipal.class).size(), 3);
        Assert.assertTrue(descriptor.getSupportedPrincipals(UsernamePrincipal.class).contains(baz));
    }
    
    /**
     * Tests handling of active/inactive checks.
     * 
     * @throws InterruptedException ...
     */
    @Test public void testActiveResults() throws InterruptedException {
        AuthenticationResult result = new AuthenticationResult("test", new UsernamePrincipal("foo"));
        Assert.assertTrue(descriptor.isResultActive(result));
        
        Thread.sleep(20);
        
        descriptor.setLifetime(Duration.ofMillis(10));
        Assert.assertFalse(descriptor.isResultActive(result));
        
        descriptor.setLifetime(Duration.ofSeconds(5));
        Assert.assertTrue(descriptor.isResultActive(result));
        
        Thread.sleep(20);
        final Duration tenms = Duration.ofMillis(10);
        assert tenms!=null;
        descriptor.setInactivityTimeout(tenms);
        Assert.assertFalse(descriptor.isResultActive(result));
        
        result.setLastActivityInstantToNow();
        Assert.assertTrue(descriptor.isResultActive(result));
    }
}
