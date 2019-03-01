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

package net.shibboleth.idp.authn.context;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactoryRegistry;
import net.shibboleth.idp.authn.principal.TestPrincipal;

import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link AuthenticationContext} unit test. */
@Test
public class AuthenticationContextTest {

    /** Tests initiation instant instantiation. */
    public void testInitiationInstant() throws Exception {
        Instant start = Instant.now();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);

        AuthenticationContext ctx = new AuthenticationContext();
        Assert.assertTrue(ctx.getInitiationInstant().isAfter(start));
    }

    /** Tests mutating forcing authentication. */
    public void testForcingAuthentication() throws Exception {
        AuthenticationContext ctx = new AuthenticationContext();
        Assert.assertFalse(ctx.isForceAuthn());

        ctx.setForceAuthn(true);
        Assert.assertTrue(ctx.isForceAuthn());
    }

    /** Tests active results. */
    public void testActiveResults() throws Exception {
        final AuthenticationResult result = new AuthenticationResult("test", new Subject());

        final AuthenticationContext ctx = new AuthenticationContext();
        Assert.assertTrue(ctx.getActiveResults().isEmpty());
        
        ctx.setActiveResults(Arrays.asList(result));

        Assert.assertEquals(ctx.getActiveResults().size(), 1);
        Assert.assertEquals(ctx.getActiveResults().get("test"), result);
    }
    
    /** Tests potential flow instantiation. */
    public void testPotentialFlows() throws Exception {
        AuthenticationContext ctx = new AuthenticationContext();
        Assert.assertTrue(ctx.getPotentialFlows().isEmpty());

        final AuthenticationFlowDescriptor descriptor = new AuthenticationFlowDescriptor();
        descriptor.setId("test");
        ctx = new AuthenticationContext();
        ctx.getPotentialFlows().put(descriptor.getId(), descriptor);
        Assert.assertEquals(ctx.getPotentialFlows().size(), 1);
        Assert.assertEquals(ctx.getPotentialFlows().get("test"), descriptor);
    }

    /** Tests mutating attempted flow. */
    public void testAttemptedFlow() throws Exception {
        final AuthenticationContext ctx = new AuthenticationContext();
        Assert.assertNull(ctx.getAttemptedFlow());

        final AuthenticationFlowDescriptor descriptor = new AuthenticationFlowDescriptor();
        descriptor.setId("test");
        ctx.setAttemptedFlow(descriptor);
        Assert.assertEquals(ctx.getAttemptedFlow(), descriptor);
    }

    /** Tests setting completion instant. */
    public void testCompletionInstant() throws Exception {
        final AuthenticationContext ctx = new AuthenticationContext();
        Assert.assertNull(ctx.getCompletionInstant());

        Instant now = Instant.now();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);

        ctx.setCompletionInstant();
        Assert.assertTrue(ctx.getCompletionInstant().isAfter(now));
    }
    
    /** Tests RequestedPrincipalContext helpers. */
    public void testRequestedPrincipalContextHelpers() throws Exception {
        final AuthenticationContext ctx = new AuthenticationContext();
        ctx.setPrincipalEvalPredicateFactoryRegistry(new PrincipalEvalPredicateFactoryRegistry());
        
        ctx.addRequestedPrincipalContext("foo", new TestPrincipal("bar"), false);
        RequestedPrincipalContext rpCtx = ctx.getSubcontext(RequestedPrincipalContext.class);
        Assert.assertNotNull(rpCtx);
        Assert.assertEquals(rpCtx.getOperator(), "foo");
        Assert.assertEquals(rpCtx.getRequestedPrincipals(), Collections.singletonList(new TestPrincipal("bar")));
        
        Assert.assertFalse(ctx.addRequestedPrincipalContext("foo", new TestPrincipal("bar"), false));
        
        ctx.addRequestedPrincipalContext("fob", TestPrincipal.class.getName(), "baz", true);
        rpCtx = ctx.getSubcontext(RequestedPrincipalContext.class);
        Assert.assertNotNull(rpCtx);
        Assert.assertEquals(rpCtx.getOperator(), "fob");
        Assert.assertEquals(rpCtx.getRequestedPrincipals(), Collections.singletonList(new TestPrincipal("baz")));

        ctx.addRequestedPrincipalContext("fog", TestPrincipal.class.getName(), Arrays.asList("baf", "bag"), true);
        rpCtx = ctx.getSubcontext(RequestedPrincipalContext.class);
        Assert.assertNotNull(rpCtx);
        Assert.assertEquals(rpCtx.getOperator(), "fog");
        Assert.assertEquals(rpCtx.getRequestedPrincipals().size(), 2);
    }
    
    @Test(expectedExceptions = ClassCastException.class)
    public void testRequestedPrincipalContextHelperBadType() throws Exception {
        final AuthenticationContext ctx = new AuthenticationContext();
        ctx.setPrincipalEvalPredicateFactoryRegistry(new PrincipalEvalPredicateFactoryRegistry());
        
        ctx.addRequestedPrincipalContext("fob", AuthenticationContext.class.getName(), "baz", false);
    }
    
}