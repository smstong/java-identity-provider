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

package net.shibboleth.idp.saml.saml2.profile.impl;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.OutboundMessageContextLookup;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.criterion.ProtocolCriterion;
import org.opensaml.saml.criterion.RoleDescriptorCriterion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.profile.context.EncryptionContext;
import org.opensaml.xmlsec.EncryptionConfiguration;
import org.opensaml.xmlsec.EncryptionParameters;
import org.opensaml.xmlsec.EncryptionParametersResolver;
import org.opensaml.xmlsec.SecurityConfigurationSupport;
import org.opensaml.xmlsec.criterion.EncryptionConfigurationCriterion;
import org.opensaml.xmlsec.criterion.EncryptionOptionalCriterion;
import org.slf4j.Logger;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.saml.saml2.profile.config.ArtifactResolutionProfileConfiguration;
import net.shibboleth.saml.saml2.profile.config.SAML2AssertionProducingProfileConfiguration;
import net.shibboleth.saml.saml2.profile.config.SAML2ProfileConfiguration;
import net.shibboleth.saml.saml2.profile.config.SingleLogoutProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;

/**
 * Action that resolves and populates {@link EncryptionParameters} on an {@link EncryptionContext}
 * created/accessed via a lookup function, by default on a {@link RelyingPartyContext} child of the
 * profile request context.
 * 
 * <p>The resolution process is contingent on the active profile configuration requesting encryption
 * of some kind, and an {@link EncryptionContext} is also created to capture these requirements.</p>
 * 
 * <p>The OpenSAML default, per-RelyingParty, and default per-profile {@link EncryptionConfiguration}
 * objects are input to the resolution process, along with the relying party's SAML metadata, which in
 * most cases will be the source of the eventual encryption key.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#INVALID_SEC_CFG}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CTX}
 * @event {@link IdPEventIds#INVALID_PROFILE_CONFIG}
 */
public class PopulateEncryptionParameters extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateEncryptionParameters.class);
    
    /** Strategy used to look up a {@link RelyingPartyContext} for configuration options. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;
    
    /** Strategy used to look up the {@link EncryptionContext} to store parameters in. */
    @Nonnull private Function<ProfileRequestContext,EncryptionContext> encryptionContextLookupStrategy;

    /** Strategy used to look up a SAML peer context. */
    @Nullable private Function<ProfileRequestContext,SAMLPeerEntityContext> peerContextLookupStrategy;
    
    /** Metadata protocolSupportEnumeration value to provide to resolver. */
    @Nullable private String samlProtocol;

    /** Metadata role type to provide to resolver. */
    @Nullable private QName peerRole;
    
    /** Strategy used to look up a per-request {@link EncryptionConfiguration} list. */
    @NonnullAfterInit private Function<ProfileRequestContext,List<EncryptionConfiguration>> configurationLookupStrategy;
    
    /** Resolver for parameters to store into context. */
    @NonnullAfterInit private EncryptionParametersResolver encParamsresolver;
    
    /** Active configurations to feed into resolver. */
    @Nullable private List<EncryptionConfiguration> encryptionConfigurations;
    
    /** Is encryption optional in the case no parameters can be resolved? */
    private boolean encryptionOptional;
    
    /** Flag tracking whether assertion encryption is required. */
    private boolean encryptAssertions;

    /** Flag tracking whether assertion encryption is required. */
    private boolean encryptIdentifiers;

    /** Flag tracking whether assertion encryption is required. */
    private boolean encryptAttributes;

    /** Constructor. */
    public PopulateEncryptionParameters() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        
        // Create context by default.
        final Function<ProfileRequestContext,EncryptionContext> ecls =
                new ChildContextLookup<>(EncryptionContext.class, true).compose(
                        new ChildContextLookup<>(RelyingPartyContext.class));
        assert ecls != null;
        encryptionContextLookupStrategy = ecls;

        // Default: outbound msg context -> SAMLPeerEntityContext
        final Function<ProfileRequestContext,SAMLPeerEntityContext> pcls =
                new ChildContextLookup<>(SAMLPeerEntityContext.class).compose(new OutboundMessageContextLookup());
        assert pcls != null;
        peerContextLookupStrategy = pcls;
    }
    
    /**
     * Set the strategy used to return the {@link RelyingPartyContext} for configuration options.
     * 
     * @param strategy lookup strategy
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        checkSetterPreconditions();
        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to look up the {@link EncryptionContext} to set the flags for.
     * 
     * @param strategy lookup strategy
     */
    public void setEncryptionContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,EncryptionContext> strategy) {
        checkSetterPreconditions();
        encryptionContextLookupStrategy = Constraint.isNotNull(strategy,
                "EncryptionContext lookup strategy cannot be null");
    }

    /**
     * Set the protocol constant to use during resolution.
     * 
     * @param protocol the protocol constant to set
     */
    public void setProtocol(@Nullable final String protocol) {
        samlProtocol = StringSupport.trimOrNull(protocol);
    }

    /**
     * Set the operational role to use during resolution.
     * 
     * @param role the operational role to set
     */
    public void setRole(@Nullable final QName role) {
        peerRole = role;
    }
    
    /**
     * Set the strategy used to look up a per-request {@link EncryptionConfiguration} list.
     * 
     * @param strategy lookup strategy
     */
    public void setConfigurationLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,List<EncryptionConfiguration>> strategy) {
        checkSetterPreconditions();
        configurationLookupStrategy = Constraint.isNotNull(strategy,
                "EncryptionConfiguration lookup strategy cannot be null");
    }

    /**
     * Set lookup strategy for {@link SAMLPeerEntityContext} for input to resolution.
     * 
     * @param strategy  lookup strategy
     */
    public void setPeerContextLookupStrategy(
            @Nullable final Function<ProfileRequestContext,SAMLPeerEntityContext> strategy) {
        checkSetterPreconditions();
        peerContextLookupStrategy = strategy;
    }
    
    /**
     * Set the encParamsresolver to use for the parameters to store into the context.
     * 
     * @param newResolver   encParamsresolver to use
     */
    public void setEncryptionParametersResolver(
            @Nonnull final EncryptionParametersResolver newResolver) {
        checkSetterPreconditions();
        encParamsresolver = Constraint.isNotNull(newResolver, "EncryptionParametersResolver cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (encParamsresolver == null) {
            throw new ComponentInitializationException("EncryptionParametersResolver cannot be null");
        } else if (configurationLookupStrategy == null) {
            configurationLookupStrategy = new Function<>() {
                public List<EncryptionConfiguration> apply(final ProfileRequestContext input) {
                    return CollectionSupport.singletonList(
                            SecurityConfigurationSupport.ensureGlobalEncryptionConfiguration());
                }
            };
        }
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        final RelyingPartyContext rpContext = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpContext == null) {
            log.debug("{} Unable to locate RelyingPartyContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        } else if (!(rpContext.getProfileConfig() instanceof SAML2ProfileConfiguration)) {
            log.debug("{} Not a SAML 2 profile configuration, nothing to do", getLogPrefix());
            return false;
        }
                
        final MessageContext imc = profileRequestContext.getInboundMessageContext();  
        Object msg = null;
        if (imc != null) {
            msg = imc.getMessage();
        }
        
        if (msg instanceof AuthnRequest req) {
            final NameIDPolicy policy = req.getNameIDPolicy();
            if (policy != null) {
                final String requestedFormat = policy.getFormat();
                if (requestedFormat != null && NameID.ENCRYPTED.equals(requestedFormat)) {
                    log.debug("{} Request asked for encrypted identifier, disregarding installed predicate");
                    encryptIdentifiers = true;
                }
            }
        } else if (msg != null && rpContext.getProfileConfig() instanceof SingleLogoutProfileConfiguration) {
            log.debug("{} Inbound logout message, nothing to do", getLogPrefix());
            return false;
        }
        
        final SAML2ProfileConfiguration profileConfiguration = (SAML2ProfileConfiguration) rpContext.getProfileConfig();
        assert profileConfiguration!=null;
        if (!encryptIdentifiers) {
            encryptIdentifiers = profileConfiguration.isEncryptNameIDs(profileRequestContext);
            // Encryption can only be optional if the request didn't specify it above.
            encryptionOptional = profileConfiguration.isEncryptionOptional(profileRequestContext);
        }

        if (profileConfiguration instanceof SAML2AssertionProducingProfileConfiguration appc) {
            encryptAssertions = appc.isEncryptAssertions(profileRequestContext);
            encryptAttributes = appc.isEncryptAttributes(profileRequestContext);
        } else if (profileConfiguration instanceof ArtifactResolutionProfileConfiguration arpc) {
            encryptAssertions = arpc.isEncryptAssertions(profileRequestContext);
            encryptAttributes = arpc.isEncryptAttributes(profileRequestContext);
        }
        
        if (!encryptAssertions && !encryptIdentifiers && !encryptAttributes) {
            log.debug("{} No encryption requested, nothing to do", getLogPrefix());
            return false;
        }

        encryptionConfigurations = configurationLookupStrategy.apply(profileRequestContext);
        
        log.debug("{} Encryption for assertions ({}), identifiers ({}), attributes({})", getLogPrefix(),
                encryptAssertions, encryptIdentifiers, encryptAttributes);
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        log.debug("{} Resolving EncryptionParameters for request", getLogPrefix());
        
        final EncryptionContext encryptCtx = encryptionContextLookupStrategy.apply(profileRequestContext);
        if (encryptCtx == null) {
            log.debug("{} No EncryptionContext returned by lookup strategy", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return;
        }
        
        try {
            final List<EncryptionConfiguration> configs = encryptionConfigurations ;
            if (configs == null || configs.isEmpty()) {
                throw new ResolverException("No EncryptionConfigurations returned by lookup strategy");
            }
            
            final EncryptionParameters params =
                    encParamsresolver.resolveSingle(buildCriteriaSet(profileRequestContext, configs));
            log.debug("{} {} EncryptionParameters", getLogPrefix(),
                    params != null ? "Resolved" : "Failed to resolve");
            if (params != null) {
                if (encryptAssertions) {
                    encryptCtx.setAssertionEncryptionParameters(params);
                }
                if (encryptIdentifiers) {
                    encryptCtx.setIdentifierEncryptionParameters(params);
                }
                if (encryptAttributes) {
                    encryptCtx.setAttributeEncryptionParameters(params);
                }
            } else {
                if (encryptionOptional) {
                    log.debug("{} Resolver returned no EncryptionParameters", getLogPrefix());
                    log.debug("{} Encryption is optional, ignoring inability to encrypt", getLogPrefix());
                } else {
                    log.warn("{} Resolver returned no EncryptionParameters", getLogPrefix());
                    ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_SEC_CFG);
                }
            }
        } catch (final ResolverException e) {
            log.error("{} Error resolving EncryptionParameters", getLogPrefix(), e);
            if (encryptionOptional) {
                log.debug("{} Encryption is optional, ignoring inability to encrypt", getLogPrefix());
            } else {
                ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_SEC_CFG);
            }
        }
    }
// Checkstyle: CyclomaticComplexity ON
    
    /**
     * Build the criteria used as input to the {@link EncryptionParametersResolver}.
     * 
     * @param profileRequestContext current profile request context
     * @param configurations the {@link EncryptionConfiguration}s
     * 
     * @return  the criteria set to use
     */
    @Nonnull private CriteriaSet buildCriteriaSet(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final List<EncryptionConfiguration> configurations) {
        
        final CriteriaSet criteria = new CriteriaSet(new EncryptionConfigurationCriterion(configurations));
        
        criteria.add(new EncryptionOptionalCriterion(encryptionOptional));

        if (peerContextLookupStrategy != null) {
            final SAMLPeerEntityContext peerCtx = peerContextLookupStrategy.apply(profileRequestContext);
            if (peerCtx != null) {
                final String peerEntityId = peerCtx.getEntityId(); 
                if (peerEntityId != null) {
                    log.debug("{} Adding entityID to resolution criteria", getLogPrefix());
                    criteria.add(new EntityIdCriterion(peerEntityId));
                    if (samlProtocol != null) {
                        criteria.add(new ProtocolCriterion(samlProtocol));
                    }
                    if (peerRole != null) {
                        criteria.add(new EntityRoleCriterion(peerRole));
                    }
                }
                final SAMLMetadataContext metadataCtx = peerCtx.getSubcontext(SAMLMetadataContext.class);
                final RoleDescriptor roleDescriptor = metadataCtx == null ? null : metadataCtx.getRoleDescriptor();
                if (roleDescriptor != null) {
                    log.debug("{} Adding role metadata to resolution criteria", getLogPrefix());
                    criteria.add(new RoleDescriptorCriterion(roleDescriptor));
                }
            }
        }
        
        return criteria;
    }
    
}