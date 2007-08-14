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

package edu.internet2.middleware.shibboleth.common.attribute.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncoder;

/**
 * Basic {@Attribute} implementation.
 * 
 * @param <ValueType> value type
 */
public class BasicAttribute<ValueType> extends BaseAttribute<ValueType> implements Cloneable {

    /** ID of this attribute. */
    private String id;

    /** Map of attribute encoders for this attribute, keyed off of category. */
    private ArrayList<AttributeEncoder> encoders;

    /** Set of values for this attribute. */
    private Collection<ValueType> values;

    /** Comparator for this attribute. */
    private Comparator<ValueType> comparator;

    /** Constructor. */
    public BasicAttribute() {
        encoders = new ArrayList<AttributeEncoder>();
        values = new ArrayList<ValueType>();
    }

    /**
     * Constructor.
     * 
     * @param attributeId the ID of this attribute
     */
    public BasicAttribute(String attributeId) {
        id = DatatypeHelper.safeTrimOrNullString(attributeId);
        encoders = new ArrayList<AttributeEncoder>();
        values = new ArrayList<ValueType>();
    }

    /** {@inheritDoc} */
    public ArrayList<AttributeEncoder> getEncoders() {
        return encoders;
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /**
     * Set id of this attribute.
     * 
     * @param newID new ID
     */
    public void setId(String newID) {
        id = newID;
    }

    /** {@inheritDoc} */
    public Comparator<ValueType> getValueComparator() {
        return comparator;
    }

    /**
     * Set value comparator for this attribute.
     * 
     * @param newComparator new value comparator
     */
    public void setValueComparator(Comparator<ValueType> newComparator) {
        comparator = newComparator;
    }

    /** {@inheritDoc} */
    public Collection<ValueType> getValues() {
        return values;
    }

    /**
     * Replace the current set of values with the given set.
     * 
     * @param newValues new values to replace existing ones
     */
    public void setValues(Collection<ValueType> newValues) {
        values = newValues;
    }

    /** {@inheritDoc} */
    public BasicAttribute<ValueType> clone() {
        BasicAttribute<ValueType> newAttribute = new BasicAttribute<ValueType>();

        newAttribute.setId(getId());

        newAttribute.setValueComparator(this.getValueComparator());

        newAttribute.getValues().addAll(getValues());

        newAttribute.getEncoders().addAll(getEncoders());

        return newAttribute;
    }
}