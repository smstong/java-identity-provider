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

package net.shibboleth.idp.saml.profile.impl;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;

import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.component.ComponentInitializationException;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link InitializeRelyingPartyContextFromSAMLPeer} unit test. */
@SuppressWarnings("javadoc")
public class InitializeRelyingPartyContextFromSAMLPeerTest {
    
    private RequestContext src;
    
    private ProfileRequestContext prc;

    private InitializeRelyingPartyContextFromSAMLPeer action;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        prc.removeSubcontext(RelyingPartyContext.class);

        action = new InitializeRelyingPartyContextFromSAMLPeer();
        action.initialize();
    }

    @Test
    public void testNoPeerContext() {
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
    }
    

    @Test
    public void testPeerContext() {
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        final SAMLPeerEntityContext peer =
                imc.ensureSubcontext(SAMLPeerEntityContext.class);
        peer.setEntityId("foo");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final RelyingPartyContext rp = prc.getSubcontext(RelyingPartyContext.class);
        assert rp!=null;
        Assert.assertSame(rp.getRelyingPartyIdContextTree(), peer);
        Assert.assertEquals(rp.getRelyingPartyId(), "foo");
    }
    
}