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

package net.shibboleth.idp.profile.impl;

import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.profile.impl.CheckMandatoryIssuer.NoMessageIssuerException;

import org.opensaml.messaging.context.BasicMessageMetadataSubcontext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link CheckMandatoryIssuer}. */
public class CheckMandatoryIssuerTest {

    @Test
    public void testWithIssuer() throws Exception {
        ProfileRequestContext profileRequestContext = ActionTestingSupport.buildProfileRequestContext();
        RequestContext springRequestContext = ActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        CheckMandatoryIssuer action = new CheckMandatoryIssuer();
        action.setId("test");
        action.initialize();

        Event result = action.execute(springRequestContext);
        ActionTestingSupport.assertProceedEvent(result);
    }

    @Test
    public void testNoIssuer() throws Exception {
        ProfileRequestContext profileRequestContext = ActionTestingSupport.buildProfileRequestContext();
        RequestContext springRequestContext = ActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);        

        CheckMandatoryIssuer action = new CheckMandatoryIssuer();
        action.setId("test");
        action.initialize();
        
        BasicMessageMetadataSubcontext messageMetadata = ActionSupport.getRequiredInboundMessageMetadata(action, profileRequestContext);
        messageMetadata.setMessageIssuer(null);

        try{
        action.execute(springRequestContext);
            Assert.fail();
        } catch (NoMessageIssuerException e) {
            // expected this
        }
    }
}