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

package net.shibboleth.idp.attribute.resolver.spring.ad;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.script.ScriptException;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.impl.ad.ScriptedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.AttributeResolverNamespaceHandler;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

// TODO incomplete port from v2
/**
 * Spring bean definition parser for scripted attribute configuration elements.
 */
public class ScriptedAttributeDefinitionBeanDefinitionParser extends BaseAttributeDefinitionBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "Script");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ScriptedAttributeDefinitionBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return ScriptedAttributeDefinition.class;
    }
    
    /** Query the DOM and get the script from the appropriate subelements.
     * @param config The DOM we are interested in
     * @return The script as a string or throws an {@link BeanCreationException}
     */
    @Nonnull private String getScript(Element config) {
        String script = null;
        List<Element> scriptElem =
                ElementSupport.getChildElements(config,
                        new QName(AttributeResolverNamespaceHandler.NAMESPACE, "Script"));
        List<Element> scriptFileElem =
                ElementSupport.getChildElements(config, new QName(AttributeResolverNamespaceHandler.NAMESPACE,
                        "ScriptFile"));
        if (scriptElem != null && scriptElem.size() > 0) {
            if (scriptFileElem != null && scriptFileElem.size() > 0) {
                log.info("Attribute definition {}: definition contains both <Script> "
                        + "and <ScriptFile> elements, taking the <Script> element", getDefinitionId());
            }
            script = scriptElem.get(0).getTextContent();
        } else {
            if (scriptFileElem != null && scriptFileElem.size() > 0) {
                String scriptFile = scriptFileElem.get(0).getTextContent();
                try {
                    script = StringSupport.inputStreamToString(new FileInputStream(scriptFile), null);
                } catch (IOException e) {
                    throw new BeanCreationException("Attribute definition " + getDefinitionId()
                            + ": Unable to read script file " + scriptFile, e);
                }
            }
        }

        if (script == null) {
            throw new BeanCreationException("No script specified for this attribute definition");
        }
        return script;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        String scriptLanguage = "javascript";
        if (config.hasAttributeNS(null, "language")) {
            scriptLanguage = config.getAttributeNS(null, "language");
        }
        log.debug("Attribute definition {}: scripting language: {}", getDefinitionId(), scriptLanguage);

        String script = getScript(config);
        log.debug("Attribute definition {} script: {}", getDefinitionId(), script);
        try {
            builder.addPropertyValue("script", new EvaluableScript(scriptLanguage, script));
        } catch (ScriptException e) {
            log.error("Attribute definition {}, could not create the EvaluableScript : {} ", new Object[] {
                    getDefinitionId(), e,});
            throw new BeanCreationException("Could not create the EvaluableScript");
        }
    }
}