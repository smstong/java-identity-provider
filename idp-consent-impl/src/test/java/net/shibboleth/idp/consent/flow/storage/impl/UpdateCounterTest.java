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

import org.opensaml.storage.StorageRecord;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.profile.testing.ActionTestingSupport;

/** {@link UpdateCounter} unit test. */
@SuppressWarnings("javadoc")
public class UpdateCounterTest extends AbstractConsentStorageActionTest {

    @BeforeMethod public void setUpAction() throws Exception {
        action = new UpdateCounter();
        populateAction();
    }

    @Test public void testCreateCounter() throws Exception {
        action.initialize();

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        final StorageRecord<?> record = getMemoryStorageService().read("context", "key");
        assert record!=null;
        Assert.assertEquals(record.getVersion(), 1);
    }

    @Test public void testUpdateCounter() throws Exception {
        action.initialize();

        Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        StorageRecord<?> record = getMemoryStorageService().read("context", "key");
        assert record!=null;
        Assert.assertEquals(record.getVersion(), 1);

        event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);

        record = getMemoryStorageService().read("context", "key");
        assert record!=null;
        Assert.assertEquals(record.getVersion(), 2);
    }

}
