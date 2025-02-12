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

package net.shibboleth.idp.consent.flow.ar.impl;

import java.util.HashMap;
import java.util.Map;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.context.AttributeReleaseContext;
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.consent.impl.ConsentTestingSupport;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.profile.context.RelyingPartyContext;

import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link ReleaseAttributes} unit test. */
@SuppressWarnings("javadoc")
public class ReleaseAttributesTest extends AbstractAttributeReleaseActionTest {

    private AttributeReleaseContext arc;

    @BeforeMethod void setUpAction() throws Exception {
        action = new ReleaseAttributes();

        final Consent consentToAttribute1 = new Consent();
        consentToAttribute1.setId("attribute1");
        consentToAttribute1.setApproved(true);

        final Consent consentToAttribute2 = new Consent();
        consentToAttribute2.setId("attribute2");
        consentToAttribute2.setApproved(false);

        final Map<String, Consent> consent = new HashMap<>();
        consent.put(consentToAttribute1.getId(), consentToAttribute1);
        consent.put(consentToAttribute2.getId(), consentToAttribute2);

        final ConsentContext consentCtx = prc.getSubcontext(ConsentContext.class);
        assert consentCtx!=null;
        consentCtx.getPreviousConsents().putAll(consent);

        arc = prc.getSubcontext(AttributeReleaseContext.class);
    }

    @Test public void testAllAttributesAreConsentable() throws Exception {

        final Map<String, IdPAttribute> consentableAttributes = ConsentTestingSupport.newAttributeMap();
        arc.getConsentableAttributes().clear();
        arc.getConsentableAttributes().putAll(consentableAttributes);

        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx != null;
        final AttributeContext attrCtx = rpCtx.getSubcontext(AttributeContext.class);
        assert attrCtx != null;
        Assert.assertEquals(attrCtx.getIdPAttributes().size(), 1);
        Assert.assertTrue(attrCtx.getIdPAttributes().containsKey("attribute1"));
        Assert.assertFalse(attrCtx.getIdPAttributes().containsKey("attribute2"));
        Assert.assertFalse(attrCtx.getIdPAttributes().containsKey("attribute3"));
    }

    @Test public void testSomeAttributesAreConsentable() throws Exception {

        final Map<String, IdPAttribute> consentableAttributes = ConsentTestingSupport.newAttributeMap();
        consentableAttributes.remove("attribute2");
        arc.getConsentableAttributes().clear();
        arc.getConsentableAttributes().putAll(consentableAttributes);

        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);
        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx != null;
        final AttributeContext attrCtx = rpCtx .getSubcontext(AttributeContext.class);
        assert attrCtx != null;

        Assert.assertEquals(attrCtx.getIdPAttributes().size(), 2);
        Assert.assertTrue(attrCtx.getIdPAttributes().containsKey("attribute1"));
        Assert.assertTrue(attrCtx.getIdPAttributes().containsKey("attribute2"));
        Assert.assertFalse(attrCtx.getIdPAttributes().containsKey("attribute3"));
    }

}
