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

package net.shibboleth.idp.attribute.resolver.spring.pc;

import net.shibboleth.idp.spring.BaseSpringNamespaceHandler;

/**
 * Namespace handler for the principal connector. This is a noop and is here purely to allow us to have a handler
 * declared (since all parsing is done inline).
 */
public class PrincipalConnectorNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for principal connector (which have not handlers). */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:resolver:pc";

    /** {@inheritDoc} */
    @Override public void init() {
        registerBeanDefinitionParser(DirectConnectorParser.ELEMENT_NAME, new DirectConnectorParser());

        registerBeanDefinitionParser(TransientConnectorParser.ELEMENT_NAME, new TransientConnectorParser());
    }
}