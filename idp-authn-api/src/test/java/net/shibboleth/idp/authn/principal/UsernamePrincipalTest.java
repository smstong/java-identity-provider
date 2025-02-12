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

package net.shibboleth.idp.authn.principal;

import javax.security.auth.Subject;

import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.shared.logic.ConstraintViolationException;

/** {@link UsernamePrincipal} unit test. */
public class UsernamePrincipalTest {

    /** Tests that everything is properly initialized during object construction. */
    @Test public void testInstantiation() {
        UsernamePrincipal principal = new UsernamePrincipal("bob");
        Assert.assertEquals(principal.getName(), "bob");

        try {
            new UsernamePrincipal("");
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }

        try {
            new UsernamePrincipal("   ");
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }
    }
    
    /** Tests that equality is working correctly. */
    @Test public void testSubjectBehavior() {
        final Subject subject = new Subject();
        Assert.assertTrue(subject.getPrincipals(UsernamePrincipal.class).isEmpty());
        
        subject.getPrincipals().add(new UsernamePrincipal("jdoe"));
        Assert.assertEquals(subject.getPrincipals(UsernamePrincipal.class).size(), 1);

        subject.getPrincipals().add(new UsernamePrincipal("jdoe"));
        Assert.assertEquals(subject.getPrincipals(UsernamePrincipal.class).size(), 1);
    }
    
}