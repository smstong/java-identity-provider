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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoder;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscoderSupport;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.cas.attribute.Attribute;
import net.shibboleth.idp.cas.attribute.transcoding.impl.CASStringAttributeTranscoder;
import net.shibboleth.idp.cas.config.ConfigLookupFunction;
import net.shibboleth.idp.cas.config.ValidateConfiguration;
import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationResponse;
import net.shibboleth.idp.cas.ticket.TicketPrincipalLookupFunction;
import net.shibboleth.idp.cas.ticket.TicketState;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceException;
import net.shibboleth.shared.service.ServiceableComponent;

/**
 * Prepares {@link TicketValidationResponse} for use in CAS protocol response views. Possible outcomes:
 * <ul>
 *     <li><code>null</code> on success</li>
 *     <li>{@link ProtocolError#IllegalState IllegalState}</li>
 * </ul>
 *
 * @author Marvin S. Addison
 */
public class PrepareTicketValidationResponseAction extends
        AbstractCASProtocolAction<TicketValidationRequest, TicketValidationResponse> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PrepareTicketValidationResponseAction.class);

    /** Function used to retrieve AttributeContext. */
    @Nonnull private Function<ProfileRequestContext,AttributeContext> attributeContextFunction;

    /** Function used to retrieve subject principal. */
    @Nonnull private Function<ProfileRequestContext,String> principalLookupFunction;

    /** Profile configuration lookup function. */
    @Nonnull private final ConfigLookupFunction<ValidateConfiguration> configLookupFunction;
    
    /** Transcoder registry service object. */
    @NonnullAfterInit private ReloadableService<AttributeTranscoderRegistry> transcoderRegistry;
    
    /** Fallback rule that does a simple/default encode. */
    @NonnullAfterInit private TranscodingRule defaultTranscodingRule;
    
    /** Stored off context from request. */
    @NonnullBeforeExec private AttributeContext attributeContext;
    
    /** Stored consented attributes from ticket. */
    @Nullable private Set<String> consentedAttributeIds;
    
    /** Profile configuration. */
    @NonnullBeforeExec private ValidateConfiguration validateConfiguration;
    
    /** CAS response. */
    @NonnullBeforeExec private TicketValidationResponse ticketValidationResponse;

    /** Constructor. */
    public PrepareTicketValidationResponseAction() {
        final Function<ProfileRequestContext,AttributeContext> acf =
                new ChildContextLookup<>(AttributeContext.class, true).compose(
                        new ChildContextLookup<>(RelyingPartyContext.class));
        assert acf != null;
        attributeContextFunction = acf;
        principalLookupFunction = new TicketPrincipalLookupFunction();
        configLookupFunction = new ConfigLookupFunction<>(ValidateConfiguration.class);
    }

    /**
     * Sets the registry of transcoding rules to apply to encode attributes.
     * 
     * @param registry registry service interface
     */
    public void setTranscoderRegistry(@Nonnull final ReloadableService<AttributeTranscoderRegistry> registry) {
        checkSetterPreconditions();
        transcoderRegistry = Constraint.isNotNull(registry, "AttributeTranscoderRegistry cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (transcoderRegistry == null) {
            throw new ComponentInitializationException("AttributeTranscoderRegistry cannot be null");
        }
        
        final AttributeTranscoder<?> transcoder = new CASStringAttributeTranscoder();
        transcoder.initialize();
        defaultTranscodingRule = new TranscodingRule(
                CollectionSupport.singletonMap(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder));
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        attributeContext = attributeContextFunction.apply(profileRequestContext);
        if (attributeContext == null) {
            log.warn("{} AttributeContext not found in profile request context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_ATTRIBUTE_CTX);
            return false;
        }
        
        validateConfiguration = configLookupFunction.apply(profileRequestContext);
        if (validateConfiguration == null) {
            log.warn("{} Cannot locate ValidateConfiguration", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        }
        
        try {
            ticketValidationResponse = getCASResponse(profileRequestContext);
            final TicketState state = getCASTicket(profileRequestContext).getTicketState();
            if (state != null) {
                consentedAttributeIds = state.getConsentedAttributeIds();
            }
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
            return false;
        }
        
        return true;
    }    
    
// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final String principal;
        final String userAttributeName = validateConfiguration.getUserAttribute(profileRequestContext);
        if (userAttributeName != null) {
            log.debug("{} Using {} for CAS username", getLogPrefix(), userAttributeName);
            final IdPAttribute attribute = attributeContext.getIdPAttributes().get(userAttributeName);
            if (attribute != null && !attribute.getValues().isEmpty()) {
                final IdPAttributeValue value = attribute.getValues().get(0);
                if (value instanceof ScopedStringAttributeValue) {
                    final ScopedStringAttributeValue scopedValue = (ScopedStringAttributeValue) value;
                    log.warn("{} Lossy use of attribute value {} from attribute {}", getLogPrefix(),
                            scopedValue.getValue(), attribute.getId());
                    principal = scopedValue.getValue();
                } else if (value instanceof StringAttributeValue) {
                    principal = ((StringAttributeValue) value).getValue();
                } else {
                    log.warn("{} Use of attribute value type {} from attribute {}", getLogPrefix(),
                            value.getClass(), attribute.getId());
                    principal = value.getNativeValue().toString();
                }
            } else {
                log.debug("{} Filtered attribute {} has no value", getLogPrefix(), userAttributeName);
                principal = null;
            }
        } else {
            principal = principalLookupFunction.apply(profileRequestContext);
        }

        if (principal == null) {
            throw new IllegalStateException("Principal cannot be null");
        }

        ticketValidationResponse.setUserName(principal);
        
        final Collection<IdPAttribute> inputAttributes = attributeContext.getIdPAttributes().values();
        final ArrayList<Attribute> encodedAttributes = new ArrayList<>(inputAttributes.size());

        try (final ServiceableComponent<AttributeTranscoderRegistry> component =
                    transcoderRegistry.getServiceableComponent()) {
            for (final IdPAttribute attribute : inputAttributes) {
                assert attribute != null;
                final Set<String> ids = consentedAttributeIds;
                if (ids == null || ids.contains(attribute.getId())) {
                    encodeAttribute(component.getComponent(), profileRequestContext, attribute, encodedAttributes);
                } else {
                    log.info("{} Skipping attribute {} not in stored consent list from ticket", getLogPrefix(),
                            attribute.getId());
                }
            }
        } catch (final ServiceException e) {
            log.error("{} Attribute transoding service unavailable", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.UNABLE_ENCODE_ATTRIBUTE);
            return;
        }
        
        encodedAttributes.forEach(a -> {
            assert a!=null; ticketValidationResponse.addAttribute(a);
            }
        );
    }
// Checkstyle: CyclomaticComplexity ON

    /**
     * Access the registry of transcoding rules to transform the input attribute into a target type.
     * 
     * @param registry  registry of transcoding rules
     * @param profileRequestContext current profile request context
     * @param attribute input attribute
     * @param results collection to add results to
     * 
     * @return number of results added
     */
    protected int encodeAttribute(@Nonnull final AttributeTranscoderRegistry registry,
            @Nonnull final ProfileRequestContext profileRequestContext, @Nonnull final IdPAttribute attribute,
            @Nonnull @Live final Collection<Attribute> results) {
        
        Collection<TranscodingRule> transcodingRules = registry.getTranscodingRules(attribute, Attribute.class);
        if (transcodingRules.isEmpty()) {
            log.debug("{} Attribute {} does not have any transcoding rules, applying default", getLogPrefix(),
                    attribute.getId());
            assert defaultTranscodingRule != null;
            transcodingRules = CollectionSupport.singletonList(defaultTranscodingRule);
        }
        
        int count = 0;
        
        for (final TranscodingRule rules : transcodingRules) {
            assert rules != null;
            try {
                final AttributeTranscoder<Attribute> transcoder = TranscoderSupport.<Attribute>getTranscoder(rules);
                final Attribute encodedAttribute =
                        transcoder.encode(profileRequestContext, attribute, Attribute.class, rules);
                if (encodedAttribute != null) {
                    results.add(encodedAttribute);
                    count++;
                }
            } catch (final AttributeEncodingException e) {
                log.debug("{} Unable to encode attribute {}", getLogPrefix(), attribute.getId(), e);
            }
        }
        
        return count;
    }
    
}