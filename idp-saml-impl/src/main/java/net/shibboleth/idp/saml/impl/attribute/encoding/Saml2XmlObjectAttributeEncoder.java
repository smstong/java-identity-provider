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

package net.shibboleth.idp.saml.impl.attribute.encoding;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.XMLObjectAttributeValue;
import net.shibboleth.idp.attribute.mapper.IdPRequestedAttribute;
import net.shibboleth.idp.attribute.mapper.impl.RequestedAttributeMapper;
import net.shibboleth.idp.attribute.mapper.impl.XmlObjectAttributeValueMapper;
import net.shibboleth.idp.saml.attribute.encoding.AbstractSaml2AttributeEncoder;
import net.shibboleth.idp.saml.attribute.encoding.AttributeMapperFactory;
import net.shibboleth.idp.saml.attribute.encoding.SamlEncoderSupport;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;

/**
 * {@link net.shibboleth.idp.attribute.AttributeEncoder} that produces SAML 2 attributes from
 * {@link IdPAttribute} that contains {@link XMLObject} values.
 */
public class Saml2XmlObjectAttributeEncoder extends AbstractSaml2AttributeEncoder<XMLObjectAttributeValue> implements
        AttributeMapperFactory<RequestedAttribute, IdPRequestedAttribute> {

    /** {@inheritDoc} */
    protected boolean canEncodeValue(IdPAttribute attribute, AttributeValue value) {
        return value instanceof XMLObjectAttributeValue;
    }

    /** {@inheritDoc} */
    protected XMLObject encodeValue(IdPAttribute attribute, XMLObjectAttributeValue value)
            throws AttributeEncodingException {
        return SamlEncoderSupport.encodeXmlObjectValue(attribute,
                org.opensaml.saml.saml2.core.AttributeValue.DEFAULT_ELEMENT_NAME, value.getValue());
    }

    /** {@inheritDoc} */
    @Nonnull public RequestedAttributeMapper getRequestedMapper() {
        final RequestedAttributeMapper val;

        val = new RequestedAttributeMapper();
        val.setAttributeFormat(getNamespace());
        val.setId(getFriendlyName());
        val.setSAMLName(getName());
        val.setValueMapper(new XmlObjectAttributeValueMapper());

        return val;
    }

}