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

package net.shibboleth.idp.authn.proxy.impl;


import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.testing.ConstantSupplier;

/** {@link ExtractDiscoveryResponse} unit test. */
@SuppressWarnings("javadoc")
public class ExtractDiscoveryResponseTest extends BaseAuthenticationContextTest {
    
    private ExtractDiscoveryResponse action; 
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        action = new ExtractDiscoveryResponse();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
        action.initialize();
    }
    
    @Test public void testNoServlet() throws ComponentInitializationException {
        action = new ExtractDiscoveryResponse();
        action.initialize();
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.getAuthenticatingAuthority();
    }

    @Test public void testFailure() {
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.getAuthenticatingAuthority();
    }

    @Test public void testSuccess() {
        getMockHttpServletRequest(action).addParameter("entityID", "foo");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        Assert.assertEquals(ac.getAuthenticatingAuthority(), "foo");
    }

}