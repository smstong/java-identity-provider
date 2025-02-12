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

package net.shibboleth.idp.profile.interceptor.impl;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.logic.PredicateSupport;

/** {@link SelectProfileInterceptorFlow} unit test. */
@SuppressWarnings("javadoc")
public class SelectProfileInterceptorFlowTest extends PopulateProfileInterceptorContextTest {

    private SelectProfileInterceptorFlow action;

    private ProfileInterceptorContext interceptorCtx;

    @BeforeMethod public void setUp() throws Exception {
        super.setUp();

        action = new SelectProfileInterceptorFlow();
        action.initialize();

        interceptorCtx = prc.getSubcontext(ProfileInterceptorContext.class);
    }

    @Test public void testSelect() {

        final Event event = action.execute(src);
        assert event != null;
        ActionTestingSupport.assertEvent(event, ProfileInterceptorFlowDescriptor.FLOW_ID_PREFIX + "test1");

        final ProfileInterceptorFlowDescriptor flow =interceptorCtx.getAttemptedFlow();
        assert flow != null && flow.equals(interceptorCtx.getAvailableFlows().get(ProfileInterceptorFlowDescriptor.FLOW_ID_PREFIX + "test1"));
        Assert.assertEquals(flow.getId(), event.getId());
    }

    @Test public void testIncompleteFlows() {
        action.execute(src);
        final Event event = action.execute(src);
        assert event != null;
        ActionTestingSupport.assertEvent(event, ProfileInterceptorFlowDescriptor.FLOW_ID_PREFIX + "test2");

        final ProfileInterceptorFlowDescriptor flow =interceptorCtx.getAttemptedFlow();
        assert flow != null && flow.equals(interceptorCtx.getAvailableFlows().get(ProfileInterceptorFlowDescriptor.FLOW_ID_PREFIX + "test2"));
        Assert.assertEquals(flow.getId(), event.getId());
    }

    @Test public void testPredicate() {
        interceptorCtx.getAvailableFlows().get(ProfileInterceptorFlowDescriptor.FLOW_ID_PREFIX + "test1")
                .setActivationCondition(PredicateSupport.<ProfileRequestContext> alwaysFalse());

        final Event event = action.execute(src);
        assert event != null;
        ActionTestingSupport.assertEvent(event, ProfileInterceptorFlowDescriptor.FLOW_ID_PREFIX + "test2");

        final ProfileInterceptorFlowDescriptor flow =interceptorCtx.getAttemptedFlow();
        assert flow != null && flow.equals(interceptorCtx.getAvailableFlows().get(ProfileInterceptorFlowDescriptor.FLOW_ID_PREFIX + "test2"));
        Assert.assertEquals(flow.getId(), event.getId());
    }

}