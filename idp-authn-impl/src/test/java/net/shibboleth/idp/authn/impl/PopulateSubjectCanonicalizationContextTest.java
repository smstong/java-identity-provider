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

import java.util.List;

import javax.security.auth.Subject;

import net.shibboleth.idp.authn.SubjectCanonicalizationFlowDescriptor;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.testing.ActionTestingSupport;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link PopulateSubjectCanonicalizationContext} unit test and base class for further action tests. */
@SuppressWarnings("javadoc")
public class PopulateSubjectCanonicalizationContextTest {

    protected List<SubjectCanonicalizationFlowDescriptor> c14nFlows;

    protected RequestContext src;
    
    protected ProfileRequestContext prc;
    
    @BeforeMethod public void setUp() throws Exception {        
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        prc.ensureSubcontext(SubjectCanonicalizationContext.class).setSubject(new Subject());

        c14nFlows = List.of(new SubjectCanonicalizationFlowDescriptor(),
                new SubjectCanonicalizationFlowDescriptor(), new SubjectCanonicalizationFlowDescriptor());
        c14nFlows.get(0).setId("test1");
        c14nFlows.get(1).setId("test2");
        c14nFlows.get(2).setId("test3");

        final PopulateSubjectCanonicalizationContext action = new PopulateSubjectCanonicalizationContext();
        action.setAvailableFlows(c14nFlows);
        action.initialize();

        action.execute(src);
    }

    /**
     * Test that the context is properly added.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testAction() throws Exception {
        
        ActionTestingSupport.assertProceedEvent(prc);
        final SubjectCanonicalizationContext c14nCtx = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert c14nCtx!=null;

        Assert.assertEquals(c14nCtx.getPotentialFlows().size(), 3);
        Assert.assertNotNull(c14nCtx.getPotentialFlows().get("test1"));
    }
}
