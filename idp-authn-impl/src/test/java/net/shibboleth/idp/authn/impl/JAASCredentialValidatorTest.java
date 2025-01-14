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

package net.shibboleth.idp.authn.impl;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.AuthenticationErrorContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.authn.principal.impl.ExactPrincipalEvalPredicateFactory;
import net.shibboleth.idp.authn.testing.TestPrincipal;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.net.URISupport;
import net.shibboleth.shared.spring.resource.ResourceHelper;
import net.shibboleth.shared.testing.ConstantSupplier;
import net.shibboleth.shared.testing.InMemoryDirectory;

/** Unit test for JAAS validation. */
@SuppressWarnings("javadoc")
public class JAASCredentialValidatorTest extends BaseAuthenticationContextTest {

    private static final String DATA_PATH = "src/test/resources/net/shibboleth/idp/authn/impl/";

    private static final String DATA_CLASSPATH = "/net/shibboleth/idp/authn/impl/";

    private JAASCredentialValidator validator;
    
    private ValidateCredentials action;

    private InMemoryDirectory directoryServer;

    /**
     * Creates an UnboundID in-memory directory server. Leverages LDIF found in test resources.
     */
    @BeforeClass public void setupDirectoryServer() {
        directoryServer =
            new InMemoryDirectory(
                new String[] {"dc=shibboleth,dc=net"},
                new ClassPathResource(DATA_CLASSPATH + "loginLDAPTest.ldif"),
                10389);
        directoryServer.start();
    }

    /**
     * Shutdown the in-memory directory server.
     * 
     * @throws Exception
     */
    @AfterClass public void teardownDirectoryServer() throws Exception {
        if (directoryServer.openConnectionCount() > 0) {
            Thread.sleep(100);
        }
        assertEquals(directoryServer.openConnectionCount(), 0);
        directoryServer.stop(true);
    }

    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();

        validator = new JAASCredentialValidator();
        validator.setId("jaastest");
        
        action = new ValidateCredentials();
        assert validator != null;
        action.setValidators(CollectionSupport.singletonList(validator));
        
        final Map<String,Collection<String>> mappings = new HashMap<>();
        mappings.put("UnknownUsername", CollectionSupport.singleton("DN_RESOLUTION_FAILURE"));
        mappings.put("InvalidPassword", CollectionSupport.singleton("INVALID_CREDENTIALS"));
        action.setClassifiedMessages(mappings);
        
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
    }

    @Test public void testMissingFlow() throws ComponentInitializationException {
        validator.initialize();
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
    }

    @Test public void testMissingUser() throws ComponentInitializationException {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        validator.initialize();
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testMissingUser2() throws ComponentInitializationException {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        ac.ensureSubcontext(UsernamePasswordContext.class);
        
        validator.initialize();
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testNoConfig() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "foo");
        getMockHttpServletRequest(action).addParameter("password", "bar");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac!= null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        validator.initialize();
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_CREDENTIALS);
        AuthenticationErrorContext errorCtx = ac.getSubcontext(AuthenticationErrorContext.class);
        assert errorCtx != null;
        Assert.assertEquals(errorCtx.getExceptions().size(), 1);
        Assert.assertTrue(errorCtx.getExceptions().get(0) instanceof LoginException);
    }

    @Test public void testBadConfig() throws ComponentInitializationException, URISyntaxException, IOException {
        getMockHttpServletRequest(action).addParameter("username", "foo");
        getMockHttpServletRequest(action).addParameter("password", "bar");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        validator.setLoginConfigNames(CollectionSupport.singletonList("ShibBadAuth"));
        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigParameters(URISupport.fileURIFromAbsolutePath(getCurrentDir()
                + '/' + DATA_PATH + "jaas.config"));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_CREDENTIALS);
        AuthenticationErrorContext errorCtx = ac.getSubcontext(AuthenticationErrorContext.class);
        assert errorCtx != null;
        Assert.assertEquals(errorCtx.getExceptions().size(), 1);
        Assert.assertTrue(errorCtx.getExceptions().get(0) instanceof LoginException);
    }

    @Test public void testUnsupportedConfig() throws ComponentInitializationException, URISyntaxException, IOException {
        getMockHttpServletRequest(action).addParameter("username", "foo");
        getMockHttpServletRequest(action).addParameter("password", "bar");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        final RequestedPrincipalContext rpc = ac.ensureSubcontext(RequestedPrincipalContext.class);
        assert rpc!= null;
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(CollectionSupport.<Principal>singletonList(new TestPrincipal("test1")));

        validator.setLoginConfigurations(CollectionSupport.singletonList(new Pair<String,Collection<Principal>>("ShibUserPassAuth",
                CollectionSupport.singletonList(new TestPrincipal("test2")))));
        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigParameters(URISupport.fileURIFromAbsolutePath(getCurrentDir()
                + '/' + DATA_PATH + "jaas.config"));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.REQUEST_UNSUPPORTED);
    }
    
    @Test public void testUnmatchedUser() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "foo");
        getMockHttpServletRequest(action).addParameter("password", "bar");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        ac.ensureSubcontext(UsernamePasswordContext.class);
        
        validator.setMatchExpression(Pattern.compile("foo.+"));
        validator.initialize();
        
        action.initialize();
        
        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.REQUEST_UNSUPPORTED);
    }

    @Test public void testBadUsername() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "foo");
        getMockHttpServletRequest(action).addParameter("password", "bar");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigResource(ResourceHelper.of(new ClassPathResource(DATA_CLASSPATH + "jaas.config")));
        
        validator.initialize();
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "UnknownUsername");
        AuthenticationErrorContext errorCtx = ac.getSubcontext(AuthenticationErrorContext.class);
        assert errorCtx != null;
        Assert.assertTrue(errorCtx.getExceptions().get(0) instanceof LoginException);
        Assert.assertTrue(errorCtx.isClassifiedError("UnknownUsername"));
        Assert.assertFalse(errorCtx.isClassifiedError("InvalidPassword"));
    }

    @Test public void testBadPassword() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL");
        getMockHttpServletRequest(action).addParameter("password", "bar");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigResource(ResourceHelper.of(new ClassPathResource(DATA_CLASSPATH + "jaas.config")));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "InvalidPassword");
        AuthenticationErrorContext errorCtx = ac.getSubcontext(AuthenticationErrorContext.class);
        assert errorCtx != null; 
        Assert.assertTrue(errorCtx.getExceptions().get(0) instanceof LoginException);
        Assert.assertFalse(errorCtx.isClassifiedError("UnknownUsername"));
        Assert.assertTrue(errorCtx.isClassifiedError("InvalidPassword"));
    }

    @Test public void testAuthorized() throws ComponentInitializationException, URISyntaxException, IOException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL");
        getMockHttpServletRequest(action).addParameter("password", "changeit");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));

        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigParameters(URISupport.fileURIFromAbsolutePath(getCurrentDir()
                + '/' + DATA_PATH + "jaas.config"));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationResult ar = ac.getAuthenticationResult();
        assert ar != null;
        Assert.assertEquals(ar.getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL");
    }

    @Test public void testAuthorizedAndKeep() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL");
        getMockHttpServletRequest(action).addParameter("password", "changeit");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac!= null;
        ac.setAttemptedFlow(authenticationFlows.get(0));

        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigResource(ResourceHelper.of(new ClassPathResource(DATA_CLASSPATH + "jaas.config")));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationResult ar = ac.getAuthenticationResult();
        assert ar != null;
        Assert.assertEquals(ar.getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL");
    }

    @Test public void testSupported() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL");
        getMockHttpServletRequest(action).addParameter("password", "changeit");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        final RequestedPrincipalContext rpc = ac.ensureSubcontext(RequestedPrincipalContext.class);
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(CollectionSupport.<Principal>singletonList(new TestPrincipal("test1")));

        validator.setLoginConfigurations(CollectionSupport.singletonList(new Pair<>("ShibUserPassAuth",
                CollectionSupport.singletonList(new TestPrincipal("test1")))));
        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigResource(ResourceHelper.of(new ClassPathResource(DATA_CLASSPATH + "jaas.config")));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationResult ar = ac.getAuthenticationResult();
        assert ar != null;
        Assert.assertEquals(ar.getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL");
        Assert.assertEquals(ar.getSubject().getPrincipals(TestPrincipal.class).iterator()
                .next().getName(), "test1");
    }
    
    @Test public void testMultiConfigAuthorized() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL");
        getMockHttpServletRequest(action).addParameter("password", "changeit");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));

        validator.setLoginConfigNames(Arrays.asList("ShibBadAuth", "ShibUserPassAuth"));
        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigResource(ResourceHelper.of(new ClassPathResource(DATA_CLASSPATH + "jaas.config")));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationResult ar = ac.getAuthenticationResult();
        assert ar != null;
        Assert.assertEquals(ar.getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL");
    }
    
    @Test public void testMatchAndAuthorized() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addParameter("username", "PETER_THE_PRINCIPAL");
        getMockHttpServletRequest(action).addParameter("password", "changeit");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));

        validator.setLoginConfigType("JavaLoginConfig");
        validator.setLoginConfigResource(ResourceHelper.of(new ClassPathResource(DATA_CLASSPATH + "jaas.config")));
        validator.setMatchExpression(Pattern.compile(".+_THE_.+"));
        validator.initialize();
        
        action.initialize();

        doExtract();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationResult ar = ac.getAuthenticationResult();
        assert ar != null;
        Assert.assertEquals(ar.getSubject().getPrincipals(UsernamePrincipal.class).iterator()
                .next().getName(), "PETER_THE_PRINCIPAL");
    }
    
    private void doExtract() throws ComponentInitializationException {
        final ExtractUsernamePasswordFromFormRequest extract = new ExtractUsernamePasswordFromFormRequest();
        extract.setHttpServletRequestSupplier(action.getHttpServletRequestSupplier());
        extract.initialize();
        extract.execute(src);
    }

    private String getCurrentDir() throws IOException {

        final String currentDir = new java.io.File(".").getCanonicalPath();

        return currentDir.replace(File.separatorChar, '/');
    }
    
}