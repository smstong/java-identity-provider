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

package net.shibboleth.idp.profile.spring.relyingparty.security;

import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.util.BaseSpringNamespaceHandler;
import net.shibboleth.idp.profile.spring.relyingparty.security.credential.BasicInlineCredentialParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.credential.BasicResourceCredentialParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.credential.X509InlineCredentialParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.credential.X509ResourceCredentialParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.trustengine.CertPathPKIXValidationOptionsParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.trustengine.PKIXInlineValidationInfoParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.trustengine.PKIXResourceValidationInfoParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.trustengine.PKIXValidationOptionsParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.trustengine.SignatureChainingParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.trustengine.StaticExplicitKeySignatureParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.trustengine.StaticPKIXSignatureParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.trustengine.UnsupportedTrustEngineParser;

/** Namespace handler <em>{@value NAMESPACE}</em>. */
public class SecurityNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:security";

    /** Credential element name. */
    public static final QName CREDENTIAL_ELEMENT_NAME = new QName(NAMESPACE, "Credential");

    /** TrustEngine element name. */
    public static final QName TRUST_ENGINE_ELEMENT_NAME = new QName(NAMESPACE, "TrustEngine");

    /** SecurityPolicy element name. */
    public static final QName SECURITY_POLICY_NAME = new QName(NAMESPACE, "SecurityPolicy");

    /** {@inheritDoc} */
    @Override public void init() {
        // Credentials
        registerBeanDefinitionParser(X509ResourceCredentialParser.TYPE_NAME_FILESYSTEM,
                new X509ResourceCredentialParser());
        registerBeanDefinitionParser(X509ResourceCredentialParser.TYPE_NAME_RESOURCE,
                new X509ResourceCredentialParser());
        registerBeanDefinitionParser(X509InlineCredentialParser.TYPE_NAME, new X509InlineCredentialParser());
        registerBeanDefinitionParser(BasicInlineCredentialParser.TYPE_NAME, new BasicInlineCredentialParser());
        registerBeanDefinitionParser(BasicResourceCredentialParser.TYPE_NAME_FILESYSTEM,
                new BasicResourceCredentialParser());
        registerBeanDefinitionParser(BasicResourceCredentialParser.TYPE_NAME_RESOURCE,
                new BasicResourceCredentialParser());

        registerBeanDefinitionParser(StaticExplicitKeySignatureParser.TYPE_NAME,new StaticExplicitKeySignatureParser());
        registerBeanDefinitionParser(StaticPKIXSignatureParser.TYPE_NAME, new StaticPKIXSignatureParser());
        registerBeanDefinitionParser(SignatureChainingParser.TYPE_NAME, new SignatureChainingParser());

        // Metadata based unsupported 
        registerBeanDefinitionParser(UnsupportedTrustEngineParser.METADATA_EXPLICIT_KEY_TYPE,
                new UnsupportedTrustEngineParser());
        registerBeanDefinitionParser(UnsupportedTrustEngineParser.METADATA_EXPLICIT_KEY_SIGNATURE_TYPE,
                new UnsupportedTrustEngineParser());
        registerBeanDefinitionParser(UnsupportedTrustEngineParser.METADATA_PKIX_CREDENTIAL_TYPE,
                new UnsupportedTrustEngineParser());
        registerBeanDefinitionParser(UnsupportedTrustEngineParser.METADATA_PKIX_SIGNATURE_TYPE,
                new UnsupportedTrustEngineParser());

        // Validation Info
        registerBeanDefinitionParser(PKIXResourceValidationInfoParser.TYPE_NAME_FILESYSTEM,
                new PKIXResourceValidationInfoParser());
        registerBeanDefinitionParser(PKIXResourceValidationInfoParser.TYPE_NAME_RESOURCE,
                new PKIXResourceValidationInfoParser());
        registerBeanDefinitionParser(PKIXInlineValidationInfoParser.SCHEMA_TYPE, new PKIXInlineValidationInfoParser());

        // Validation Opts
        registerBeanDefinitionParser(PKIXValidationOptionsParser.ELEMENT_NAME, new PKIXValidationOptionsParser());
        registerBeanDefinitionParser(CertPathPKIXValidationOptionsParser.ELEMENT_NAME,
                new CertPathPKIXValidationOptionsParser());

        // Credential unsupported
        registerBeanDefinitionParser(UnsupportedTrustEngineParser.CHAINING_TYPE, new UnsupportedTrustEngineParser());
        registerBeanDefinitionParser(UnsupportedTrustEngineParser.PKIX_CREDENTIAL, new UnsupportedTrustEngineParser());
        registerBeanDefinitionParser(UnsupportedTrustEngineParser.STATIC_EXPLICIT_KEY_TYPE, new UnsupportedTrustEngineParser());

    }
}