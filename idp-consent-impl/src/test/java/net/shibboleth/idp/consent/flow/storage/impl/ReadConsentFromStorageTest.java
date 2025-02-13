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

package net.shibboleth.idp.consent.flow.storage.impl;

import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.consent.impl.ConsentTestingSupport;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;

import org.opensaml.storage.impl.MemoryStorageService;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link ReadConsentFromStorage} unit test. */
@SuppressWarnings("javadoc")
public class ReadConsentFromStorageTest extends AbstractConsentStorageActionTest {

    @BeforeMethod public void setUpAction() throws Exception {
        action = new ReadConsentFromStorage();
        populateAction();
    }

    @Test public void testReadConsentFromStorage() throws Exception {

        final MemoryStorageService ss = getMemoryStorageService();
        ss.create("context", "key", ConsentTestingSupport.newConsentMap(),
                ((AbstractConsentStorageAction) action).getStorageSerializer(), null);

        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final ConsentContext consentCtx = prc.getSubcontext(ConsentContext.class);
        assert consentCtx!=null;
        Assert.assertEquals(consentCtx.getPreviousConsents(), ConsentTestingSupport.newConsentMap());
    }

    @Test public void testReadEmptyStorage() throws Exception {
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final ConsentContext consentCtx = prc.getSubcontext(ConsentContext.class);
        assert consentCtx!=null;
        Assert.assertTrue(consentCtx.getPreviousConsents().isEmpty());
    }
}
