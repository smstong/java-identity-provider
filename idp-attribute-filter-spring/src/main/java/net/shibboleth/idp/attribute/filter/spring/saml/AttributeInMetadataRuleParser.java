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

package net.shibboleth.idp.attribute.filter.spring.saml;

// TODO TEST
import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.filter.policyrule.saml.impl.AttributeInMetadataPolicyRule;
import net.shibboleth.idp.attribute.filter.spring.matcher.BaseAttributeValueMatcherParser;
import net.shibboleth.idp.saml.profile.config.navigate.AttributeConsumerServiceLookupFunction;
import net.shibboleth.idp.saml.profile.config.navigate.EntityDescriptorLookupFunction;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.saml.common.messaging.context.AttributeConsumingServiceContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.google.common.base.Functions;

/**
 * Bean definition parser for {@link AttributeInMetadataPolicyRule}.
 */
public class AttributeInMetadataRuleParser extends BaseAttributeValueMatcherParser {

    /** Schema type. */
    public static final QName ATTRIBUTE_IN_METADATA = new QName(AttributeFilterSAMLNamespaceHandler.NAMESPACE,
            "AttributeInMetadata");

    /** Schema type. */
    public static final QName ENTITY_ATTRIBUTE_IN_METADATA = new QName(AttributeFilterSAMLNamespaceHandler.NAMESPACE,
            "EntityAttributeInMetadata");

    /** {@inheritDoc} */
    @Override @Nonnull protected Class<AttributeInMetadataPolicyRule> getNativeBeanClass() {
        return AttributeInMetadataPolicyRule.class;
    }

    /** {@inheritDoc} */
    @Override protected void doNativeParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, builder);

        if (config.hasAttributeNS(null, "onlyIfRequired")) {
            builder.addPropertyValue("onlyIfRequired", config.getAttributeNodeNS(null, "onlyIfRequired"));
        }

        if (config.hasAttributeNS(null, "matchIfMetadataSilent")) {
            builder.addPropertyValue("matchIfMetadataSilent ", 
                    config.getAttributeNodeNS(null, "matchIfMetadataSilent"));
        }

        if (ElementSupport.isElementNamed(config, ENTITY_ATTRIBUTE_IN_METADATA)) {
            builder.addPropertyValue("objectStrategy", new EntityDescriptorLookupFunction());
        } else {
            builder.addPropertyValue("objectStrategy", Functions.compose(new AttributeConsumerServiceLookupFunction(),
                    new ChildContextLookup<SAMLMetadataContext, AttributeConsumingServiceContext>(
                            AttributeConsumingServiceContext.class)));
        }
    }
}
