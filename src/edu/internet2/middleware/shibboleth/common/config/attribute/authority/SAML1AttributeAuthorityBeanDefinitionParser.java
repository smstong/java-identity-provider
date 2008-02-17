/*
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.config.attribute.authority;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethSAML1AttributeAuthority;
import edu.internet2.middleware.shibboleth.common.config.service.AbstractServiceBeanDefinitionParser;

/** SAML 1 attribute authority bean definition parsers. */
public class SAML1AttributeAuthorityBeanDefinitionParser extends AbstractServiceBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeAuthorityNamespaceHandler.NAMESPACE,
            "SAML1AttributeAuthority");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return ShibbolethSAML1AttributeAuthority.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        builder.addConstructorArgReference(element.getAttributeNS(null, "resolver"));

        if (element.hasAttributeNS(null, "filter")) {
            builder.addPropertyReference("filteringEngine", element.getAttributeNS(null, "filter"));
        }
    }
}