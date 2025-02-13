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

package net.shibboleth.idp.test.flows.cas;

import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketIdentifierGenerationStrategy;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.idp.cas.ticket.TicketState;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionManager;
import net.shibboleth.idp.test.flows.AbstractFlowTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.execution.FlowExecutionOutcome;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;

import static org.testng.Assert.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Tests the flow behind the <code>/proxy</code> endpoint.
 *
 * @author Marvin S. Addison
 */
@ContextConfiguration(locations = {
        "/test/test-cas-beans.xml",
})
@SuppressWarnings({"javadoc", "null"})
public class ProxyFlowTest extends AbstractFlowTest {

    /** Flow id. */
    @Nonnull
    private static String FLOW_ID = "cas/proxy";

    @Autowired
    @Qualifier("shibboleth.CASTicketService")
    private TicketService ticketService;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    @Qualifier("shibboleth.CASProxyValidateIdPSessionPredicate")
    private ToggleablePredicate validateIdPSessionPredicate;

    @BeforeMethod
    public void disableIdPSessionValidation() {
        validateIdPSessionPredicate.setResult(false);
    }

    @Test
    public void testInvalidRequestNoTicket() throws Exception {
        externalContext.getMockRequestParameterMap().put("targetService", "https://test.example.org/");
        overrideEndStateOutput(FLOW_ID, "ProtocolErrorView");

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        final String responseBody = response.getContentAsString();
        final FlowExecutionOutcome outcome = result.getOutcome();
        assertEquals(outcome.getId(), "ProtocolErrorView");
        assertTrue(responseBody.contains("<cas:proxyFailure code=\"INVALID_REQUEST\">"));
        assertTrue(responseBody.contains("E_TICKET_NOT_SPECIFIED"));
    }

    @Test
    public void testInvalidRequestNoService() throws Exception {
        externalContext.getMockRequestParameterMap().put("pgt", "PGT-123-ABC");
        overrideEndStateOutput(FLOW_ID, "ProtocolErrorView");

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        final String responseBody = response.getContentAsString();
        final FlowExecutionOutcome outcome = result.getOutcome();
        assertEquals(outcome.getId(), "ProtocolErrorView");
        assertTrue(responseBody.contains("<cas:proxyFailure code=\"INVALID_REQUEST\">"));
        assertTrue(responseBody.contains("E_SERVICE_NOT_SPECIFIED"));
    }

    @Test
    public void testSuccess() throws Exception {
        final String principal = "john";
        final IdPSession session = sessionManager.createSession(principal);
        final ProxyGrantingTicket ticket = createProxyGrantingTicket(session.getId(), principal);

        externalContext.getMockRequestParameterMap().put("targetService", ticket.getService());
        externalContext.getMockRequestParameterMap().put("pgt", ticket.getId());

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        final String responseBody = response.getContentAsString();
        assertEquals(result.getOutcome().getId(), "ProxySuccess");
        assertTrue(responseBody.contains("<cas:proxySuccess>"));
        assertTrue(responseBody.contains("<cas:proxyTicket>PT-"));
    }


    @Test
    public void testSuccessWithIdPSessionValidation() throws Exception {
        validateIdPSessionPredicate.setResult(true);
        final String principal = "john";
        final IdPSession session = sessionManager.createSession(principal);
        final ProxyGrantingTicket ticket = createProxyGrantingTicket(session.getId(), principal);

        externalContext.getMockRequestParameterMap().put("targetService", ticket.getService());
        externalContext.getMockRequestParameterMap().put("pgt", ticket.getId());

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        final String responseBody = response.getContentAsString();
        assertEquals(result.getOutcome().getId(), "ProxySuccess");
        assertTrue(responseBody.contains("<cas:proxySuccess>"));
        assertTrue(responseBody.contains("<cas:proxyTicket>PT-"));
    }

    @Test
    public void testFailureTicketExpired() throws Exception {
        externalContext.getMockRequestParameterMap().put("targetService", "https://test.example.org/");
        externalContext.getMockRequestParameterMap().put("pgt", "PGT-123-ABC");

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        assertEquals(result.getOutcome().getId(), "ProtocolErrorView");
        final String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains("<cas:proxyFailure code=\"INVALID_TICKET\""));
        assertTrue(responseBody.contains("E_TICKET_EXPIRED"));
    }

    @Test
    public void testFailureSessionExpired() throws Exception {
        validateIdPSessionPredicate.setResult(true);
        final ProxyGrantingTicket ticket = createProxyGrantingTicket("No-Such-SessionId", "nobody");

        externalContext.getMockRequestParameterMap().put("targetService", ticket.getService());
        externalContext.getMockRequestParameterMap().put("pgt", ticket.getId());

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        assertEquals(result.getOutcome().getId(), "ProtocolErrorView");
        final String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains("<cas:proxyFailure code=\"INVALID_TICKET\""));
        assertTrue(responseBody.contains("E_SESSION_EXPIRED"));
    }

    private ProxyGrantingTicket createProxyGrantingTicket(final String sessionId, final String principal) {
        final ServiceTicket st = ticketService.createServiceTicket(
                new TicketIdentifierGenerationStrategy("ST", 25).generateIdentifier(),
                Instant.now().plusSeconds(10),
                "https://service.example.org/",
                new TicketState(sessionId, principal, Instant.now(), "Password"),
                false);
        return ticketService.createProxyGrantingTicket(
                new TicketIdentifierGenerationStrategy("PGT", 50).generateIdentifier(),
                Instant.now().plus(1, ChronoUnit.HOURS),
                st,
                "https://service.example.org/proxy");
    }
}
