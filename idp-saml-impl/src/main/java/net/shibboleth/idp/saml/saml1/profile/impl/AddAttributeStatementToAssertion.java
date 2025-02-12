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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.Attribute;
import org.opensaml.saml.saml1.core.AttributeStatement;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.saml.saml1.profile.SAML1ActionSupport;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.saml.profile.impl.BaseAddAttributeStatementToAssertion;
import net.shibboleth.shared.annotation.constraint.NullableElements;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.service.ServiceException;
import net.shibboleth.shared.service.ServiceableComponent;

/**
 * Action that builds an {@link AttributeStatement} and adds it to an {@link Assertion} returned by a lookup
 * strategy, by default in the {@link ProfileRequestContext#getOutboundMessageContext()}.
 * 
 * <p>If no {@link Response} exists, then an {@link Assertion} directly in the outbound message context will
 * be used or created</p>

 * <p>The {@link IdPAttribute} set to be encoded is drawn from an
 * {@link net.shibboleth.idp.attribute.context.AttributeContext} returned from a lookup strategy, by default located
 * on the {@link net.shibboleth.profile.context.RelyingPartyContext} beneath the profile request context.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link IdPEventIds#UNABLE_ENCODE_ATTRIBUTE}
 */
public class AddAttributeStatementToAssertion extends BaseAddAttributeStatementToAssertion<Attribute> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AddAttributeStatementToAssertion.class);
    
    /** Strategy used to locate the {@link Assertion} to operate on. */
    @Nonnull private Function<ProfileRequestContext,Assertion> assertionLookupStrategy;
    
    /** Constructor. */
    public AddAttributeStatementToAssertion() {
        assertionLookupStrategy = new AssertionStrategy();
    }

    /**
     * Set the strategy used to locate the {@link Assertion} to operate on.
     * 
     * @param strategy strategy used to locate the {@link Assertion} to operate on
     */
    public void setAssertionLookupStrategy(@Nonnull final Function<ProfileRequestContext,Assertion> strategy) {
        checkSetterPreconditions();
        assertionLookupStrategy = Constraint.isNotNull(strategy, "Assertion lookup strategy cannot be null");
    }

//CheckStyle: ReturnCount OFF
    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        try {
            final AttributeStatement statement = buildAttributeStatement(profileRequestContext,
                    getAttributeContext().getIdPAttributes().values());
            if (statement == null) {
                log.debug("{} No AttributeStatement was built, nothing to do", getLogPrefix());
                return;
            }

            final Assertion assertion = assertionLookupStrategy.apply(profileRequestContext);
            if (assertion == null) {
                log.error("Unable to obtain Assertion to modify");
                ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
                return;
            }
                        
            assertion.getAttributeStatements().add(statement);

            log.debug("{} Adding constructed AttributeStatement to Assertion {} ", getLogPrefix(), assertion.getID());
        } catch (final AttributeEncodingException e) {
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.UNABLE_ENCODE_ATTRIBUTE);
        }
    }
//CheckStyle: ReturnCount ON

// CheckStyle: CyclomaticComplexity OFF    
    /**
     * Builds an attribute statement from a collection of attributes.
     * 
     * @param profileRequestContext current profile request context
     * @param attributes the collection of attributes
     * 
     * @return the attribute statement or null if no attributes can be encoded
     * @throws AttributeEncodingException thrown if there is a problem encoding an attribute
     */
    @Nullable private AttributeStatement buildAttributeStatement(
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nullable @NullableElements final Collection<IdPAttribute> attributes) throws AttributeEncodingException {
        if (attributes == null || attributes.isEmpty()) {
            log.debug("{} No attributes available to be encoded, nothing to do", getLogPrefix());
            return null;
        }

        final ArrayList<Attribute> encodedAttributes = new ArrayList<>(attributes.size());

        try (final ServiceableComponent<AttributeTranscoderRegistry> component =
                getTranscoderRegistry().getServiceableComponent()) {
            for (final IdPAttribute attribute : attributes) {
                if (attribute != null && !attribute.getValues().isEmpty()) {
                    encodeAttribute(component.getComponent(), profileRequestContext, attribute, encodedAttributes);
                }
            }
        } catch (final ServiceException e) {
            throw new AttributeEncodingException("Attribute transoding service unavailable", e);
        }

        if (encodedAttributes.isEmpty()) {
            log.debug("{} No attributes were encoded as SAML 1 Attributes, nothing to do", getLogPrefix());
            return null;
        }

        final SAMLObjectBuilder<AttributeStatement> statementBuilder = (SAMLObjectBuilder<AttributeStatement>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<AttributeStatement>ensureBuilder(
                        AttributeStatement.DEFAULT_ELEMENT_NAME);

        final AttributeStatement statement = statementBuilder.buildObject();
        
        for (final Attribute attribute : encodedAttributes) {
            assert attribute != null;
            final Attribute existing = findExistingAttribute(statement, attribute);
            if (existing != null) {
                final Iterator<XMLObject> newValues = attribute.getAttributeValues().iterator();
                while (newValues.hasNext()) {
                    final XMLObject newValue = newValues.next();
                    newValues.remove();
                    existing.getAttributeValues().add(newValue);                    
                }
            } else {
                statement.getAttributes().add(attribute);
            }
        }

        return statement;
    }
// CheckStyle: CyclomaticComplexity ON
    
    /**
     * Find a matching {@link Attribute} in the statement, if any.
     * 
     * @param statement input statement
     * @param newAttribute the attribute to match
     * 
     * @return a match, or null
     */
    @Nullable private Attribute findExistingAttribute(@Nonnull final AttributeStatement statement,
            @Nonnull final Attribute newAttribute) {
        
        for (final Attribute attr : statement.getAttributes()) {
            if (Objects.equals(attr.getAttributeName(), newAttribute.getAttributeName())
                    && Objects.equals(attr.getAttributeNamespace(), newAttribute.getAttributeNamespace())) {
                return attr;
            }
        }
        
        return null;
    }

    /**
     * Encodes {@link IdPAttribute} into zero or more {@link Attribute} objects if a proper encoder is available.
     * 
     * @param registry transcoding registry
     * @param profileRequestContext current profile request context
     * @param attribute the attribute to be encoded
     * @param results collection to add the encoded SAML attributes to
     * 
     * @throws AttributeEncodingException thrown if there is a problem encoding an attribute
     */
    private void encodeAttribute(@Nonnull final AttributeTranscoderRegistry registry,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final IdPAttribute attribute, @Nonnull final Collection<Attribute> results)
                    throws AttributeEncodingException {

        log.debug("{} Attempting to encode attribute {} as a SAML 1 Attribute", getLogPrefix(), attribute.getId());

        // Uses the new registry.
        if (super.encodeAttribute(registry, profileRequestContext, attribute, Attribute.class, results) == 0) {
            log.debug("{} Attribute {} did not have usable SAML 1 Attribute transcoder rules", getLogPrefix(),
                    attribute.getId());
        }
    }
    
    /**
     * Default strategy for obtaining assertion to modify.
     * 
     * <p>If the outbound context is empty, a new assertion is created and stored there. If the outbound
     * message is already an assertion, it's returned. If the outbound message is a response, then either
     * an existing or new assertion in the response is returned, depending on the action setting. If the
     * outbound message is anything else, null is returned.</p>
     */
    private class AssertionStrategy implements Function<ProfileRequestContext,Assertion> {

        /** {@inheritDoc} */
        @Nullable public Assertion apply(@Nullable final ProfileRequestContext input) {
            final MessageContext omc = input == null ? null : input.getOutboundMessageContext(); 

            if (input != null && omc != null) {
                final Object outboundMessage = omc.getMessage();
                if (outboundMessage == null) {

                    final Assertion ret = SAML1ActionSupport.buildAssertion(AddAttributeStatementToAssertion.this,
                            getIdGenerator(), getIssuerId());
                    omc.setMessage(ret);
                    return ret;
                } else if (outboundMessage instanceof Assertion) {
                    return (Assertion) outboundMessage;
                } else if (outboundMessage instanceof Response) {
                    if (isStatementInOwnAssertion() || ((Response) outboundMessage).getAssertions().isEmpty()) {
                        return SAML1ActionSupport.addAssertionToResponse(AddAttributeStatementToAssertion.this,
                                (Response) outboundMessage, getIdGenerator(), getIssuerId());
                    }
                    return ((Response) outboundMessage).getAssertions().get(0); 
                }
            }
            
            return null;
        }
        
    }

}