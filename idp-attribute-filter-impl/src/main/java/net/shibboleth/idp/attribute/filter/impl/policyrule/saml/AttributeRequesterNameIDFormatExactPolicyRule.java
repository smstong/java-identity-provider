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

package net.shibboleth.idp.attribute.filter.impl.policyrule.saml;

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;

import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.SSODescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Checks if the attribute issuer supports the required NameID format. */
public class AttributeRequesterNameIDFormatExactPolicyRule extends AbstractNameIDFormatSupportedPolicyRule {

    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeRequesterNameIDFormatExactPolicyRule.class);

    /** {@inheritDoc} */
    @Override @Nullable protected SSODescriptor getEntitySSODescriptor(final AttributeFilterContext filterContext) {
        final SAMLMetadataContext metadataContext = filterContext.getRequesterMetadataContext();

        if (null == metadataContext) {
            log.warn("{} Could not locate SP metadata context", getLogPrefix());
            return null;
        }
        RoleDescriptor role = metadataContext.getRoleDescriptor();
        if (null == role) {
            log.warn("{} Could not locate RoleDescriptor in SP metadata", getLogPrefix());
            return null;
        }
        
        if (role instanceof SSODescriptor) {
            return (SSODescriptor) role;
        }
        log.warn("{} Located Role descriptor was of type {} and so could not be used", getLogPrefix(), role.getClass()
                .toString());
        return null;
    }

}
