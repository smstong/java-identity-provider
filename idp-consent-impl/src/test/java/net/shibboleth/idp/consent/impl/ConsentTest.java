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

package net.shibboleth.idp.consent.impl;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.shared.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link Consent} unit test. */
@SuppressWarnings("javadoc")
public class ConsentTest {

    private Consent consent;
    
    private Object nullObj;

    @BeforeMethod public void setUp() {
        consent = new Consent();
        consent.setId("test");
    }

    @Test public void testInstantation() {
        Assert.assertEquals(consent.getId(), "test");
        Assert.assertNull(consent.getValue());
        Assert.assertFalse(consent.isApproved());
    }

    @Test(expectedExceptions = ConstraintViolationException.class) public void testEmptyValue() {
        consent.setValue("");
    }

    @SuppressWarnings("null")
    @Test(expectedExceptions = ConstraintViolationException.class) public void testNullValue() {
        consent.setValue((String) nullObj);
    }

    @Test public void testValue() {
        consent.setValue("value");
        Assert.assertEquals(consent.getValue(), "value");
    }

    @Test public void testIsApproved() {
        consent.setApproved(true);
        Assert.assertTrue(consent.isApproved());

        consent.setApproved(false);
        Assert.assertFalse(consent.isApproved());
    }

    @Test public void testEqualityAndHashCode() {
        Assert.assertEquals(consent, consent);
        Assert.assertNotEquals(consent, null);
        
        final Consent otherConsent = new Consent();
        Assert.assertNotEquals(consent, otherConsent);
        Assert.assertFalse(consent.hashCode() == otherConsent.hashCode());

        otherConsent.setId("test");
        Assert.assertEquals(consent, otherConsent);
        Assert.assertTrue(consent.hashCode() == otherConsent.hashCode());

        consent.setValue("value");
        Assert.assertNotEquals(consent, otherConsent);
        Assert.assertFalse(consent.hashCode() == otherConsent.hashCode());

        otherConsent.setValue("value");
        Assert.assertEquals(consent, otherConsent);
        Assert.assertTrue(consent.hashCode() == otherConsent.hashCode());

        consent.setApproved(true);
        Assert.assertNotEquals(consent, otherConsent);
        Assert.assertFalse(consent.hashCode() == otherConsent.hashCode());

        otherConsent.setApproved(true);
        Assert.assertEquals(consent, otherConsent);
        Assert.assertTrue(consent.hashCode() == otherConsent.hashCode());
    }
}
