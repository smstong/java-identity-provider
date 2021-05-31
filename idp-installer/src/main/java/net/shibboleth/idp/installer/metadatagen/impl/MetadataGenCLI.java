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

package net.shibboleth.idp.installer.metadatagen.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.LangBearing;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.ext.reqattr.RequestedAttributes;
import org.opensaml.saml.ext.saml2mdui.Description;
import org.opensaml.saml.ext.saml2mdui.DisplayName;
import org.opensaml.saml.ext.saml2mdui.Logo;
import org.opensaml.saml.ext.saml2mdui.UIInfo;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.metadata.ArtifactResolutionService;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
import org.opensaml.saml.saml2.metadata.AttributeService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import net.shibboleth.ext.spring.cli.AbstractCommandLine;
import net.shibboleth.idp.Version;
import net.shibboleth.idp.cli.AbstractIdPHomeAwareCommandLine;
import net.shibboleth.idp.saml.xmlobject.ExtensionsConstants;
import net.shibboleth.idp.saml.xmlobject.Scope;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.DOMTypeSupport;
import net.shibboleth.utilities.java.support.xml.XMLConstants;

/**
 * Command Line to generate Metadata.
 */
public class MetadataGenCLI extends AbstractIdPHomeAwareCommandLine<MetadataGenCommandLineArguments> {

    /** Class logger. */
    @Nullable private Logger log;

    /** Certificate and other property driven data. */
    private MetadataGenParameters parameters;

    /** Where we are outputting to? */
    private PrintWriter output;

    /** The processed arguments. */
    private MetadataGenCommandLineArguments args;
    
    /** The DnsName (cached because we need it often). */
    private String dnsName;
    
    /** {@inheritDoc} */
    @Override
    @Nonnull protected synchronized Logger getLogger() {
        if (log == null) {
            log = LoggerFactory.getLogger(MetadataGenCLI.class);
        }
        return log;
    }

    /** {@inheritDoc} */
    protected Class<MetadataGenCommandLineArguments> getArgumentClass() {
        return MetadataGenCommandLineArguments.class;
    }

    /** {@inheritDoc} */
    protected String getVersion() {
        return Version.getVersion();
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements protected List<Resource> getAdditionalSpringResources() {
        return List.of(
               new ClassPathResource("net/shibboleth/idp/installer/metadatagen/metadatagen.xml"));
    }

    /**
     * Write out any &lt;KeyDescriptor&gt;Elements.
     * @param outputBackChannel Do we output the back channel certificates?
     */
    private void outputKeyDescriptors(final boolean outputBackChannel) {
        final List<List<String>> signing = new ArrayList<>(2);
        if (outputBackChannel &&
            parameters.getBackchannelCert() != null &&
            !parameters.getBackchannelCert().isEmpty()) {
            output.format("        <!--  First signing certificate is BackChannel, the Second is FrontChannel -->\n");
            signing.add(parameters.getBackchannelCert());
        }
        if (parameters.getSigningCert() != null && !parameters.getSigningCert().isEmpty()) {
            signing.add(parameters.getSigningCert());
        }
        outputKeyDescriptors(signing, "signing");
        outputKeyDescriptors(Collections.singletonList(parameters.getEncryptionCert()), "encryption");
        output.format("\n");
    }

    /**
     * Write out &lt;KeyDescriptor&gt;Elements. of a specific type
     *
     * @param certs the certificates
     * @param use the type - signing or encryption
     */
    private void outputKeyDescriptors(@Nullable final List<List<String>> certs, @Nonnull @NotEmpty final String use) {

        if (null == certs || certs.isEmpty()) {
            return;
        }
        for (final List<String> cert : certs) {
            output.format("        <%s use=\"%s\">\n",KeyDescriptor.DEFAULT_ELEMENT_LOCAL_NAME, use);
            output.format("          <%s:%s>\n", SignatureConstants.XMLSIG_PREFIX, KeyInfo.DEFAULT_ELEMENT_LOCAL_NAME);
            output.format("            <%s:%s>\n",
                    SignatureConstants.XMLSIG_PREFIX, X509Data.DEFAULT_ELEMENT_LOCAL_NAME);
            output.format("              <%s:%s>\n",
                    SignatureConstants.XMLSIG_PREFIX, X509Certificate.DEFAULT_ELEMENT_LOCAL_NAME);
            output.format("%s\n",String.join("\n", cert));
            output.format("              </%s:%s>\n",
                    SignatureConstants.XMLSIG_PREFIX, X509Certificate.DEFAULT_ELEMENT_LOCAL_NAME);
            output.format("            </%s:%s>\n",
                    SignatureConstants.XMLSIG_PREFIX, X509Data.DEFAULT_ELEMENT_LOCAL_NAME);
            output.format("          </%s:%s>\n",SignatureConstants.XMLSIG_PREFIX, KeyInfo.DEFAULT_ELEMENT_LOCAL_NAME);
            output.format("        </%s>\n\n",KeyDescriptor.DEFAULT_ELEMENT_LOCAL_NAME);
        }
    }

    /** Output the MDUI for one language.
     * @param lang the language to emit
     *
     */
    private void outputMDUI(final String lang) {
        final Environment env = getApplicationContext().getEnvironment();
        final String displayName = env.getProperty(MetadataGenCommandLineArguments.MDUI_DISPLAY_NAME_PREFIX+lang);
        if (displayName != null) {
            output.format("            <%s:%s %s:%s=\"%s\">%s<%s:%s>\n",
                    SAMLConstants.SAML20MDUI_PREFIX, DisplayName.DEFAULT_ELEMENT_LOCAL_NAME,
                    XMLConstants.XML_PREFIX, LangBearing.XML_LANG_ATTR_LOCAL_NAME,
                    lang, displayName,
                    SAMLConstants.SAML20MDUI_PREFIX, DisplayName.DEFAULT_ELEMENT_LOCAL_NAME);
        }
        final String description = env.getProperty(MetadataGenCommandLineArguments.MDUI_DESCRIPTION_PREFIX+lang);
        if (description != null) {
            output.format("            <%s:%s %s:%s=\"%s\">%s<%s:%s>\n",
                    SAMLConstants.SAML20MDUI_PREFIX, Description.DEFAULT_ELEMENT_LOCAL_NAME,
                    XMLConstants.XML_PREFIX, LangBearing.XML_LANG_ATTR_LOCAL_NAME,
                    lang, description,
                    SAMLConstants.SAML20MDUI_PREFIX, Description.DEFAULT_ELEMENT_LOCAL_NAME);
        }
    }

    /** Output Scope and MDUI.
     * @param includeMDUI do we output the MDUI
     */
    private void outputExtensions(final boolean includeMDUI) {
        output.format("        <%s>\n", Extensions.DEFAULT_ELEMENT_LOCAL_NAME);
        final Environment env = getApplicationContext().getEnvironment();
        final String scope = StringSupport.trimOrNull(env.getProperty("idp.scope"));
        if (scope != null) {
            output.format("          <%s:%s regexp=\"false\">%s</%s:%s>\n",
                    ExtensionsConstants.SHIB_MDEXT10_PREFIX, Scope.DEFAULT_ELEMENT_LOCAL_NAME,
                    scope,
                    ExtensionsConstants.SHIB_MDEXT10_PREFIX, Scope.DEFAULT_ELEMENT_LOCAL_NAME);
        }

        if (includeMDUI) {
            output.format("          <%s:%s>\n",
                    SAMLConstants.SAML20MDUI_PREFIX, UIInfo.DEFAULT_ELEMENT_LOCAL_NAME);
            final String mduiLangs = env.getProperty(MetadataGenCommandLineArguments.MDUI_LANGS_PROPERTY);
            if (mduiLangs == null) {
                output.format("\n<!--\n          Fill in the details for your IdP here\n\n");
                output.format("            <%s:%s %s:%s=\"en\">A Name for the IdP<%s:%s>\n",
                        SAMLConstants.SAML20MDUI_PREFIX, DisplayName.DEFAULT_ELEMENT_LOCAL_NAME,
                        XMLConstants.XML_PREFIX, LangBearing.XML_LANG_ATTR_LOCAL_NAME,
                        SAMLConstants.SAML20MDUI_PREFIX, DisplayName.DEFAULT_ELEMENT_LOCAL_NAME);
                output.format("            <%s:%s %s:%s=\"en\">A Description for the IdP<%s:%s>\n",
                        SAMLConstants.SAML20MDUI_PREFIX, Description.DEFAULT_ELEMENT_LOCAL_NAME,
                        XMLConstants.XML_PREFIX, LangBearing.XML_LANG_ATTR_LOCAL_NAME,
                        SAMLConstants.SAML20MDUI_PREFIX, Description.DEFAULT_ELEMENT_LOCAL_NAME);
            } else {
                for (final String lang:mduiLangs.split(" ")) {
                    outputMDUI(lang);
                }
            }
            final String logoY = env.getProperty(MetadataGenCommandLineArguments.MDUI_LOGO_HEIGHT, "80");
            final String logoX = env.getProperty(MetadataGenCommandLineArguments.MDUI_LOGO_WIDTH, "80");
            final String logoPath = env.getProperty(MetadataGenCommandLineArguments.MDUI_LOGO_PATH, "/path/to/logo");
            output.format("            <%s:%s height=\"%s\" width=\"%s\">https://%s%s</%s:%s>\n",
                    SAMLConstants.SAML20MDUI_PREFIX, Logo.DEFAULT_ELEMENT_LOCAL_NAME,
                    logoY, logoX,  getDnsName(), logoPath,
                    SAMLConstants.SAML20MDUI_PREFIX, Logo.DEFAULT_ELEMENT_LOCAL_NAME);
            if (mduiLangs == null) {
                output.format("-->\n\n");
            }
            output.format("          </%s:%s>\n", SAMLConstants.SAML20MDUI_PREFIX, UIInfo.DEFAULT_ELEMENT_LOCAL_NAME);
        }
        output.format("\n        </%s>\n\n", Extensions.DEFAULT_ELEMENT_LOCAL_NAME);
    }

    /** Output Logout end points. */
    private void outputLogoutEndpoints() {
        output.format("        <%s Binding=\"%s\""
                + " Location=\"https://%s/idp/profile/SAML2/Redirect/SLO\"/>\n",
                SingleLogoutService.DEFAULT_ELEMENT_LOCAL_NAME,
                SAMLConstants.SAML2_REDIRECT_BINDING_URI,
                getDnsName());
        output.format("        <%s Binding=\"%s\""
                + " Location=\"https://%s/idp/profile/SAML2/POST/SLO\"/>\n",
                SingleLogoutService.DEFAULT_ELEMENT_LOCAL_NAME,
                SAMLConstants.SAML2_POST_BINDING_URI,
                getDnsName());
        output.format("        <%s Binding=\"%s\""
                + " Location=\"https://%s/idp/profile/SAML2/POST/SLO-SimpleSign\"/>\n",
                SingleLogoutService.DEFAULT_ELEMENT_LOCAL_NAME,
                SAMLConstants.SAML2_POST_SIMPLE_SIGN_BINDING_URI,
                getDnsName());

        if (parameters.getBackchannelCert() != null && !parameters.getBackchannelCert().isEmpty()) {
            output.format("        <%s Binding=\"%s\""
                    + " Location=\"https://%s:8443/idp/profile/SAML2/SOAP/SLO\"/>\n",
                    SingleLogoutService.DEFAULT_ELEMENT_LOCAL_NAME,
                    SAMLConstants.SAML2_SOAP11_BINDING_URI,
                    getDnsName());
        }
    }

    /** Output Artifact Endpoints. */
    private void outputArtifactEndpoints() {
        if (args.isSaml1()) {
            output.format("        <%s Binding=\"%s\""
                    + " Location=\"https://%s:8443/idp/profile/SAML1/SOAP/ArtifactResolution\" index=\"1\"/>\n",
                    ArtifactResolutionService.DEFAULT_ELEMENT_LOCAL_NAME,
                    SAMLConstants.SAML1_SOAP11_BINDING_URI,
                    getDnsName());
        }
        output.format("        <%s Binding=\"%s\""
                + " Location=\"https://%s:8443/idp/profile/SAML2/SOAP/ArtifactResolution\" index=\"2\"/>\n",
                ArtifactResolutionService.DEFAULT_ELEMENT_LOCAL_NAME,
                SAMLConstants.SAML2_SOAP11_BINDING_URI,
                getDnsName());
    }

    /** Output SSO Endpoints. */
    private void outputSSOEndpoints() {
        if (args.isSaml1()) {
            output.format("        <%s Binding=\"%s\""
                    + " Location=\"https://%s/idp/profile/Shibboleth/SSO\"/>\n",
                    SingleSignOnService.DEFAULT_ELEMENT_LOCAL_NAME,
                    "urn:mace:shibboleth:1.0:profiles:AuthnRequest",
                    getDnsName());

        }
        output.format("        <%s Binding=\"%s\""
                + " %s:%s=\"true\""
                + " Location=\"https://%s/idp/profile/SAML2/POST/SSO\"/>\n",
                SingleSignOnService.DEFAULT_ELEMENT_LOCAL_NAME,
                SAMLConstants.SAML2_POST_BINDING_URI,
                SAMLConstants.SAML20PREQ_ATTRR_PREFIX, RequestedAttributes.SUPPORTS_REQUESTED_ATTRIBUTES_LOCAL_NAME,
                getDnsName());
        output.format("        <%s Binding=\"%s\""
                + " %s:%s=\"true\""
                + " Location=\"https://%s/idp/profile/SAML2/POST-SimpleSign/SSO\"/>\n",
                SingleSignOnService.DEFAULT_ELEMENT_LOCAL_NAME,
                SAMLConstants.SAML2_POST_SIMPLE_SIGN_BINDING_URI,
                SAMLConstants.SAML20PREQ_ATTRR_PREFIX, RequestedAttributes.SUPPORTS_REQUESTED_ATTRIBUTES_LOCAL_NAME,
                getDnsName());
        output.format("        <%s Binding=\"%s\""
                + " %s:%s=\"true\""
                + " Location=\"https://%s/idp/profile/SAML2/Redirect/SSO\"/>\n",
                SingleSignOnService.DEFAULT_ELEMENT_LOCAL_NAME,
                SAMLConstants.SAML2_REDIRECT_BINDING_URI,
                SAMLConstants.SAML20PREQ_ATTRR_PREFIX, RequestedAttributes.SUPPORTS_REQUESTED_ATTRIBUTES_LOCAL_NAME,
                getDnsName());
    }

    /**
     * Write the &lt;IDPSSODescriptor&gt;.
     */
    private void outputIDPSSO() {
        final List<String> protocols = new ArrayList<>(4);
        if (!args.isSaml1() && !args.isSaml2()) {
            return;
        }
        if (args.isSaml1()) {
            protocols.add(SAMLConstants.SAML20P_NS);
        }
        if (args.isSaml2()) {
            protocols.add(SAMLConstants.SAML20P_NS);
            protocols.add(SAMLConstants.SAML11P_NS);
            protocols.add("urn:mace:shibboleth:1.0");
        }
        output.format("    <%s protocolSupportEnumeration=\"%s\">\n",
                IDPSSODescriptor.DEFAULT_ELEMENT_LOCAL_NAME,
                String.join(" ", protocols));

        outputExtensions(true);

        outputKeyDescriptors(true);

        if (args.isArtifact()) {
            outputArtifactEndpoints();
        }

        if (args.isLogout()) {
            outputLogoutEndpoints();
        }

        outputSSOEndpoints();

        output.format("    </%s>\n", IDPSSODescriptor.DEFAULT_ELEMENT_LOCAL_NAME);
    }

    /**
     * Write the &lt;AttributeAuthorityDescriptor&gt;.*/
    private void outputAtttributeAuthorityDescriptor() {
        final List<String> protocols;

        if (args.isSaml1()) {
            if (args.isAttributeFetch()) {
                // Both
                protocols = Arrays.asList(SAMLConstants.SAML20P_NS, SAMLConstants.SAML11P_NS);
            } else {
                // SAML1 only
                protocols = Collections.singletonList(SAMLConstants.SAML11P_NS);
            }
        } else if (args.isAttributeFetch()) {
            // SAML2 only
            protocols = Collections.singletonList(SAMLConstants.SAML20P_NS);
        } else {
            // Neither
            return;
        }

        output.format("    <%s protocolSupportEnumeration=\"%s\">\n",
                AttributeAuthorityDescriptor.DEFAULT_ELEMENT_LOCAL_NAME,
                String.join(" ", protocols));

        outputExtensions(false);
        outputKeyDescriptors(true);
        if (args.isSaml1()) {
            output.format("        <%s Binding=\"%s\""
                    + " Location=\"https://%s:8443/idp/profile/SAML1/SOAP/AttributeQuery\"/>\n",
                    AttributeService.DEFAULT_ELEMENT_LOCAL_NAME,
                    SAMLConstants.SAML1_SOAP11_BINDING_URI,
                    getDnsName());
        }
        if (args.isAttributeFetch()) {
            output.format("        <%s Binding=\"%s\""
                    + " Location=\"https://%s:8443/idp/profile/SAML2/SOAP/AttributeQuery\"/>\n",
                    AttributeService.DEFAULT_ELEMENT_LOCAL_NAME,
                    SAMLConstants.SAML2_SOAP11_BINDING_URI,
                    getDnsName());
        }
        output.format("    </%s>\n", AttributeAuthorityDescriptor.DEFAULT_ELEMENT_LOCAL_NAME);
    }

    /**
     * Write the &lt;SPSSODescriptor&gt;.
     */
    private void outputSPSSO() {
        if (!args.isSamlSP()) {
            return;
        }
        output.format("    <%s protocolSupportEnumeration=\"%s\">\n",
                SPSSODescriptor.DEFAULT_ELEMENT_LOCAL_NAME,
                SAMLConstants.SAML20P_NS);

        outputKeyDescriptors(false);

        output.format("        <%s Binding=\"%s\""
                + " Location=\"https://%s/idp/profile/Authn/SAML2/POST/SSO\" index=\"0\"/>\n",
                AssertionConsumerService.DEFAULT_ELEMENT_LOCAL_NAME,
                SAMLConstants.SAML2_POST_BINDING_URI,
                getDnsName());

        output.format("    </%s>\n", SPSSODescriptor.DEFAULT_ELEMENT_LOCAL_NAME);
    }


    /** Output the metadata.
     * @return true iff this worked.
     */
    private int outputMetadata() {
        output.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        output.format("   <!--\n");
        output.format("     This is example metadata only. Do *NOT* supply it as is without review,\n");
        output.format("     and do *NOT* provide it in real time to your partners.\n");
        output.format("     This metadata is not dynamic - run metadatagen again to recreate.\n");
        output.format("     Created: %s\n   -->\n", Instant.now().toString());
        output.format("<%s  xmlns=\"%s\" xmlns:%s=\"%s\"\n", EntityDescriptor.DEFAULT_ELEMENT_LOCAL_NAME,
                SAMLConstants.SAML20MD_NS,
                SignatureConstants.XMLSIG_PREFIX, SignatureConstants.XMLSIG_NS);
        output.format("      xmlns:%s=\"%s\" xmlns:%s=\"%s\"\n",
                ExtensionsConstants.SHIB_MDEXT10_PREFIX, ExtensionsConstants.SHIB_MDEXT10_NS,
                XMLConstants.XML_PREFIX, XMLConstants.XML_NS);
        output.format("      xmlns:%s=\"%s\" xmlns:%s=\"%s\"\n",
                SAMLConstants.SAML20MDUI_PREFIX, SAMLConstants.SAML20MDUI_NS,
                SAMLConstants.SAML20PREQ_ATTRR_PREFIX, SAMLConstants.SAML20PREQ_ATTR_NS);
        output.format(" validUntil=\"%s\" entityID=\"%s\">\n\n",
                DOMTypeSupport.instantToString(Instant.now()), 
                getApplicationContext().getEnvironment().getProperty("idp.entityID", "idp.example.org"));
        outputIDPSSO();
        outputAtttributeAuthorityDescriptor();
        outputSPSSO();
        output.format("</EntityDescriptor>\n");
        output.flush();
        output.close();
        return RC_OK;
    }
    
    /** Lookup the dns name with a default and cache it.
     *  @return the dns name
     */
    @Nonnull String getDnsName() {
        if (dnsName == null) {
            dnsName = getApplicationContext().
                    getEnvironment().
                    getProperty(MetadataGenCommandLineArguments.DNS_NAME_PROPERTY, "idp.example.org");
        }
        return dnsName;
    }

    /** Build the {@link MetadataGenCLI#parameters} object.
     * @return true iff this worked and if everything needed was there.
     */
    // Checkstyle: CyclomaticComplexity OFF
    private boolean populateParameters() {
        try {
            parameters = getApplicationContext().getBean(MetadataGenParameters.class);
        } catch (final NoSuchBeanDefinitionException e) {
            getLogger().error("Could not locate IdPConfiguration");
            return false;
        }
        final boolean hasBackChannel = parameters.getBackchannelCert() != null 
                && !parameters.getBackchannelCert().isEmpty();
        boolean worked = true;
        if (args.isArtifact() && !hasBackChannel) {
            getLogger().error("Must specify --backChannel <path> if +artifact speificied");
            worked = false;
        }
        if (args.isAttributeFetch() && !hasBackChannel) {
            getLogger().error("Must specify --backChannel <path> if +attributeFetch speificied");
            worked = false;
        }
        if (hasBackChannel && !args.isAttributeFetch() && !args.isArtifact()  && !args.isSaml1()) {
            getLogger().error("--backChannel <path> requires +artifact and/or +attributeFetch and/or +saml1");
            worked = false;
        }
        return worked;
    }
    // Checkstyle: CyclomaticComplexity ON


    /** Set up {@link MetadataGenCLI#output}.
     * @return true iff this worked.
     */
    private boolean setupWriter() {
        if (args.getOutput() == null) {
            output = System.console().writer();
        } else {
            final File out = new File(args.getOutput());
            try {
                final FileOutputStream outStream = new FileOutputStream(out);
                output = new PrintWriter(new BufferedOutputStream(outStream));
            } catch (final IOException e) {
                getLogger().error("Could not open {}", args.getOutput(), e);
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected int doRun(@Nonnull final MetadataGenCommandLineArguments arguments) {

        args = arguments;
        final int ret = super.doRun(args);
        if (ret != RC_OK) {
            return ret;
        }
        if (!setupWriter()) {
            return RC_IO;
        }
        if (!populateParameters()) {
            return RC_IO;
        }
        final int i = outputMetadata();
        this.output.close();
        return i;
    }

    /** Shim for CLI entry point: Allows the code to be run from a test.
    *
    * @return one of the predefines {@link AbstractCommandLine#RC_INIT},
    * {@link AbstractCommandLine#RC_IO}, {@link AbstractCommandLine#RC_OK}
    * or {@link AbstractCommandLine#RC_UNKNOWN}
    *
    * @param args arguments
    */
   public static int runMain(@Nonnull final String[] args) {
       final MetadataGenCLI cli = new MetadataGenCLI();

       return cli.run(args);
   }

   /**
    * CLI entry point.
    * @param args arguments
    */
   public static void main(@Nonnull final String[] args) {
       System.exit(runMain(args));
   }
}
