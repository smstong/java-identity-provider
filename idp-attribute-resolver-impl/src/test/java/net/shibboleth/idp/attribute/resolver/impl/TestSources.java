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

package net.shibboleth.idp.attribute.resolver.impl;

import java.util.Set;
import java.util.regex.Pattern;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;
import net.shibboleth.idp.attribute.resolver.StaticAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.StaticDataConnector;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/** Basic data sources for testing the attribute generators. */
public final class TestSources {
    /** The name we use in this test for the static connector. */
    public static final String STATIC_CONNECTOR_NAME = "staticCon";

    /** The name we use in this test for the static attribute. */
    public static final String STATIC_ATTRIBUTE_NAME = "staticAtt";

    /** The name of the attribute we use as source. */
    public static final String DEPENDS_ON_ATTRIBUTE_NAME_ATTR = "at1";
    public static final String DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR = "ac1";
    
    /** The name of another attribute we use as source. */
    public static final String DEPENDS_ON_SECOND_ATTRIBUTE_NAME = "at2";

    /** Another attributes values. */
    public static final String[] SECOND_ATTRIBUTE_VALUE_STRINGS = {"at2-Val1", "at2-Val2"};
    public static final StringAttributeValue[] SECOND_ATTRIBUTE_VALUE_RESULTS = {new StringAttributeValue(SECOND_ATTRIBUTE_VALUE_STRINGS[0]),new StringAttributeValue(SECOND_ATTRIBUTE_VALUE_STRINGS[0]),};

    /** A value from both providers. */
    public static final String COMMON_ATTRIBUTE_VALUE_STRING = "at1-Data";
    public static final StringAttributeValue COMMON_ATTRIBUTE_VALUE_RESULT = new StringAttributeValue(COMMON_ATTRIBUTE_VALUE_STRING);

    /** A value from the connector. */
    public static final String CONNECTOR_ATTRIBUTE_VALUE_STRING = "at1-Connector";
    public static final StringAttributeValue CONNECTOR_ATTRIBUTE_VALUE_RESULT = new StringAttributeValue(CONNECTOR_ATTRIBUTE_VALUE_STRING);

    /** A value from the attribute. */
    public static final String ATTRIBUTE_ATTRIBUTE_VALUE_STRING = "at1-Attribute";
    public static final StringAttributeValue ATTRIBUTE_ATTRIBUTE_VALUE_RESULT = new StringAttributeValue(ATTRIBUTE_ATTRIBUTE_VALUE_STRING);

    /** Regexp. for CONNECTOR_ATTRIBUTE_VALUE (for map & regexp testing). */
    
    public static final String CONNECTOR_ATTRIBUTE_VALUE_REGEXP = "at1-(.+)or";
    public static final Pattern CONNECTOR_ATTRIBUTE_VALUE_REGEXP_PATTERN = Pattern.compile(CONNECTOR_ATTRIBUTE_VALUE_REGEXP);
    public static final StringAttributeValue CONNECTOR_ATTRIBUTE_VALUE_REGEXP_RESULT = new StringAttributeValue("Connect");
    
    /** Principal name for Principal method tests */
    public static final String TEST_PRINCIPAL = "PrincipalName";

    /** Relying party name for Principal method tests */
    public static final String TEST_RELYING_PARTY = "RP1";

    /** Authentication method for Principal method tests */
    public static final String TEST_AUTHN_METHOD = "AuthNmEthod";


    /** Constructor. */
    private TestSources() {
    }

    /**
     * Create a static connector with known attributes and values.
     * 
     * @return The connector
     * @throws ComponentInitializationException if we cannot initialized (unlikely)
     */
    public static BaseDataConnector populatedStaticConnector() throws ComponentInitializationException {
        Attribute attr;
        Set<Attribute> attributeSet;
        Set<AttributeValue> valuesSet;

        valuesSet = new LazySet<AttributeValue>();
        attributeSet = new LazySet<Attribute>();

        valuesSet.add(new StringAttributeValue(COMMON_ATTRIBUTE_VALUE_STRING));
        valuesSet.add(new StringAttributeValue(CONNECTOR_ATTRIBUTE_VALUE_STRING));
        attr = new Attribute(DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR);
        attr.setValues(valuesSet);
        attributeSet.add(attr);

        attr = new Attribute(DEPENDS_ON_SECOND_ATTRIBUTE_NAME);
        valuesSet = new LazySet<AttributeValue>();
        valuesSet.add(new StringAttributeValue(SECOND_ATTRIBUTE_VALUE_STRINGS[0]));
        valuesSet.add(new StringAttributeValue(SECOND_ATTRIBUTE_VALUE_STRINGS[1]));
        attr.setValues(valuesSet);
        attributeSet.add(attr);
        
        StaticDataConnector connector = new StaticDataConnector();
        connector.setId(STATIC_CONNECTOR_NAME);
        connector.setValues(attributeSet);
        connector.initialize();

        return connector;
    }

    /**
     * Create a static attribute with known values.
     * 
     * @return the attribute definition
     * @throws ComponentInitializationException if we cannot initialized (unlikely)
     */
    public static BaseAttributeDefinition populatedStaticAttribute() throws ComponentInitializationException {
        Attribute attr;
        Set<AttributeValue> valuesSet;

        valuesSet = new LazySet<AttributeValue>();

        valuesSet.add(new StringAttributeValue(COMMON_ATTRIBUTE_VALUE_STRING));
        valuesSet.add(new StringAttributeValue(ATTRIBUTE_ATTRIBUTE_VALUE_STRING));
        attr = new Attribute(DEPENDS_ON_ATTRIBUTE_NAME_ATTR);
        attr.setValues(valuesSet);
        
        StaticAttributeDefinition definition = new StaticAttributeDefinition();
        definition.setId(STATIC_ATTRIBUTE_NAME);
        definition.setValue(attr);
        definition.initialize();
        return definition;
    }

}
