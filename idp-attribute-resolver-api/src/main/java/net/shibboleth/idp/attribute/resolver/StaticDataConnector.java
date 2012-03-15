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

package net.shibboleth.idp.attribute.resolver;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/** A data connector that just returns a static collection of attributes. */
@ThreadSafe
public class StaticDataConnector extends BaseDataConnector {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(StaticDataConnector.class);

    /** Static collection of values returned by this connector. */
    private Optional<Map<String, Attribute>> values = Optional.absent();

    /**
     * Get the static values returned by this connector.
     * 
     * @return static values returned by this connector
     */
    @Nonnull public Map<String, Attribute> getValues() {
        return values.get();
    }

    /**
     * Set static values returned by this connector.
     * 
     * @param newValues static values returned by this connector
     */
    public synchronized void setValues(@Nullable @NullableElements Map<String, Attribute> newValues) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        // TODO need to deal with null collection and null elements

        values = Optional.<Map<String, Attribute>> of(new HashMap<String, Attribute>(newValues));
    }

    /** {@inheritDoc} */
    @Nonnull protected Optional<Map<String, Attribute>> doDataConnectorResolve(
            final AttributeResolutionContext resolutionContext) throws AttributeResolutionException {
        log.debug("Data connector '{}': Resolving static attribute {}", getId(), values.get());
        return values;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (!values.isPresent()) {
            throw new ComponentInitializationException("Static Data connector " + getId()
                    + " does not have values set up.");
        }
    }
}