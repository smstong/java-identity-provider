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

import java.time.Duration;
import java.time.Instant;

import javax.security.auth.Subject;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.SubjectLocality;
import org.opensaml.storage.StorageSerializer;
import org.opensaml.xmlsec.config.BasicXMLSecurityConfiguration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.impl.DefaultAuthenticationResultSerializer;
import net.shibboleth.idp.authn.principal.ProxyAuthenticationPrincipal;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.saml2.profile.config.impl.BrowserSSOProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.testing.ConstantSupplier;

/** {@link AddAuthnStatementToAssertion} unit test. */
@SuppressWarnings("javadoc")
public class AddAuthnStatementToAssertionTest extends OpenSAMLInitBaseTestCase {

    private RequestContext rc;
    
    private ProfileRequestContext prc;

    private AddAuthnStatementToAssertion action;
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        rc = new RequestContextBuilder().setOutboundMessage(
                SAML2ActionTestingSupport.buildResponse()).buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(rc);

        action = new AddAuthnStatementToAssertion();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
        action.initialize();
    }
    
    /** Test that the action errors out properly if there is no authentication context. */
    @Test public void testNoAuthnContext() {
        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
    }

    /** Test that the action errors out properly if there is no relying party context. */
    @Test public void testNoRelyingPartyContext() {
        prc.ensureSubcontext(AuthenticationContext.class);
        prc.removeSubcontext(RelyingPartyContext.class);

        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
    }

    /** Test that the action errors out properly if there is no context. */
    @Test public void testNoContext() {
        prc.setOutboundMessageContext(null);
        prc.ensureSubcontext(AuthenticationContext.class).setAuthenticationResult(
                new AuthenticationResult("Test", new AuthnContextClassRefPrincipal("Test")));

        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_MSG_CTX);
    }

    /**
     * Test that the action proceeds properly returning no assertions if there is no authentication result.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoAuthenticationStatement() throws Exception {
        prc.ensureSubcontext(AuthenticationContext.class);

        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
    }

    /**
     * Test that the authentication statement is properly added.
     * 
     * @throws InterruptedException ...
     * @throws ComponentInitializationException ...
     */
    @Test public void testAddAuthenticationStatement() throws InterruptedException, ComponentInitializationException {
        final Instant now = Instant.now();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);
        
        final StorageSerializer<AuthenticationResult> serializer = new DefaultAuthenticationResultSerializer();
        serializer.initialize();
        
        final AuthenticationFlowDescriptor fd = new AuthenticationFlowDescriptor();
        fd.setId("Test");
        fd.setResultSerializer(serializer);
        fd.initialize();
        
        prc.ensureSubcontext(AuthenticationContext.class).getAvailableFlows().put("Test", fd);
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac !=null;
        ac.setAuthenticationResult(
                new AuthenticationResult("Test", new AuthnContextClassRefPrincipal("Test")));
        
        final MockHttpServletRequest req = ((MockHttpServletRequest) action.getHttpServletRequest());
        assert req!=null;
        req.setRemoteAddr("127.0.0.1");

        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;

        Assert.assertNotNull(omc.getMessage());
        Assert.assertTrue(omc.getMessage() instanceof Response);

        final Response response = (Response) omc.getMessage();
        assert response!=null;
        Assert.assertEquals(response.getAssertions().size(), 1);
        Assert.assertNotNull(response.getAssertions().get(0));

        final Assertion assertion = response.getAssertions().get(0);
        Assert.assertEquals(assertion.getAuthnStatements().size(), 1);
        Assert.assertNotNull(assertion.getAuthnStatements().get(0));

        final AuthnStatement authenticationStatement = assertion.getAuthnStatements().get(0);
        assert authenticationStatement != null;
        final Instant authnInstant = authenticationStatement.getAuthnInstant();
        Assert.assertTrue(authnInstant != null && authnInstant.isAfter(now));
        Assert.assertNotNull(authenticationStatement.getSessionIndex());
        Assert.assertNull(authenticationStatement.getSessionNotOnOrAfter());

        final SubjectLocality locality = authenticationStatement.getSubjectLocality();
        assert locality != null;
        Assert.assertEquals(locality.getAddress(), "127.0.0.1");
        
        final AuthnContext authnContext = authenticationStatement.getAuthnContext();
        assert authnContext != null;
        final AuthnContextClassRef acClass = authnContext.getAuthnContextClassRef();
        assert acClass != null;
        Assert.assertEquals(acClass.getURI(), "Test");
        Assert.assertTrue(authnContext.getAuthenticatingAuthorities().isEmpty());
    }

    /** Test that the authentication statement is properly added. */
    @Test public void testSessionNotOnOrAfter() {
        final BrowserSSOProfileConfiguration ssoConfig = new BrowserSSOProfileConfiguration();
        ssoConfig.setMaximumSPSessionLifetime(Duration.ofHours(1));
        ssoConfig.setSecurityConfiguration(new BasicXMLSecurityConfiguration());
        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx!=null;
        rpCtx.setProfileConfig(ssoConfig);
        
        prc.ensureSubcontext(AuthenticationContext.class).setAuthenticationResult(
                new AuthenticationResult("Test", new AuthnContextClassRefPrincipal("Test")));

        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);

        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;

        final Response response = (Response) omc.getMessage();
        assert response!=null;
        final Assertion assertion = response.getAssertions().get(0);
        final AuthnStatement authenticationStatement = assertion.getAuthnStatements().get(0);
        Assert.assertNotNull(authenticationStatement.getSessionNotOnOrAfter());
    }
    
    /** Test that the authentication statement is properly added with the right method. */
    @Test public void testAddAuthenticationStatementAndMethod() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new AuthnContextClassRefPrincipal("Foo"));
        subject.getPrincipals().add(new AuthnContextClassRefPrincipal("Bar"));
        prc.ensureSubcontext(AuthenticationContext.class).setAuthenticationResult(
                new AuthenticationResult("Test", subject));
        final RequestedPrincipalContext requested = new RequestedPrincipalContext();
        requested.setMatchingPrincipal(new AuthnContextClassRefPrincipal("Bar"));
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac !=null;
        ac.addSubcontext(requested);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);

        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;

        Assert.assertNotNull(omc.getMessage());
        Assert.assertTrue(omc.getMessage() instanceof Response);

        final Response response = (Response) omc.getMessage();
        assert response!=null;
        Assert.assertEquals(response.getAssertions().size(), 1);
        Assert.assertNotNull(response.getAssertions().get(0));

        final Assertion assertion = response.getAssertions().get(0);
        Assert.assertEquals(assertion.getAuthnStatements().size(), 1);
        Assert.assertNotNull(assertion.getAuthnStatements().get(0));

        final AuthnStatement authenticationStatement = assertion.getAuthnStatements().get(0);
        final AuthnContext authnContext = authenticationStatement.getAuthnContext();
        assert authnContext != null;
        final AuthnContextClassRef acClass = authnContext.getAuthnContextClassRef();
        assert acClass != null;
        Assert.assertEquals(acClass.getURI(), "Bar");
        Assert.assertTrue(authnContext.getAuthenticatingAuthorities().isEmpty());
    }
    
    @Test public void testAuthenticatingAuthorities() {
        prc.ensureSubcontext(AuthenticationContext.class).setAuthenticationResult(
                new AuthenticationResult("Test", new ProxyAuthenticationPrincipal(CollectionSupport.listOf("foo", "bar", "baz"))));
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);

        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        final Response response = (Response) omc.getMessage();
        assert response!=null;
        final Assertion assertion = response.getAssertions().get(0);
        final AuthnStatement authenticationStatement = assertion.getAuthnStatements().get(0);
        final AuthnContext authnContext = authenticationStatement.getAuthnContext();
        assert authnContext != null;
        Assert.assertEquals(authnContext.getAuthenticatingAuthorities().size(), 3);
        Assert.assertEquals(authnContext.getAuthenticatingAuthorities().get(0).getURI(), "foo");
        Assert.assertEquals(authnContext.getAuthenticatingAuthorities().get(1).getURI(), "bar");
        Assert.assertEquals(authnContext.getAuthenticatingAuthorities().get(2).getURI(), "baz");
    }

    @Test public void testSuppressedAuthenticatingAuthorities() {
        final BrowserSSOProfileConfiguration ssoConfig = new BrowserSSOProfileConfiguration();
        ssoConfig.setSuppressAuthenticatingAuthority(true);
        ssoConfig.setSecurityConfiguration(new BasicXMLSecurityConfiguration());
        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx!=null;
        rpCtx.setProfileConfig(ssoConfig);

        prc.ensureSubcontext(AuthenticationContext.class).setAuthenticationResult(
                new AuthenticationResult("Test", new ProxyAuthenticationPrincipal(CollectionSupport.listOf("foo", "bar", "baz"))));
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;

        final Response response = (Response) omc.getMessage();
        assert response!=null;
        final Assertion assertion = response.getAssertions().get(0);
        final AuthnStatement authenticationStatement = assertion.getAuthnStatements().get(0);
        final AuthnContext authnContext = authenticationStatement.getAuthnContext();
        assert authnContext != null;
        Assert.assertTrue(authnContext.getAuthenticatingAuthorities().isEmpty());
    }

}
