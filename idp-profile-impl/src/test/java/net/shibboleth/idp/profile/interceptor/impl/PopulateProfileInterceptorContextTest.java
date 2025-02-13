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

import java.util.List;

import javax.annotation.Nonnull;

import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.FunctionSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link PopulateProfileInterceptorContext} unit test. */
public class PopulateProfileInterceptorContextTest {

    @Nonnull protected List<ProfileInterceptorFlowDescriptor> interceptorFlows = CollectionSupport.emptyList();

    protected RequestContext src;

    protected ProfileRequestContext prc;

    /**
     * Test setup.
     * 
     * @throws Exception 
     */ 
    @BeforeMethod public void setUp() throws Exception {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);

        interceptorFlows = CollectionSupport.listOf(new ProfileInterceptorFlowDescriptor(), new ProfileInterceptorFlowDescriptor(),
                        new ProfileInterceptorFlowDescriptor());
        interceptorFlows.get(0).setId("intercept/test1");
        interceptorFlows.get(1).setId("intercept/test2");
        interceptorFlows.get(2).setId("intercept/test3");

        final PopulateProfileInterceptorContext action = new PopulateProfileInterceptorContext();
        action.setAvailableFlows(interceptorFlows);
        action.setActiveFlowsLookupStrategy(FunctionSupport.constant(CollectionSupport.listOf("test1", "test2", "test3")));
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }

    /**
     * Test that the context is properly added.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testAction() throws Exception {
        final ProfileInterceptorContext interceptorContext = prc.getSubcontext(ProfileInterceptorContext.class);
        assert interceptorContext != null;
        final List<ProfileInterceptorFlowDescriptor> availableFlows =
                List.copyOf(interceptorContext.getAvailableFlows().values());
        Assert.assertEquals(availableFlows.size(), 3);
        Assert.assertEquals(availableFlows.get(0).getId(), "intercept/test1");
        Assert.assertEquals(availableFlows.get(1).getId(), "intercept/test2");
        Assert.assertEquals(availableFlows.get(2).getId(), "intercept/test3");
    }

    /**
     * Test that the context is properly added.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testError() throws Exception {
        final PopulateProfileInterceptorContext action = new PopulateProfileInterceptorContext();
        action.setAvailableFlows(interceptorFlows);
        action.setActiveFlowsLookupStrategy(FunctionSupport.constant(CollectionSupport.singletonList("test4")));
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, IdPEventIds.INVALID_PROFILE_CONFIG);
    }

}
