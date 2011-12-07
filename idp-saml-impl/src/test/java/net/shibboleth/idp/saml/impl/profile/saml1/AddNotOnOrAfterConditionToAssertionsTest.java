/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.saml.impl.profile.saml1;

import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.saml1.core.Assertion;
import org.opensaml.saml1.core.Conditions;
import org.opensaml.saml1.core.Response;
import org.opensaml.xml.Configuration;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/** {@link AddNotOnOrAfterConditionToAssertions} unit test. */
public class AddNotOnOrAfterConditionToAssertionsTest {

    @BeforeSuite()
    public void initOpenSAML() throws InitializationException {
        InitializationService.initialize();
    }

    /** Test that action errors out properly if there is no response. */
    @Test
    public void testNoResponse() throws Exception {
        ProfileRequestContext<Object, Response> profileRequestContext =
                ActionTestingSupport.buildProfileRequestContext();

        Saml1ActionTestingSupport.buildRelyingPartySubcontext(profileRequestContext, null);

        RequestContext springRequestContext = ActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        AddNotOnOrAfterConditionToAssertions action = new AddNotOnOrAfterConditionToAssertions();
        action.setId("test");
        action.initialize();

        try {
            action.execute(springRequestContext);
            Assert.fail();
        } catch (ProfileException e) {
            // expected this
        }
    }

    /** Test that action errors out properly if there is no assertion in the response. */
    @Test
    public void testNoAssertion() throws Exception {
        ProfileRequestContext<Object, Response> profileRequestContext =
                ActionTestingSupport.buildProfileRequestContext();

        profileRequestContext.getOutboundMessageContext().setMessage(Saml1ActionTestingSupport.buildResponse());

        Saml1ActionTestingSupport.buildRelyingPartySubcontext(profileRequestContext, null);

        RequestContext springRequestContext = ActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        AddNotOnOrAfterConditionToAssertions action = new AddNotOnOrAfterConditionToAssertions();
        action.setId("test");
        action.initialize();

        try {
            action.execute(springRequestContext);
            Assert.fail();
        } catch (ProfileException e) {
            // expected this
        }
    }

    /**
     * Test that the condition is properly added if there is a single assertion, without a Conditions element, in the
     * response.
     */
    @Test
    public void testSingleAssertion() throws Exception {
        Assertion assertion = Saml1ActionTestingSupport.buildAssertion();

        Response response = Saml1ActionTestingSupport.buildResponse();
        response.getAssertions().add(assertion);

        ProfileRequestContext<Object, Response> profileRequestContext =
                ActionTestingSupport.buildProfileRequestContext();
        profileRequestContext.getOutboundMessageContext().setMessage(response);

        Saml1ActionTestingSupport.buildRelyingPartySubcontext(profileRequestContext, null);

        RequestContext springRequestContext = ActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        AddNotOnOrAfterConditionToAssertions action = new AddNotOnOrAfterConditionToAssertions();
        action.setId("test");
        action.initialize();

        Event result = action.execute(springRequestContext);
        ActionTestingSupport.assertProceedEvent(result);

        Assert.assertNotNull(response.getAssertions());
        Assert.assertEquals(response.getAssertions().size(), 1);

        Assert.assertNotNull(assertion.getConditions());
        Assert.assertNotNull(assertion.getConditions().getNotOnOrAfter());
    }

    /**
     * Test that the condition is properly added if there is a single assertion, with a Conditions element, in the
     * response.
     */
    @Test
    public void testSingleAssertionWithExistingConditions() throws Exception {
        SAMLObjectBuilder<Conditions> conditionsBuilder =
                (SAMLObjectBuilder<Conditions>) Configuration.getBuilderFactory().getBuilder(Conditions.TYPE_NAME);
        Conditions conditions = conditionsBuilder.buildObject();

        Assertion assertion = Saml1ActionTestingSupport.buildAssertion();
        assertion.setConditions(conditions);

        Response response = Saml1ActionTestingSupport.buildResponse();
        response.getAssertions().add(assertion);

        ProfileRequestContext<Object, Response> profileRequestContext =
                ActionTestingSupport.buildProfileRequestContext();
        profileRequestContext.getOutboundMessageContext().setMessage(response);

        Saml1ActionTestingSupport.buildRelyingPartySubcontext(profileRequestContext, null);

        RequestContext springRequestContext = ActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        AddNotOnOrAfterConditionToAssertions action = new AddNotOnOrAfterConditionToAssertions();
        action.setId("test");
        action.initialize();

        Event result = action.execute(springRequestContext);
        ActionTestingSupport.assertProceedEvent(result);

        Assert.assertNotNull(assertion.getConditions());
        Assert.assertSame(assertion.getConditions(), conditions);
        Assert.assertNotNull(assertion.getConditions().getNotOnOrAfter());
    }

    /** Test that the condition is properly added if there are multiple assertions in the response. */
    @Test
    public void testMultipleAssertion() throws Exception {
        Response response = Saml1ActionTestingSupport.buildResponse();
        response.getAssertions().add(Saml1ActionTestingSupport.buildAssertion());
        response.getAssertions().add(Saml1ActionTestingSupport.buildAssertion());
        response.getAssertions().add(Saml1ActionTestingSupport.buildAssertion());

        ProfileRequestContext<Object, Response> profileRequestContext =
                ActionTestingSupport.buildProfileRequestContext();
        profileRequestContext.getOutboundMessageContext().setMessage(response);

        Saml1ActionTestingSupport.buildRelyingPartySubcontext(profileRequestContext, null);

        RequestContext springRequestContext = ActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        AddNotOnOrAfterConditionToAssertions action = new AddNotOnOrAfterConditionToAssertions();
        action.setId("test");
        action.initialize();

        Event result = action.execute(springRequestContext);
        ActionTestingSupport.assertProceedEvent(result);

        Assert.assertNotNull(response.getAssertions());
        Assert.assertEquals(response.getAssertions().size(), 3);

        for (Assertion assertion : response.getAssertions()) {
            Assert.assertNotNull(assertion.getConditions());
            Assert.assertNotNull(assertion.getConditions().getNotOnOrAfter());
        }
    }
}