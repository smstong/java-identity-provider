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

package net.shibboleth.idp.attribute;

import java.util.function.Supplier;
import com.google.common.collect.Multimap;

/**
 * Container for decoded attributes. This gives us a distinguished class to look for in the
 * {@link org.opensaml.core.xml.XMLObject#getObjectMetadata()}.
 */
public class AttributesMapContainer implements Supplier<Multimap<String,IdPAttribute>> {

    /** The map we are encapsulating.*/
    private final Multimap<String,IdPAttribute> providedValue;

    /**
     * Constructor.
     * 
     * @param value the value to return.
     */
    public AttributesMapContainer(final Multimap<String,IdPAttribute> value) {
        providedValue = value;
    }

    /** {@inheritDoc} */
    @Override public Multimap<String,IdPAttribute> get() {
        return providedValue;
    }

}
