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

package net.shibboleth.idp.cas.flow.impl;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.cas.protocol.ProtocolContext;
import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.idp.cas.service.ServiceContext;
import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.cas.ticket.TicketContext;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;

import javax.annotation.Nonnull;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.test.MockExternalContext;
import org.springframework.webflow.test.MockRequestContext;

/**
 * Helper component to build the Webflow {@link RequestContext} needed for flow action tests.
 *
 * @author Marvin S. Addison
 */
@SuppressWarnings("javadoc")
public class TestContextBuilder {

    /** Root of context tree for our tests. */
    @Nonnull private ProfileRequestContext profileRequestContext = new ProfileRequestContext();

    public TestContextBuilder(final String profileId) {
        profileRequestContext.setProfileId(profileId);
    }

    @Nonnull public TestContextBuilder addAuthenticationContext(final AuthenticationResult result) {
        final AuthenticationContext authnCtx = new AuthenticationContext();
        authnCtx.setAuthenticationResult(result);
        profileRequestContext.addSubcontext(authnCtx);
        return this;
    }

    @Nonnull public TestContextBuilder addSessionContext(final IdPSession session) {
        final SessionContext sessionContext = new SessionContext();
        sessionContext.setIdPSession(session);
        profileRequestContext.addSubcontext(sessionContext);
        return this;
    }

    @Nonnull public TestContextBuilder addSubjectContext(final String principal) {
        final SubjectContext subjectContext = new SubjectContext();
        subjectContext.setPrincipalName(principal);
        profileRequestContext.addSubcontext(subjectContext);
        return this;
    }

    @Nonnull public TestContextBuilder addProtocolContext(final Object request, final Object response) {
        final ProtocolContext<Object,Object> context = new ProtocolContext<>();
        context.setRequest(request);
        context.setResponse(response);
        profileRequestContext.addSubcontext(context);
        return this;
    }

    @Nonnull public TestContextBuilder addTicketContext(@Nonnull final Ticket ticket) {
        final ProtocolContext<?,?> context = profileRequestContext.ensureSubcontext(ProtocolContext.class);
        context.addSubcontext(new TicketContext(ticket));
        return this;
    }

    @Nonnull  public TestContextBuilder addRelyingPartyContext(
            final String serviceURL, final boolean verified, final ProfileConfiguration config) {
        final RelyingPartyContext rpc = new RelyingPartyContext();
        rpc.setVerified(verified);
        rpc.setRelyingPartyId(serviceURL);
        rpc.setProfileConfig(config);
        profileRequestContext.addSubcontext(rpc);
        return this;
    }

    @Nonnull public TestContextBuilder addServiceContext(@Nonnull final Service service) {
        final ProtocolContext<?,?> context = profileRequestContext.ensureSubcontext(ProtocolContext.class);
        context.addSubcontext(new ServiceContext(service));
        return this;
    }

    @Nonnull public RequestContext build() {
        final MockRequestContext requestContext = new MockRequestContext();
        final MockExternalContext externalContext = new MockExternalContext();
        externalContext.setNativeRequest(new MockHttpServletRequest());
        externalContext.setNativeResponse(new MockHttpServletResponse());
        requestContext.setExternalContext(externalContext);
        requestContext.getConversationScope().put(ProfileRequestContext.BINDING_KEY, profileRequestContext);
        return requestContext;
    }
}
