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

package net.shibboleth.idp.authn.impl;


import java.util.Arrays;
import java.util.Map;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.testing.ConstantSupplier;

/** {@link PreserveAuthenticationFlowState} unit test. */
@SuppressWarnings("javadoc")
public class PreserveAuthenticationFlowStateTest extends BaseAuthenticationContextTest {
    
    private PreserveAuthenticationFlowState action; 
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        action = new PreserveAuthenticationFlowState();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
        action.setParameterNames(Arrays.asList("foo", "foo2"));
        action.initialize();
    }
    
    @Test public void testNoServlet() throws ComponentInitializationException {
        action = new PreserveAuthenticationFlowState();
        action.initialize();
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        final Map<String, Object> asm = ac.getAuthenticationStateMap();
        Assert.assertTrue(asm.isEmpty());
    }

    @Test public void testNoParameters() throws ComponentInitializationException {
        action = new PreserveAuthenticationFlowState();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
        action.initialize();
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        final Map<String, Object> asm = ac.getAuthenticationStateMap();
        Assert.assertTrue(asm.isEmpty());
    }

    @Test public void testNoneFound(){
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        final Map<String, Object> asm = ac.getAuthenticationStateMap();
        Assert.assertTrue(asm.isEmpty());
    }
    
    @Test public void testNoValues() {
        
        getMockHttpServletRequest(action).addParameter("foo", (String) null);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        Assert.assertEquals(authCtx.getAuthenticationStateMap().size(), 1);
        Assert.assertTrue(authCtx.getAuthenticationStateMap().containsKey("foo"));
        Assert.assertNull(authCtx.getAuthenticationStateMap().get("foo"));
    }
    
    @Test public void testSingleValued() {
        
        getMockHttpServletRequest(action).addParameter("foo", "bar");
        getMockHttpServletRequest(action).addParameter("foo2", "bar2");
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        Assert.assertEquals(authCtx.getAuthenticationStateMap().size(), 2);
        Assert.assertEquals(authCtx.getAuthenticationStateMap().get("foo"), "bar");
        Assert.assertEquals(authCtx.getAuthenticationStateMap().get("foo2"), "bar2");
    }
    
    @Test public void testMultiValued() {
        
        getMockHttpServletRequest(action).addParameter("foo", new String[]{"bar", "bar2"});
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        Assert.assertEquals(authCtx.getAuthenticationStateMap().size(), 1);
        Assert.assertEquals(authCtx.getAuthenticationStateMap().get("foo"), Arrays.asList("bar", "bar2"));
    }
    
}