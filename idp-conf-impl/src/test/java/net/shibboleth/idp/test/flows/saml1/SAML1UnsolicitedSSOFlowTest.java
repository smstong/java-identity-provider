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

package net.shibboleth.idp.test.flows.saml1;

import javax.annotation.Nonnull;

import net.shibboleth.idp.saml.profile.impl.BaseIdPInitiatedSSORequestMessageDecoder;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * SAML 1 unsolicited SSO flow test.
 */
@SuppressWarnings({"null"})
public class SAML1UnsolicitedSSOFlowTest extends AbstractSAML1FlowTest {

    /** The flow id. */
    @Nonnull public final static String FLOW_ID = "Shibboleth/SSO";

    /**
     * Test the SAML1 unsolicited SSO flow.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML1UnsolicitedSSOFlow() throws Exception {

        // NOTE: This test can fail for a subtle reason involving the use of attribute push with SAML 1.1
        // We are triggering that setting by relying on a ByReference metadata filter to attach a
        // profile setting to the test metadata, so it fails for reasons that can be non-obvious if that
        // mechanism breaks, and will fail on the attribute statement checking step.
        
        buildRequest();

        overrideEndStateOutput(FLOW_ID);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        validateResult(result, FLOW_ID);

        Assert.assertEquals(getResponse(result).getRecipient(), SP_ACS_URL);
    }

    /**
     * Build the {@link MockHttpServletRequest}.
     */
    public void buildRequest() {
        request.setMethod("GET");
        // TODO time request parameter ?
        request.addParameter(BaseIdPInitiatedSSORequestMessageDecoder.PROVIDER_ID_PARAM, SP_ENTITY_ID);
        request.addParameter(BaseIdPInitiatedSSORequestMessageDecoder.SHIRE_PARAM, SP_ACS_URL);
        request.addParameter(BaseIdPInitiatedSSORequestMessageDecoder.TARGET_PARAM, SP_RELAY_STATE);
    }
}
