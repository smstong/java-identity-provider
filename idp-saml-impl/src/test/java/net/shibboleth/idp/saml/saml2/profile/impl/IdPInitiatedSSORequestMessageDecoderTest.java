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

package net.shibboleth.idp.saml.saml2.profile.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.opensaml.core.testing.XMLObjectBaseTestCase;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.messaging.context.SAMLMessageInfoContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.saml.profile.impl.BaseIdPInitiatedSSORequestMessageDecoder;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.testing.ConstantSupplier;

/**
 * Test the {@link IdPInitiatedSSORequestMessageDecoder}.
 */
@SuppressWarnings("javadoc")
public class IdPInitiatedSSORequestMessageDecoderTest extends XMLObjectBaseTestCase {
    
    private IdPInitiatedSSORequestMessageDecoder decoder;
    
    private MockHttpServletRequest request;
    
    private String entityId = "http://sp.example.org";
    
    private String acsUrl = "http://sp.example.org/acs";
    
    private String relayState = "myRelayState";
    
    private String sessionID = "abc123";
    
    private String messageID;
    
    private Instant time;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        time = Instant.now();
        
        messageID = "_" + sessionID + "!" + Long.toUnsignedString(time.getEpochSecond());
        
        request = new MockHttpServletRequest();
        request.setRequestedSessionId(sessionID);
        
        decoder = new IdPInitiatedSSORequestMessageDecoder();
        assert request!=null;
        decoder.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
        decoder.initialize();
    }
    
    @Test
    public void testDecoder() throws MessageDecodingException {
        request.addParameter(BaseIdPInitiatedSSORequestMessageDecoder.PROVIDER_ID_PARAM,  entityId);
        request.addParameter(BaseIdPInitiatedSSORequestMessageDecoder.SHIRE_PARAM,  acsUrl);
        request.addParameter(BaseIdPInitiatedSSORequestMessageDecoder.TARGET_PARAM,  relayState);
        request.addParameter(BaseIdPInitiatedSSORequestMessageDecoder.TIME_PARAM,  Long.toString(time.getEpochSecond()));
        
        decoder.decode();
        
        final MessageContext messageContext = decoder.getMessageContext();
        assert messageContext!=null;
        final AuthnRequest authnRequest = (AuthnRequest) messageContext.getMessage();
        assert authnRequest!=null;
        
        final Issuer issuer = authnRequest.getIssuer();
        assert issuer != null;
        Assert.assertEquals(issuer.getValue(), entityId, "Incorrect decoded message entityId value");
        Assert.assertEquals(authnRequest.getAssertionConsumerServiceURL(), acsUrl, "Incorrect decoded message ACS URL value");
        Assert.assertEquals(authnRequest.getIssueInstant(), time.truncatedTo(ChronoUnit.SECONDS),
                "Incorrect decoded message issue instant value");
        Assert.assertEquals(authnRequest.getID(), messageID, "Incorrect decoded message ID value");
        
        Assert.assertEquals(messageContext.ensureSubcontext(SAMLPeerEntityContext.class).getEntityId(), entityId,
                "Incorrect decoded entityId value in peer context");
        
        SAMLBindingContext bindingContext = messageContext.ensureSubcontext(SAMLBindingContext.class);
        Assert.assertEquals(bindingContext.getRelayState(), relayState,
                "Incorrect decoded relay state value in binding context");
        Assert.assertEquals(bindingContext.getBindingUri(), "urn:mace:shibboleth:2.0:profiles:AuthnRequest",
                "Incorrect binding URI in binding context");
        
        SAMLMessageInfoContext msgInfoContext = messageContext.ensureSubcontext(SAMLMessageInfoContext.class);
        Assert.assertEquals(msgInfoContext.getMessageIssueInstant(), time.truncatedTo(ChronoUnit.SECONDS),
                "Incorrect decoded issue instant value in message info context");
        Assert.assertEquals(msgInfoContext.getMessageId(), messageID,
                "Incorrect decoded message ID value in message info context");
    }

}
