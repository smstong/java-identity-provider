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

package net.shibboleth.idp.saml.session;

import java.time.Instant;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.NameID;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link SAML2SPSession} unit test. */
public class SAML2SPSessionTest extends OpenSAMLInitBaseTestCase {

    /**
     * Tests that everything is properly initialized during object construction.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testInstantiation() throws Exception {
        
        NameID nameID = (NameID) XMLObjectSupport.buildXMLObject(NameID.DEFAULT_ELEMENT_NAME);
        nameID.setValue("joe@example.org");
        nameID.setFormat(NameID.EMAIL);
        
        Instant start = Instant.now();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);

        SAML2SPSession session = new SAML2SPSession("test", Instant.now(),
                Instant.now().plusSeconds(60), nameID, "1234567890", "https://sp.example.org/acs", false);
        Assert.assertEquals(session.getId(), "test");
        Assert.assertTrue(session.getCreationInstant().isAfter(start));
        Assert.assertTrue(session.getExpirationInstant().isAfter(session.getCreationInstant()));
        Assert.assertSame(session.getNameID(), nameID);
        Assert.assertEquals(session.getSessionIndex(), "1234567890");
        Assert.assertEquals(session.getSPSessionKey(), "joe@example.org");
        Assert.assertEquals(session.getACSLocation(), "https://sp.example.org/acs");
        Assert.assertFalse(session.supportsLogoutPropagation());
    }

}