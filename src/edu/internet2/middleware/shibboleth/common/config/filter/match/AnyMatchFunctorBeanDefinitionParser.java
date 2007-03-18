/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.config.filter.match;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match.AnyMatchFunctor;

/**
 * Bean definition parser for {@link AnyMatchFunctor} objects.
 */
public class AnyMatchFunctorBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type. */
    public static final QName TYPE_NAME = new QName(BasicMatchFunctorNamespaceHandler.NAMESPACE, "ANY");
    
    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return AnyMatchFunctor.class;
    }
    
    /** {@inheritDoc} */
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        return super.resolveId(element, definition, parserContext);
    }
}