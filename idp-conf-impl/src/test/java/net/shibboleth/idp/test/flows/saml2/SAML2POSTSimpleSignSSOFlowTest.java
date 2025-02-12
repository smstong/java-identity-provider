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

package net.shibboleth.idp.test.flows.saml2;

import java.io.UnsupportedEncodingException;

import javax.annotation.Nonnull;

import net.shibboleth.shared.codec.Base64Support;
import net.shibboleth.shared.codec.EncodingException;
import net.shibboleth.shared.xml.SerializeSupport;

import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

/**
 * SAML 2 POST-SimpleSign SSO flow test.
 */
@SuppressWarnings({"javadoc", "null"})
public class SAML2POSTSimpleSignSSOFlowTest extends AbstractSAML2SSOFlowTest {

    /** Flow id. */
    @Nonnull public final static String FLOW_ID = "SAML2/POST-SimpleSign/SSO";

    /**
     * Test the SAML 2 POST SSO flow.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testSAML2POSTSSOFlow() throws Exception {

        buildRequest();

        overrideEndStateOutput(FLOW_ID);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        validateResult(result, FLOW_ID);
    }

    /**
     * Build the {@link MockHttpServletRequest}.
     * 
     * @throws Exception if an error occurs
     */
    public void buildRequest() throws Exception {

        request.setMethod("POST");
        request.setRequestURI("/idp/profile/" + FLOW_ID);

        final AuthnRequest authnRequest = buildAuthnRequest(request);
        authnRequest.setDestination(getDestinationPostSimpleSign(request));

        final MessageContext messageContext =
                buildOutboundMessageContext(authnRequest, SAMLConstants.SAML2_POST_SIMPLE_SIGN_BINDING_URI);
        final SAMLObject message = (SAMLObject) messageContext.getMessage();
        assert message!=null;
        final String encodedMessage = encodeMessage(message);
        request.addParameter("SAMLRequest", encodedMessage);
    }

    /**
     * Base64 the given SAML message.
     * 
     * @param message the SAML message
     * @return Base64 encoded message
     * @throws MarshallingException if there is a problem marshalling the XMLObject
     * @throws UnsupportedEncodingException If the named charset is not supported
     * @throws EncodingException if the message can not be base64 encoded
     */
    @Nonnull public String encodeMessage(@Nonnull final SAMLObject message) throws MarshallingException,
            UnsupportedEncodingException, EncodingException {
        final Element domMessage = XMLObjectSupport.marshall(message);
        final String messageXML = SerializeSupport.nodeToString(domMessage);

        return Base64Support.encode(messageXML.getBytes("UTF-8"), Base64Support.UNCHUNKED);
    }
}
