/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider;

import java.util.HashMap;
import java.util.Map;

import edu.internet2.middleware.shibboleth.common.attribute.AttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.AttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.DataConnector;

/**
 * Contextual information for performing an attribute resolution.
 */
public class ShibbolethResolutionContext {

    /** Attribute request context. */
    private ShibbolethAttributeRequestContext requestContext;

    /** Attribute Definitions that have been resolved for this request. */
    private Map<String, AttributeDefinition> definitions;

    /** Data Connectors that have been resolved for this request. */
    private Map<String, DataConnector> connectors;
    
    /**
     * Constructor.
     *
     * @param context the attribute request this resolution is being performed for
     */
    public ShibbolethResolutionContext(ShibbolethAttributeRequestContext context) {
        requestContext = context;
        definitions = new HashMap<String, AttributeDefinition>();
        connectors = new HashMap<String, DataConnector>();
    }
    
    /**
     * Gets the attribute request that started this resolution.
     * 
     * @return attribute request that started this resolution
     */
    public AttributeRequestContext getAttributeRequestContext(){
        return requestContext;
    }

    /** {@inheritDoc} */
    public Map<String, AttributeDefinition> getResolvedAttributeDefinitions() {
        return definitions;
    }

    /** {@inheritDoc} */
    public Map<String, DataConnector> getResolvedDataConnectors() {
        return connectors;
    }
}