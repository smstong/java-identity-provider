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

package net.shibboleth.idp.saml.profile.impl;

import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.messaging.context.SAMLSubjectNameIdentifierContext;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml2.core.NameID;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.saml.authn.principal.NameIDPrincipal;
import net.shibboleth.idp.saml.authn.principal.NameIdentifierPrincipal;
import net.shibboleth.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.profile.context.navigate.IssuerLookupFunction;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;

/**
 * Action that extracts a SAML Subject from an inbound message, and prepares a
 * {@link SubjectCanonicalizationContext} to process it into a principal identity.
 * 
 * <p>If the inbound message does not supply a {@link NameIdentifier} or {@link NameID} to
 * process, then nothing is done, and the local event ID {@link #NO_SUBJECT} is signaled.</p>
 * 
 * <p>A policy predicate may also be executed to control the conditions under which a subject
 * name may be used by a requester, possibly resulting in a {@link AuthnEventIds#INVALID_SUBJECT}
 * event.</p>
 * 
 * <p>Otherwise, a custom {@link java.security.Principal} of the appropriate type is wrapped around the
 * identifier object and a Java {@link Subject} is prepared for canonicalization.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#INVALID_SUBJECT}
 * @event {@link #NO_SUBJECT}
 * 
 * @post If "proceed" signaled, then ProfileRequestContext.getSubcontext(SubjectCanonicalizationContext.class) != null
 */
public class ExtractSubjectFromRequest extends AbstractProfileAction {

    /** Local event signaling that canonicalization is unnecessary. */
    @Nonnull @NotEmpty public static final String NO_SUBJECT = "NoSubject";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ExtractSubjectFromRequest.class);
    
    /** Predicate to validate use of {@link NameID} or {@link NameIdentifier} in subject. */
    @Nullable private Predicate<ProfileRequestContext> nameIDPolicyPredicate;
    
    /** Function used to obtain the requester ID. */
    @Nullable private Function<ProfileRequestContext,String> requesterLookupStrategy;

    /** Function used to obtain the responder ID. */
    @Nullable private Function<ProfileRequestContext,String> responderLookupStrategy;
    
    /** SAML 1 or 2 identifier object to wrap for c14n. */
    @NonnullBeforeExec private SAMLObject nameIdentifier;
    
    /** Constructor.
     * 
     * @throws ComponentInitializationException if unable to initialize default objects
     */
    public ExtractSubjectFromRequest() throws ComponentInitializationException {
        requesterLookupStrategy = new RelyingPartyIdLookupFunction();
        responderLookupStrategy = new IssuerLookupFunction();
    }
    
    /**
     * Set the strategy used to locate the requester ID for canonicalization.
     * 
     * @param strategy lookup strategy
     */
    public void setRequesterLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        requesterLookupStrategy = strategy;
    }

    /**
     * Set the strategy used to locate the responder ID for canonicalization.
     * 
     * @param strategy lookup strategy
     */
    public void setResponderLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        responderLookupStrategy = strategy;
    }
    
    /**
     * Set a predicate used to validate use of the {@link NameID} or {@link NameIdentifier} in the subject.
     * 
     * @param predicate predicate to use
     */
    public void setNameIDPolicyPredicate(@Nullable final Predicate<ProfileRequestContext> predicate) {
        checkSetterPreconditions();
        nameIDPolicyPredicate = predicate;
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        final MessageContext msgCtx = profileRequestContext.getInboundMessageContext();
        if (msgCtx == null || msgCtx.getMessage() == null) {
            log.debug("{} No inbound message", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, NO_SUBJECT);
            return false;
        }

        nameIdentifier = msgCtx.ensureSubcontext(SAMLSubjectNameIdentifierContext.class).getSubjectNameIdentifier();
        if (nameIdentifier == null) {
            log.debug("{} No Subject NameID/NameIdentifier in message needs inbound processing", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, NO_SUBJECT);
            return false;
        }
        
        if (nameIDPolicyPredicate != null && !nameIDPolicyPredicate.test(profileRequestContext)) {
            log.warn("{} Consumption of NameID/NameIdentifier blocked by policy", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
            return false;
        }
        
        return super.doPreExecute(profileRequestContext);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final Subject subject;
        final SAMLObject identifier = nameIdentifier;
        if (identifier instanceof NameIdentifier) {
            log.debug("{} Creating Subject for canonicalization around NameIdentifier {}", getLogPrefix(),
                    ((NameIdentifier) identifier).getValue());
            subject = new Subject(false,
                    CollectionSupport.singleton(new NameIdentifierPrincipal((NameIdentifier) identifier)),
                    CollectionSupport.emptySet(), CollectionSupport.emptySet());
        } else if (identifier instanceof NameID) {
            log.debug("{} Creating Subject for canonicalization around NameID {}", getLogPrefix(),
                    ((NameID) identifier).getValue());
            subject = new Subject(false,
                    CollectionSupport.singleton(new NameIDPrincipal((NameID) identifier)),
                    CollectionSupport.emptySet(), CollectionSupport.emptySet());
        } else {
            subject = null;
        }
        
        if (subject == null) {
            log.debug("{} Identifier was not of a supported type, ignoring", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, NO_SUBJECT);
            return;
        }
        
        final SubjectCanonicalizationContext c14n = new SubjectCanonicalizationContext();
        c14n.setSubject(subject);
        if (requesterLookupStrategy != null) {
            c14n.setRequesterId(requesterLookupStrategy.apply(profileRequestContext));
        }
        if (responderLookupStrategy != null) {
            c14n.setResponderId(responderLookupStrategy.apply(profileRequestContext));
        }
        
        profileRequestContext.addSubcontext(c14n, true);
        log.debug("{} Created subject canonicalization context", getLogPrefix());
    }
    

    /**
     * Lookup function that returns the {@link NameIdentifier} or {@link NameID} from the request in the inbound
     * message context.
     */
    public static class SubjectNameLookupFunction implements Function<ProfileRequestContext,SAMLObject> {
        
        /** {@inheritDoc} */
        @Nullable public SAMLObject apply(@Nullable final ProfileRequestContext profileRequestContext) {
            
            if (profileRequestContext != null) {
                final MessageContext msgCtx = profileRequestContext.getInboundMessageContext();
                if (msgCtx != null) {
                    return msgCtx.ensureSubcontext(SAMLSubjectNameIdentifierContext.class)
                            .getSubjectNameIdentifier();
                }
            }
            
            return null;
        }
    }

}