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

package net.shibboleth.idp.consent.flow.impl;

import org.opensaml.profile.action.EventIds;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.consent.impl.ConsentTestingSupport;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.testing.ConstantSupplier;

/** {@link ExtractConsent} unit test. */
@SuppressWarnings("javadoc")
public class ExtractConsentTest extends AbstractConsentActionTest {

    @BeforeMethod public void setUpCurrentConsents() throws Exception {
        final ConsentContext consentContext = prc.getSubcontext(ConsentContext.class);
        assert consentContext!= null;
        consentContext.getCurrentConsents().putAll(ConsentTestingSupport.newConsentMap());
    }

    @Test public void testMissingHttpServletRequest() throws Exception {
        action = new ExtractConsent();
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);

        final ConsentContext consentContext = prc.getSubcontext(ConsentContext.class);
        assert consentContext!= null;
        final Consent consent1 = consentContext.getCurrentConsents().get("consent1");
        final Consent consent2 = consentContext.getCurrentConsents().get("consent2");
        assert consent1!= null && consent2!= null;
        Assert.assertNotNull(consent2);
        Assert.assertFalse(consent1.isApproved());
        Assert.assertFalse(consent2.isApproved());
    }

    @Test public void testNoUserInput() throws Exception {
        action = new ExtractConsent();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final ConsentContext consentContext = prc.getSubcontext(ConsentContext.class);
        assert consentContext!= null;
        final Consent consent1 = consentContext.getCurrentConsents().get("consent1");
        final Consent consent2 = consentContext.getCurrentConsents().get("consent2");
        Assert.assertNotNull(consent1);
        Assert.assertNotNull(consent2);
        Assert.assertFalse(consent1.isApproved());
        Assert.assertFalse(consent2.isApproved());
    }

    @Test public void testSingleUserInput() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(ExtractConsent.CONSENT_IDS_REQUEST_PARAMETER, "consent1");

        action = new ExtractConsent();
        action.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final ConsentContext consentContext = prc.getSubcontext(ConsentContext.class);
        assert consentContext!= null;
        final Consent consent1 = consentContext.getCurrentConsents().get("consent1");
        final Consent consent2 = consentContext.getCurrentConsents().get("consent2");
        Assert.assertNotNull(consent1);
        Assert.assertNotNull(consent2);
        Assert.assertTrue(consent1.isApproved());
        Assert.assertFalse(consent2.isApproved());
    }

    @Test public void testMultipleUserInput() throws Exception {
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.addParameter(ExtractConsent.CONSENT_IDS_REQUEST_PARAMETER, "consent1");
        httpServletRequest.addParameter(ExtractConsent.CONSENT_IDS_REQUEST_PARAMETER, "consent2");

        action = new ExtractConsent();
        action.setHttpServletRequestSupplier(new ConstantSupplier<>(httpServletRequest));
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final ConsentContext consentContext = prc.getSubcontext(ConsentContext.class);
        assert consentContext!= null;
        final Consent consent1 = consentContext.getCurrentConsents().get("consent1");
        final Consent consent2 = consentContext.getCurrentConsents().get("consent2");
        Assert.assertNotNull(consent1);
        Assert.assertNotNull(consent2);
        Assert.assertTrue(consent1.isApproved());
        Assert.assertTrue(consent2.isApproved());
    }
}
