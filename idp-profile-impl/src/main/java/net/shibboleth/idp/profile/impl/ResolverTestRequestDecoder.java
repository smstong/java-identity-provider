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

package net.shibboleth.idp.profile.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.decoder.servlet.AbstractHttpServletRequestMessageDecoder;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLProtocolContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Decodes an incoming resolver test message.
 */
public class ResolverTestRequestDecoder extends AbstractHttpServletRequestMessageDecoder {

    /** Name of the query parameter carrying the subject name: {@value} . */
    @Nonnull @NotEmpty public static final String PRINCIPAL_PARAM = "principal";

    /** Name of the query parameter carrying the requesterID: {@value} . */
    @Nonnull @NotEmpty public static final String REQUESTER_ID_PARAM = "requester";

    /** Name of the query parameter carrying the ACS index: {@value} . */
    @Nonnull @NotEmpty public static final String ACS_INDEX_PARAM = "acsIndex";

    /** Name of the query parameter carrying the protocol: {@value} . */
    @Nonnull @NotEmpty public static final String PROTOCOL_PARAM = "protocol";

    /** Name of the query parameter for the SAML 1 protocol: {@value} . */
    @Nonnull @NotEmpty public static final String SAML1_PARAM = "saml1";

    /** Name of the query parameter for the SAML 2 protocol: {@value} . */
    @Nonnull @NotEmpty public static final String SAML2_PARAM = "saml2";

    /**
     * Name of the query parameter for the unfiltered option: {@value} .
     * 
     * @since 5.1.0
     */
    @Nonnull @NotEmpty public static final String UNFILTERED_PARAM = "unfiltered";

    /** Constructor. */
    public ResolverTestRequestDecoder() {
        setProtocolMessageLoggerSubCategory("ADMIN");
    }

    /** {@inheritDoc} */
    @Override
    protected void doDecode() throws MessageDecodingException {
        final HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            throw new MessageDecodingException("Unable to locate HttpServletRequest");
        }
        
        final ResolverTestRequest message = new ResolverTestRequest(getPrincipal(request), getRequesterId(request),
                getIndex(request), getProtocol(request), getUnfiltered(request));
        final MessageContext messageContext = new MessageContext();
        messageContext.setMessage(message);
        setMessageContext(messageContext);
        
        final SAMLPeerEntityContext peerCtx = new SAMLPeerEntityContext();
        peerCtx.setRole(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        peerCtx.setEntityId(message.getRequesterId());
        messageContext.addSubcontext(peerCtx, true);
        
        if (message.getProtocol() != null) {
            messageContext.ensureSubcontext(SAMLProtocolContext.class).setProtocol(message.getProtocol());
        }
    }

    /**
     * Get the name of the subject.
     * 
     * @param request current HTTP request
     * 
     * @return the name of the subject
     * 
     * @throws MessageDecodingException thrown if the request does not contain a subject name
     */
    @Nonnull @NotEmpty protected String getPrincipal(@Nonnull final HttpServletRequest request)
            throws MessageDecodingException {
        final String name = StringSupport.trimOrNull(request.getParameter(PRINCIPAL_PARAM));
        if (name == null) {
            throw new MessageDecodingException("Request did not contain the " + PRINCIPAL_PARAM + " query parameter.");
        }
        return name;
    }

    /**
     * Get the ID of the requester.
     * 
     * @param request current HTTP request
     * 
     * @return the ID of the requester
     * 
     * @throws MessageDecodingException thrown if the request does not contain a requester name
     */
    @Nonnull @NotEmpty protected String getRequesterId(@Nonnull final HttpServletRequest request)
            throws MessageDecodingException {
        final String name = StringSupport.trimOrNull(request.getParameter(REQUESTER_ID_PARAM));
        if (name == null) {
            throw new MessageDecodingException("Request did not contain the " + REQUESTER_ID_PARAM
                    + " query parameter.");
        }
        return name;
    }

    /**
     * Get the ACS index.
     * 
     * @param request current HTTP request
     * 
     * @return the ACS index, or null
     */
    @Nullable protected Integer getIndex(@Nonnull final HttpServletRequest request) {
        final String index = StringSupport.trimOrNull(request.getParameter(ACS_INDEX_PARAM));
        if (index != null) {
            return Integer.valueOf(index);
        }
        
        return null;
    }

    /**
     * Get the protocol.
     * 
     * @param request current HTTP request
     * 
     * @return the protocol, or null
     */
    @Nullable protected String getProtocol(@Nonnull final HttpServletRequest request) {
        final String protocol = StringSupport.trimOrNull(request.getParameter(PROTOCOL_PARAM));
        if (protocol != null) {
            return protocol;
        }
        
        if (request.getParameter(SAML1_PARAM) != null) {
            return SAMLConstants.SAML11P_NS;
        } else if (request.getParameter(SAML2_PARAM) != null) {
            return SAMLConstants.SAML20P_NS;
        }
        
        return null;
    }
    
    /**
     * Get the unfiltered flag.
     * 
     * @param request current HTTP request
     * 
     * @return the unfiltered flag, or null
     * 
     * @since 5.1.0
     */
    @Nullable protected Boolean getUnfiltered(@Nonnull final HttpServletRequest request) {
        return request.getParameter(UNFILTERED_PARAM) != null;
    }

}