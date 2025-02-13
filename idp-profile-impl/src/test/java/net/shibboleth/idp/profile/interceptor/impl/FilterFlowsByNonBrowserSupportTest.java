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

import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;

/** {@link FilterFlowsByNonBrowserSupport} unit test. */
@SuppressWarnings({"javadoc", "null"})
public class FilterFlowsByNonBrowserSupportTest extends PopulateProfileInterceptorContextTest {

    private FilterFlowsByNonBrowserSupport action;

    @BeforeMethod public void setUp() throws Exception {
        super.setUp();

        action = new FilterFlowsByNonBrowserSupport();
        action.initialize();
    }

    @Test public void testBrowserProfile() {
        final ProfileInterceptorContext interceptorCtx = prc.getSubcontext(ProfileInterceptorContext.class);
        assert interceptorCtx != null;
        prc.setBrowserProfile(true);

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(interceptorCtx.getAvailableFlows().size(), 3);
    }

    @Test public void testNoFiltering() {
        final ProfileInterceptorContext interceptorCtx = prc.getSubcontext(ProfileInterceptorContext.class);
        assert interceptorCtx != null;

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(interceptorCtx.getAvailableFlows().size(), 3);
    }

    @Test public void testPartialFiltering() {
        final ProfileInterceptorContext interceptorCtx = prc.getSubcontext(ProfileInterceptorContext.class);
        assert interceptorCtx != null;
        interceptorCtx.getAvailableFlows().get("intercept/test1").setNonBrowserSupported(false);

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(interceptorCtx.getAvailableFlows().size(), 2);
        Assert.assertNotNull(interceptorCtx.getAvailableFlows().get("intercept/test2"));
        Assert.assertNotNull(interceptorCtx.getAvailableFlows().get("intercept/test3"));
    }
}
