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

package net.shibboleth.idp.test.flows;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.security.credential.Credential;
import org.opensaml.soap.soap11.Body;
import org.opensaml.soap.soap11.Envelope;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.binding.expression.Expression;
import org.springframework.binding.expression.ExpressionParser;
import org.springframework.binding.expression.support.FluentParserContext;
import org.springframework.binding.mapping.impl.DefaultMapper;
import org.springframework.binding.mapping.impl.DefaultMapping;
import org.springframework.core.io.ClassPathResource;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.definition.registry.FlowDefinitionLocator;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.EndState;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.execution.FlowExecutionOutcome;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.springframework.webflow.executor.FlowExecutor;
import org.springframework.webflow.executor.FlowExecutorImpl;
import org.springframework.webflow.expression.spel.WebFlowSpringELExpressionParser;
import org.springframework.webflow.test.MockExternalContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import com.google.common.net.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.shibboleth.idp.spring.IdPPropertiesApplicationContextInitializer;
import net.shibboleth.idp.test.PreferFileSystemApplicationContextInitializer;
import net.shibboleth.idp.test.PreferFileSystemContextLoader;
import net.shibboleth.idp.test.TestEnvironmentApplicationContextInitializer;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;
import net.shibboleth.shared.security.IdentifierGenerationStrategy.ProviderType;
import net.shibboleth.shared.servlet.impl.HttpServletRequestResponseContext;
import net.shibboleth.shared.spring.security.factory.X509CertificateFactoryBean;
import net.shibboleth.shared.testing.InMemoryDirectory;
import net.shibboleth.shared.xml.ParserPool;

/**
 * Abstract flow test.
 */
@ContextConfiguration(
        locations = {
                "/net/shibboleth/idp/conf/global-system.xml",
                "/net/shibboleth/idp/conf/mvc-beans.xml",
                "/net/shibboleth/idp/conf/webflow-config.xml",
                "/test/test-beans.xml",
                "/test/override-beans.xml",},
        initializers = {
                TestEnvironmentApplicationContextInitializer.class,
                PreferFileSystemApplicationContextInitializer.class,
                IdPPropertiesApplicationContextInitializer.class},
        loader = PreferFileSystemContextLoader.class)
@WebAppConfiguration
@SuppressWarnings({"null"})
public abstract class AbstractFlowTest extends AbstractTestNGSpringContextTests {

    /** Logger. */
    @Nonnull private final static Logger log = LoggerFactory.getLogger(AbstractFlowTest.class);

    /** Path to LDIF file to be imported into directory server. */
    @Nonnull public final static String LDIF_FILE = "/test/test-ldap.ldif";

    /** Path to keystore file to be used by the directory server. */
    @Nonnull public final static String KEYSTORE_FILE = "/test/test-ldap.keystore";

    /** The IDP entity ID. */
    @Nonnull public final static String IDP_ENTITY_ID = "https://idp.example.org";

    /** The SP entity ID. */
    @Nonnull public final static String SP_ENTITY_ID = "https://sp.example.org";

    /** The SP ACS URL. */
    @Nonnull public final static String SP_ACS_URL = "https://localhost:8443/sp/SAML1/POST/ACS";

    /** The SP relay state. */
    @Nonnull public final static String SP_RELAY_STATE = "myRelayState";

    /** The end state ID. */
    @Nonnull public final static String END_STATE_ID = "end";

    /** The end state output attribute expression which retrieves the profile request context. */
    @Nonnull public final static String END_STATE_OUTPUT_ATTR_EXPR = "flowRequestContext.getConversationScope().get('"
            + ProfileRequestContext.BINDING_KEY + "')";

    /** The name of the end state flow output attribute containing the profile request context. */
    @Nonnull public final static String END_STATE_OUTPUT_ATTR_NAME = "ProfileRequestContext";

    /** The name of the bean which maps principals to IP ranges for IP address based authn. */
    @Nonnull public final static String IP_ADDRESS_AUTHN_MAP_BEAN_NAME = "shibboleth.authn.IPAddress.Mappings";

    /** The flow ID for IP address based authn. */
    @Nonnull public final static String IP_ADDRESS_AUTHN_FLOW_ID = "authn/IPAddress";

    /** The name of the bean defining the SAML 1 Direct c14n descriptor. */
    @Nonnull public final static String SAML1_TRANSFORM_C14N_BEAN_NAME = "c14n/SAML1Transform";

    /** The name of the bean defining the SAML 2 Direct c14n descriptor. */
    @Nonnull public final static String SAML2_TRANSFORM_C14N_BEAN_NAME = "c14n/SAML2Transform";

    /** In-memory directory server. A single instance is used for all child tests. */
    @NonnullAfterInit private InMemoryDirectory directoryServer;

    /** Mock external context. */
    protected MockExternalContext externalContext;

    /** The web flow executor. */
    protected FlowExecutor flowExecutor;

    /** Mock request. */
    protected MockHttpServletRequest request;

    /** Mock response. */
    protected MockHttpServletResponse response;

    /** Parser pool */
    @NonnullAfterInit protected static ParserPool parserPool;

    /** XMLObject builder factory */
    @NonnullAfterInit protected static XMLObjectBuilderFactory builderFactory;

    /** XMLObject marshaller factory */
    @NonnullAfterInit protected static MarshallerFactory marshallerFactory;

    /** XMLObject unmarshaller factory */
    @NonnullAfterInit protected static UnmarshallerFactory unmarshallerFactory;

    /** UUID identifier generation strategy. */
    protected IdentifierGenerationStrategy idGenerator;

    /** IdP credential wired via test/test-beans.xml. */
    @Qualifier("test.idp.Credential") @Autowired protected Credential idpCredential;

    /** SP credential wired via test/test-beans.xml. */
    @Qualifier("test.sp.Credential") @Autowired protected Credential spCredential;

    /** SP certificate wired via test/test-beans.xml. */
    @Autowired @Qualifier("test.sp.X509Certificate") protected X509CertificateFactoryBean certFactoryBean;

    /**
     * {@link HttpServletRequestResponseContext#clearCurrent()}
     */
    @AfterMethod public void clearThreadLocals() {
        HttpServletRequestResponseContext.clearCurrent();
    }

    /**
     * Initialize the web flow executor.
     */
    @BeforeMethod public void initializeFlowExecutor() {
        assert applicationContext!=null;
        flowExecutor = applicationContext.getBean("flowExecutor", FlowExecutor.class);
        Assert.assertNotNull(flowExecutor);
    }

    /**
     * Initialize mock request, response, and external context.
     */
    @BeforeMethod public void initializeMocks() {
        request = new MockHttpServletRequest();
        // add basic auth header for jdoe:changeit, see test-ldap.ldif
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic amRvZTpjaGFuZ2VpdA==");
        response = new MockHttpServletResponse();
        externalContext = new MockExternalContext();
        externalContext.setNativeRequest(request);
        externalContext.setNativeResponse(response);
    }

    /**
     * {@link HttpServletRequestResponseContext#loadCurrent(HttpServletRequest, HttpServletResponse)}
     */
    @BeforeMethod public void initializeThreadLocals() {
        assert request!=null && response!=null;
        HttpServletRequestResponseContext.loadCurrent(request, response);
    }

    /**
     * Initialize XMLObject support classes.
     */
    @BeforeClass public void initializeXMLObjectSupport() {
        parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
        unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
        idGenerator = IdentifierGenerationStrategy.getInstance(ProviderType.UUID);
    }

    /**
     * Creates an UnboundID in-memory directory server. Leverages LDIF found at {@value #LDIF_FILE}.
     */
    @SuppressWarnings("null")
    @BeforeSuite public void setupDirectoryServer() {
        directoryServer =
            new InMemoryDirectory(
                new String[] {"dc=example,dc=org", "ou=system"},
                new ClassPathResource(LDIF_FILE),
                10389,
                new ClassPathResource(KEYSTORE_FILE),
                Optional.empty());
        directoryServer.start();
    }

    /**
     * Shutdown the in-memory directory server.
     * 
     * Always run this method to avoid starting the server multiple times when tests fail.
     */
    @AfterSuite(alwaysRun = true) public void teardownDirectoryServer() {
        if (directoryServer != null) {
            directoryServer.stop(true);
        }
    }

    /**
     * Assert that the flow execution result is not null, has ended, and its flow id equals the given flow id.
     * 
     * @param result the flow execution result
     * @param flowID the flow id
     */
    public void assertFlowExecutionResult(@Nullable final FlowExecutionResult result, @Nonnull String flowID) {
        assert result!=null;
        Assert.assertEquals(result.getFlowId(), flowID);
        Assert.assertTrue(result.isEnded());
    }

    /**
     * Assert that the flow execution outcome is not null and its id equals {@value #END_STATE_ID}. For testing
     * purposes, the outcome's attribute map must map {@value #END_STATE_OUTPUT_ATTR_NAME} to the
     * {@link ProfileRequestContext}.
     * 
     * @param outcome the flow execution outcome
     */
    public void assertFlowExecutionOutcome(@Nullable final FlowExecutionOutcome outcome) {
        assertFlowExecutionOutcome(outcome, END_STATE_ID);
    }

    /**
     * Assert that the flow execution outcome is not null and its id equals the given end state id. For testing
     * purposes, the outcome's attribute map must map {@value #END_STATE_OUTPUT_ATTR_NAME} to the
     * {@link ProfileRequestContext}.
     * 
     * @param outcome the flow execution outcome
     * @param endStateId the end state id
     */
    public void assertFlowExecutionOutcome(@Nullable final FlowExecutionOutcome outcome,
            @Nullable final String endStateId) {
        Assert.assertNotNull(outcome, "Flow ended with an error");
        assert outcome != null;
        Assert.assertEquals(outcome.getId(), endStateId);
        Assert.assertTrue(outcome.getOutput().contains(END_STATE_OUTPUT_ATTR_NAME));
        Assert.assertTrue(outcome.getOutput().get(END_STATE_OUTPUT_ATTR_NAME) instanceof ProfileRequestContext);
    }

    /**
     * Assert that the profile request context has an outbound message context and that the outbound message context has
     * a message.C     * 
     * @param profileRequestContext the profile request context
     */
    public void assertProfileRequestContext(@Nullable final ProfileRequestContext profileRequestContext) {
        assert profileRequestContext!=null;
        final MessageContext omc = profileRequestContext.getOutboundMessageContext();
        assert omc != null && omc.getMessage() != null;
    }

    /**
     * Build a SOAP11 {@link Envelope} with the given payload.
     * 
     * @param payload the payload
     * @return the SOAP11 envelop
     */
    @Nonnull public static Envelope buildSOAP11Envelope(@Nonnull final XMLObject payload) {
        final XMLObjectBuilderFactory bf = builderFactory;
        assert bf != null;
        final XMLObjectBuilder<?> bfe = bf.getBuilder(Envelope.DEFAULT_ELEMENT_NAME);
        final XMLObjectBuilder<?> bfb = bf.getBuilder(Body.DEFAULT_ELEMENT_NAME);
        assert bfe!=null && bfb!=null;
        final Envelope envelope =
                (Envelope) bfe.buildObject(
                        Envelope.DEFAULT_ELEMENT_NAME);
        final Body body =
                (Body) bfb.buildObject(Body.DEFAULT_ELEMENT_NAME);

        body.getUnknownXMLObjects().add(payload);
        envelope.setBody(body);

        return envelope;
    }

    /**
     * Get the {@link Flow} with the given flow ID.
     * 
     * @param flowID the flow ID
     * 
     * @return the {@link Flow}
     * 
     * @see FlowDefinitionLocator#getFlowDefinition(String)
     */
    @Nonnull public Flow getFlow(@Nonnull final String flowID) {
        Constraint.isNotNull(flowID, "Flow ID can not be null");

        Constraint.isTrue(flowExecutor instanceof FlowExecutorImpl, "The flow executor must be an instance of "
                + FlowExecutorImpl.class);

        final FlowDefinition flowDefinition =
                ((FlowExecutorImpl) flowExecutor).getDefinitionLocator().getFlowDefinition(flowID);

        Constraint.isTrue(flowDefinition instanceof Flow, "The flow definition must be an instance of " + Flow.class);
        
        return (Flow) flowDefinition;
    }

    /**
     * Map the {@link ProfileRequestContext} as an end state output attribute with name
     * {@value #END_STATE_OUTPUT_ATTR_NAME} by assembling the flow with the given flow ID and manually setting the
     * output attributes of the end state with ID {@value #END_STATE_ID}.
     * 
     * @param flowID the flow ID
     */
    public void overrideEndStateOutput(@Nonnull final String flowID) {
        overrideEndStateOutput(flowID, END_STATE_ID);
    }

    /**
     * Map the {@link ProfileRequestContext} as an end state output attribute with name
     * {@value #END_STATE_OUTPUT_ATTR_NAME} by assembling the flow with the given flow ID and manually setting the
     * output attributes of the end state with the given id.
     * 
     * @param flowID the flow ID
     * @param endStateId the end state ID
     */
    public void overrideEndStateOutput(@Nonnull final String flowID, @Nonnull final String endStateId) {
        final FlowDefinition flow = getFlow(flowID);

        final ExpressionParser parser = new WebFlowSpringELExpressionParser(new SpelExpressionParser());
        final Expression source =
                parser.parseExpression(END_STATE_OUTPUT_ATTR_EXPR,
                        new FluentParserContext().evaluate(RequestContext.class));
        final Expression target =
                parser.parseExpression(END_STATE_OUTPUT_ATTR_NAME,
                        new FluentParserContext().evaluate(MutableAttributeMap.class));
        final DefaultMapping defaultMapping = new DefaultMapping(source, target);
        final DefaultMapper defaultMapper = new DefaultMapper();
        defaultMapper.addMapping(defaultMapping);

        final EndState endState = (EndState) flow.getState(endStateId);
        endState.setOutputMapper(defaultMapper);
    }

    /**
     * Add flows defined in a child flow definition registry to its parent registry.
     * @param flowID the flow ID
     * @param childRegistryID the child flow registry ID
     */
    public void registerFlowsInParentRegistry(@Nonnull final String flowID, @Nonnull final String childRegistryID) {
        Constraint.isNotNull(flowID, "Flow ID can not be null");
        Constraint.isNotNull(childRegistryID, "Flow registry ID can not be null");

        final Flow flow = getFlow(flowID);

        final FlowDefinitionRegistry flowRegistry =
                flow.getApplicationContext().getBean(childRegistryID, FlowDefinitionRegistry.class);

        Constraint.isNotNull(flowRegistry.getParent(), "Child flow registry must have a parent");

        for (final String flowDefinitionId : flowRegistry.getFlowDefinitionIds()) {
            log.debug("Adding flow '{}' from child registry to parent registry", flowDefinitionId);
            flowRegistry.getParent().registerFlowDefinition(flowRegistry.getFlowDefinition(flowDefinitionId));
        }
    }

    /**
     * Get the {@link ProfileRequestContext} from the output attributes of the result.
     * 
     * @param result the flow execution result
     * @return the profile request context or null
     */
    @Nullable public ProfileRequestContext retrieveProfileRequestContext(@Nonnull final FlowExecutionResult result) {
        Constraint.isNotNull(result, "Result can not be null");
        return (ProfileRequestContext) result.getOutcome().getOutput().get(END_STATE_OUTPUT_ATTR_NAME);
    }

}
