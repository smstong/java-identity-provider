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

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml1.core.Response;
import org.springframework.webflow.execution.FlowExecutionOutcome;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.Assert;

import net.shibboleth.idp.test.flows.AbstractFlowTest;

/**
 * Abstract SAML 1 flow test.
 */
public abstract class AbstractSAML1FlowTest extends AbstractFlowTest {

    /**
     * Validate the {@link FlowExecutionResult} using a default {@link SAML1TestResponseValidator}.
     * 
     * @param result the flow execution result
     * @param flowId the flow ID
     */
    public void validateResult(@Nonnull final FlowExecutionResult result, @Nonnull final String flowId) {
        validateResult(result, flowId, new SAML1TestResponseValidator());
    }

    /**
     * Validate the {@link FlowExecutionResult} using the given {@link SAML1TestResponseValidator}.
     * 
     * @param result the flow execution result
     * @param flowId the flow ID
     * @param validator the response validator
     */
    public void validateResult(@Nonnull final FlowExecutionResult result, @Nonnull final String flowId,
            @Nonnull final SAML1TestResponseValidator validator) {
        assertFlowExecutionResult(result, flowId);
        validator.validateResponse(getResponse(result));
    }

    /**
     * Get the SAML response from the flow result.
     * 
     * @param result the flow result
     * @return the SAML response
     */
    public Response getResponse(@Nonnull FlowExecutionResult result) {        
        final FlowExecutionOutcome outcome = result.getOutcome();
        assertFlowExecutionOutcome(outcome);

        final ProfileRequestContext prc = (ProfileRequestContext) outcome.getOutput().get(END_STATE_OUTPUT_ATTR_NAME);
        assertProfileRequestContext(prc);
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        Assert.assertTrue(omc.getMessage() instanceof Response);

        return (Response) omc.getMessage();
    }
}
