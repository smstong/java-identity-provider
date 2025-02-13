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

package net.shibboleth.idp.saml.saml1.profile.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.saml.profile.impl.BaseIdPInitiatedSSORequestMessageDecoder;
import net.shibboleth.idp.saml.profile.impl.IdPInitiatedSSORequest;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.messaging.context.SAMLMessageInfoContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;

/** Decodes an incoming Shibboleth Authentication Request message. */
public class IdPInitiatedSSORequestMessageDecoder extends BaseIdPInitiatedSSORequestMessageDecoder {
    
    /** Protocol binding implemented by this decoder. */
    @Nonnull @NotEmpty private static final String BINDING_URI = "urn:mace:shibboleth:1.0:profiles:AuthnRequest";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(IdPInitiatedSSORequestMessageDecoder.class);
    
    /**
     * Get the SAML binding URI supported by this decoder.
     * 
     * @return SAML binding URI supported by this decoder
     */
    @Nonnull @NotEmpty public String getBindingURI() {
        return BINDING_URI;
    }

    /** {@inheritDoc} */
    @Override
    protected void doDecode() throws MessageDecodingException {
        final IdPInitiatedSSORequest ssoRequest = buildIdPInitiatedSSORequest();

        final MessageContext messageContext = new MessageContext();
        messageContext.setMessage(ssoRequest);
        
        messageContext.ensureSubcontext(SAMLPeerEntityContext.class).setEntityId(ssoRequest.getEntityId());
        
        final SAMLMessageInfoContext msgInfoContext = messageContext.ensureSubcontext(SAMLMessageInfoContext.class);
        msgInfoContext.setMessageIssueInstant(ssoRequest.getTime());
        msgInfoContext.setMessageId(getMessageID());
        
        populateBindingContext(messageContext);

        setMessageContext(messageContext);
    }
    
    /**
     * Populate the context which carries information specific to this binding.
     * 
     * @param messageContext the current message context
     * 
     * @throws MessageDecodingException if the message content is invalid
     */
    protected void populateBindingContext(@Nonnull final MessageContext messageContext)
        throws MessageDecodingException {
        final  IdPInitiatedSSORequest message =
                Constraint.isNotNull((IdPInitiatedSSORequest) messageContext.getMessage(), "No message");
        final String relayState = message.getRelayState();
        if (relayState == null) {
            throw new MessageDecodingException("Legacy Shibboleth authentication requests require a target parameter");
        }
        log.debug("Decoded SAML relay state: {}", relayState);
        
        final SAMLBindingContext bindingContext = messageContext.ensureSubcontext(SAMLBindingContext.class);
        bindingContext.setRelayState(relayState);
        bindingContext.setBindingUri(getBindingURI());
        bindingContext.setBindingDescriptor(getBindingDescriptor());
        bindingContext.setHasBindingSignature(false);
        bindingContext.setIntendedDestinationEndpointURIRequired(false);
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected String serializeMessageForLogging(@Nullable final Object message) {
        if (message == null) {
            log.debug("Message was null, can not serialize");
            return null;
        }
        return "SAML 1 IdP-initiated request was: " + message.toString();
    }
    
}