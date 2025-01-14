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

package net.shibboleth.idp.saml.authn.principal;

import net.shibboleth.shared.logic.ConstraintViolationException;

import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link AuthenticationMethodPrincipal} unit test. */
public class AuthenticationMethodPrincipalTest {

    /**
     * Tests that everything is properly initialized during object construction.
     * 
     * @throws CloneNotSupportedException ...
     */
    @Test public void testInstantiation() throws CloneNotSupportedException {
        AuthenticationMethodPrincipal principal =
                new AuthenticationMethodPrincipal(AuthenticationStatement.KERBEROS_AUTHN_METHOD);
        Assert.assertEquals(principal.getName(), AuthenticationStatement.KERBEROS_AUTHN_METHOD);

        AuthenticationMethodPrincipal principal2 = principal.clone();
        Assert.assertEquals(principal.getName(), principal2.getName());

        try {
            new AuthenticationMethodPrincipal("");
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }

        try {
            new AuthenticationMethodPrincipal("   ");
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }
        
    }
}
