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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.criterion.ProfileRequestContextCriterion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.execution.FlowExecutionOutcome;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.net.HttpHeaders;

import jakarta.servlet.http.Cookie;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.cas.config.LoginConfiguration;
import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.criterion.SessionIdCriterion;
import net.shibboleth.idp.session.impl.StorageBackedSessionManager;
import net.shibboleth.idp.test.flows.AbstractFlowTest;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.profile.relyingparty.RelyingPartyConfigurationResolver;
import net.shibboleth.profile.relyingparty.VerifiedProfileCriterion;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.net.URISupport;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceableComponent;

/**
 * Tests the flow behind the <code>/login</code> endpoint.
 *
 * @author Marvin S. Addison
 */
@ContextConfiguration(locations = {
        "/test/test-cas-beans.xml"
})
@SuppressWarnings("javadoc")
public class LoginFlowTest extends AbstractFlowTest {

    /** Flow id. */
    @Nonnull
    private static String FLOW_ID = "cas/login";

    @Autowired
    @Qualifier("shibboleth.CASTicketService")
    private TicketService ticketService;

    @Autowired
    private StorageBackedSessionManager sessionManager;

    @Autowired
    @Qualifier("shibboleth.RelyingPartyResolverService")
    private ReloadableService<RelyingPartyConfigurationResolver> relyingPartyConfigurationResolver;

    @BeforeMethod
    public void setUp() throws Exception {
        setPostAuthenticationFlows(CollectionSupport.emptyList());
    }


    @Test
    public void testGatewayNoSession() throws Exception {
        final String service = "https://gateway.example.org/";
        externalContext.getMockRequestParameterMap().put("service", service);
        externalContext.getMockRequestParameterMap().put("gateway", "true");

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        assertEquals(result.getOutcome().getId(), "RedirectToService");
        final String url = externalContext.getExternalRedirectUrl();
        assertTrue(url.contains(service + "?ticket=ST-"));
    }

    @Test
    public void testGatewayNoSessionNoAuth() throws Exception {
        final String service = "https://gateway.example.org/";
        externalContext.getMockRequestParameterMap().put("service", service);
        externalContext.getMockRequestParameterMap().put("gateway", "true");

        // Have to remove the basic-auth creds or a passive login will succeed.
        request.removeHeader(HttpHeaders.AUTHORIZATION);
        
        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        assertEquals(result.getOutcome().getId(), "RedirectToService");
        assertEquals(externalContext.getExternalRedirectUrl(), service);
    }

    @Test
    public void testGatewayWithSession() throws Exception {
        final String service = "https://gateway.example.org/";
        final IdPSession existing = sessionManager.createSession("aurora");
        existing.addAuthenticationResult(new AuthenticationResult("authn/Password", new UsernamePrincipal("aurora")));
        externalContext.getMockRequestParameterMap().put("service", service);
        externalContext.getMockRequestParameterMap().put("gateway", "true");
        overrideEndStateOutput(FLOW_ID, "RedirectToService");
        request.setCookies(new Cookie("shib_idp_session", existing.getId()));
        initializeThreadLocals();

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        final FlowExecutionOutcome outcome = result.getOutcome();
        assertEquals(outcome.getId(), "RedirectToService");
        final String url = externalContext.getExternalRedirectUrl();
        assertTrue(url.contains(service + "?ticket=ST-"));
    }

    @Test
    public void testLoginStartSession() throws Exception {
        // The service below is registered for single logout,
        // which triggers attaching an SPSession to the IdPSession
        final String service = "https://slo.example.org/";
        externalContext.getMockRequestParameterMap().put("service", service);
        overrideEndStateOutput(FLOW_ID, "RedirectToService");

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        final FlowExecutionOutcome outcome = result.getOutcome();
        assertEquals(outcome.getId(), "RedirectToService");
        final String ticketId = getTicketIdFromUrl(externalContext.getExternalRedirectUrl());
        assert ticketId!=null;
        final Ticket st = ticketService.removeServiceTicket(ticketId);
        assert st!=null;
        final String sid = st.getSessionId();
        assert sid!=null;
        final IdPSession session = sessionManager.resolveSingle(
                new CriteriaSet(new SessionIdCriterion(sid)));
        assert session!=null;
        assertEquals(session.getSPSessions().size(), 1);
        assertEquals(session.getSPSessions().iterator().next().getId(), service);
        
        final ProfileRequestContext prc = (ProfileRequestContext) outcome.getOutput().get(END_STATE_OUTPUT_ATTR_NAME);
        assertNotNull(prc.getSubcontext(SubjectContext.class));
        assertPopulatedAttributeContext(prc);
    }

    @Test
    public void testLoginStartSessionWithPostMethod() throws Exception {
        final String service = "https://start.example.org/";
        externalContext.getMockRequestParameterMap().put("service", service);
        externalContext.getMockRequestParameterMap().put("method", "post");
        overrideEndStateOutput(FLOW_ID, "PostBackToService");

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        final FlowExecutionOutcome outcome = result.getOutcome();
        assertEquals(outcome.getId(), "PostBackToService");
        final MockHttpServletResponse mockResponse = (MockHttpServletResponse) externalContext.getNativeResponse();
        final String body = mockResponse.getContentAsString();
        assertTrue(body.contains(String.format("<form action=\"%s\" method=\"post\">", service)));
        assertTrue(body.contains("<input type=\"hidden\" name=\"ticket\" value=\"ST-"));
        final Matcher matcher = Pattern.compile("value=\"([^\"]+)").matcher(body);
        assertTrue(matcher.find());
        assertEquals(1, matcher.groupCount());
        final String ticketId = matcher.group(1);
        assert ticketId!=null;
        final Ticket st = ticketService.removeServiceTicket(ticketId);
        assert st!=null;
        final String sid = st.getSessionId();
        assert sid!=null;
        final IdPSession session = sessionManager.resolveSingle(
                new CriteriaSet(new SessionIdCriterion(sid)));
        assert session!=null;
        
        final ProfileRequestContext prc = (ProfileRequestContext) outcome.getOutput().get(END_STATE_OUTPUT_ATTR_NAME);
        assertNotNull(prc.getSubcontext(SubjectContext.class));
        assertPopulatedAttributeContext(prc);
    }

    @Test
    public void testLoginWithConsent() throws Exception {
        final String service = "https://start.example.org/";
        externalContext.getMockRequestParameterMap().put("service", service);
        setPostAuthenticationFlows(CollectionSupport.singletonList("attribute-release"));
        overrideEndStateOutput(FLOW_ID, "RedirectToService");

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        final FlowExecutionOutcome outcome = result.getOutcome();
        assertEquals(result.getOutcome().getId(), "RedirectToService");
        final String ticketId = getTicketIdFromUrl(externalContext.getExternalRedirectUrl());
        assert ticketId!=null;
        final Ticket st = ticketService.removeServiceTicket(ticketId);
        assert st!=null;
        final String sid = st.getSessionId();
        assert sid!=null;
        final IdPSession session = sessionManager.resolveSingle(
                new CriteriaSet(new SessionIdCriterion(sid)));
        assert session!=null;
        
        // Ensure we passed through the consent intercept subflow
        final ProfileRequestContext prc = (ProfileRequestContext) outcome.getOutput().get(END_STATE_OUTPUT_ATTR_NAME);
        assertNotNull(prc);
        assertNotNull(prc.getSubcontext(SubjectContext.class));
        assertNotNull(prc.getSubcontext(ConsentContext.class));
    }

    @Test
    public void testLoginExistingSession() throws Exception {
        final String service = "https://existing.example.org/";
        final IdPSession existing = sessionManager.createSession("aurora");
        existing.addAuthenticationResult(new AuthenticationResult("authn/Password", new UsernamePrincipal("aurora")));
        externalContext.getMockRequestParameterMap().put("service", service);
        overrideEndStateOutput(FLOW_ID, "RedirectToService");
        request.setCookies(new Cookie("shib_idp_session", existing.getId()));
        initializeThreadLocals();

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        final FlowExecutionOutcome outcome = result.getOutcome();
        assertEquals(outcome.getId(), "RedirectToService");
        final String ticketId = getTicketIdFromUrl(externalContext.getExternalRedirectUrl());
        assert ticketId!=null;
        final Ticket st = ticketService.removeServiceTicket(ticketId);
        assert st!=null;
        final String sid = st.getSessionId();
        assert sid!=null;
        final IdPSession session = sessionManager.resolveSingle(
                new CriteriaSet(new SessionIdCriterion(sid)));
        assert session!=null;
        assertEquals(session.getId(), existing.getId());

        final ProfileRequestContext prc = (ProfileRequestContext) outcome.getOutput().get(END_STATE_OUTPUT_ATTR_NAME);
        assertNotNull(prc.getSubcontext(SubjectContext.class));
        assertPopulatedAttributeContext(prc);
    }

    @Test
    public void testLoginExistingSessionDoNotCache() throws Exception {
        final String service = "https://existing.example.org/";
        final IdPSession existing = sessionManager.createSession("maleficent");
        externalContext.getMockRequestParameterMap().put("service", service);
        overrideEndStateOutput(FLOW_ID, "RedirectToService");
        request.setCookies(new Cookie("shib_idp_session", existing.getId()));
        initializeThreadLocals();

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        final FlowExecutionOutcome outcome = result.getOutcome();
        assertEquals(outcome.getId(), "RedirectToService");
        final String ticketId = getTicketIdFromUrl(externalContext.getExternalRedirectUrl());
        assert ticketId!=null;
        final Ticket st = ticketService.removeServiceTicket(ticketId);
        assert st!=null;
        final String sid = st.getSessionId();
        assert sid!=null;
        final IdPSession session = sessionManager.resolveSingle(
                new CriteriaSet(new SessionIdCriterion(sid)));
        assert session!=null;
        // Expect a new session to be created since authentication was required
        assertNotEquals(session.getId(), existing.getId());

        final ProfileRequestContext prc = (ProfileRequestContext) outcome.getOutput().get(END_STATE_OUTPUT_ATTR_NAME);
        assertNotNull(prc.getSubcontext(SubjectContext.class));
        assertPopulatedAttributeContext(prc);
    }

    @Test
    public void testErrorNoService() throws Exception {
        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        final String responseBody = response.getContentAsString();
        assertEquals(result.getOutcome().getId(), "AuditedErrorView");
        assertTrue(responseBody.contains("ServiceNotSpecified"));
    }

    private void setPostAuthenticationFlows(final List<String> flowIdentifiers) throws Exception {
        final ProfileRequestContext prc = new ProfileRequestContext();
        prc.setProfileId(LoginConfiguration.PROFILE_ID);
        try (final ServiceableComponent<RelyingPartyConfigurationResolver> service =
                relyingPartyConfigurationResolver.getServiceableComponent()) {
            final RelyingPartyConfiguration rpConfig = service.getComponent().resolveSingle(
                    new CriteriaSet(new ProfileRequestContextCriterion(prc),
                                new VerifiedProfileCriterion(true)));
            if (rpConfig == null) {
                throw new IllegalStateException("Relying party configuration not found");
            }
            final LoginConfiguration loginConfiguration =
                    (LoginConfiguration) rpConfig.getProfileConfiguration(prc, LoginConfiguration.PROFILE_ID);
            if (loginConfiguration == null) {
                throw new IllegalStateException("CAS login profile configuration not found");
            }
            loginConfiguration.setPostAuthenticationFlows(flowIdentifiers);
        }
    }

    private void assertPopulatedAttributeContext(final ProfileRequestContext prc) {
        assertNotNull(prc);
        final RelyingPartyContext rpc = prc.getSubcontext(RelyingPartyContext.class);
        assert rpc!=null;
        final AttributeContext ac= rpc.getSubcontext(AttributeContext.class);
        assert ac!=null;
        assertFalse(ac.getUnfilteredIdPAttributes().isEmpty());
    }

    private String getTicketIdFromUrl(final String url) {
        assertTrue(url.contains("ticket=ST-"));
        final String ticketId = url.substring(url.indexOf("ticket=") + 7);
        assertEquals(ticketId.indexOf('/'), -1);
        assertEquals(ticketId.indexOf('+'), -1);
        return URISupport.doURLDecode(ticketId);
    }
}
