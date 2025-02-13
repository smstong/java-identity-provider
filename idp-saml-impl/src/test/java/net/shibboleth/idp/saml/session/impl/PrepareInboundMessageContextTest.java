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

package net.shibboleth.idp.saml.session.impl;

import java.time.Instant;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.saml2.testing.SAML2ActionTestingSupport;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.saml.session.SAML2SPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.context.LogoutPropagationContext;
import net.shibboleth.shared.component.ComponentInitializationException;

/** {@link PrepareInboundMessageContext} unit test. */
@SuppressWarnings({"javadoc", "null"})
public class PrepareInboundMessageContextTest extends OpenSAMLInitBaseTestCase {

    private RequestContext src;
    
    private ProfileRequestContext prc;
    
    private PrepareInboundMessageContext action;

    @BeforeMethod public void setUp() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        prc.setInboundMessageContext(null);
               
        final SAML2SPSession session = new SAML2SPSession("https://sp.example.org", Instant.now(),
                Instant.now().plusSeconds(1800), SAML2ActionTestingSupport.buildNameID("jdoe"), "foo", null, true);
        prc.ensureSubcontext(LogoutPropagationContext.class).setSession(session);
        
        action = new PrepareInboundMessageContext();
        action.initialize();
    }

    @Test public void testNoLogoutPropagationContext() {
        prc.removeSubcontext(LogoutPropagationContext.class);

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
        Assert.assertNull(prc.getInboundMessageContext());
    }

    @Test public void testNoSession() {
        final LogoutPropagationContext lpc = prc.getSubcontext(LogoutPropagationContext.class);
        assert lpc!=null;
        
        lpc.setSession(null);
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
        Assert.assertNull(prc.getInboundMessageContext());
    }

    @Test public void testSuccess() {
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        final SAMLPeerEntityContext ctx = imc.getSubcontext(SAMLPeerEntityContext.class);
        assert ctx!=null;
        final LogoutPropagationContext lpc = prc.getSubcontext(LogoutPropagationContext.class);
        assert lpc!=null;
        final SPSession session = lpc.getSession();
        assert session!=null;
        Assert.assertEquals(ctx.getEntityId(), session.getId());
    }

}