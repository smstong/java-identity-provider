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

package net.shibboleth.idp.test.flows.mapper;

import javax.annotation.Nonnull;

import net.shibboleth.idp.profile.AbstractProfileAction;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLProtocolContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;

@SuppressWarnings("javadoc")
public class SpoofSAMLMessage extends AbstractProfileAction {
    
    @Override
    protected void doExecute(
            @Nonnull final ProfileRequestContext profileRequestContext) {

        final MessageContext mc = new MessageContext();
        profileRequestContext.setInboundMessageContext(mc);
        SAMLPeerEntityContext spec = mc.ensureSubcontext(SAMLPeerEntityContext.class);
        spec.setEntityId("https://sp.example.org");
        spec.setRole(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        mc.ensureSubcontext(SAMLProtocolContext.class).setProtocol(SAMLConstants.SAML20P_NS);
                
    }
    
}