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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;

import net.shibboleth.idp.cli.AbstractIdPHomeAwareCommandLineArguments;

/**
 * Command line arguments for Metadata Generation.
 */
public class MetadataGenCommandLineArguments extends AbstractIdPHomeAwareCommandLineArguments {
    
    /** Property name for the back channel certificate. */
    public static final String BACKCHANNEL_PROPERTY = "idp.metadata.backchannel.cert";
    
    /** Property name for the back channel certificate. */
    public static final String DNS_NAME_PROPERTY = "idp.metadata.dnsname";

    /** Property name for the MDUI languages. */
    public static final String MDUI_LANGS_PROPERTY = "idp.metadata.idpsso.mdui.langs";

    /** Property prefix for DisplayName. */
    public static final String MDUI_DISPLAY_NAME_PREFIX = "idp.metadata.idpsso.mdui.displayname.";

    /** Property prefix for Description. */
    public static final String MDUI_DESCRIPTION_PREFIX = "idp.metadata.idpsso.mdui.description.";

    /** Property for logo y. */
    public static final String MDUI_LOGO_HEIGHT = "idp.metadata.idpsso.mdui.logo.height";

    /** Property for logo x. */
    public static final String MDUI_LOGO_WIDTH = "idp.metadata.idpsso.mdui.logo.width";

    /** Property for logo path. */
    public static final String MDUI_LOGO_PATH = "idp.metadata.idpsso.mdui.logo.path";

    /** Logger. */
    private Logger log;

    /** Do we output SAML2. */
    @Parameter(names = { "+saml2", "+2", "+SAML2"} )
    @Nullable private boolean saml2;

    /** Do we NOT output SAML2. */
    @Parameter(names = { "-saml2", "-2", "-SAML2"} )
    @Nullable private boolean noSaml2;

    /** Do we output SAM1.?*/
    @Parameter(names = { "+saml1", "+1", "+SAML1"})
    @Nullable private boolean saml1;

    /** Do we output for an SP.*/
    @Parameter(names = { "+samlSP", "+sp", "+SP", "+SAMLSP"})
    @Nullable private boolean samlSP;

    /** Do we output logout.*/
    @Parameter(names = { "+logout", "+lo"})
    @Nullable private boolean logout;

    /** Do we output Artifact.*/
    @Parameter(names = { "+artifact", "+artefact"})
    @Nullable private boolean artifact;

    /** Do we output for an Attribute Fetch.*/
    @Parameter(names = { "+attributeFetch"})
    @Nullable private boolean attributeFetch;

    /** Certificate for (IdP) BackChannel (attribute, artifact, logout).*/
    @Parameter(names = { "--backChannel", "-bc"})
    @Nullable private String backChannelPath;

    /** DNS name (for back channel addresses). */
    @Parameter(names = { "--DNSName", "-d"})
    @Nullable private String dnsName;

    /** Output.*/
    @Parameter(names = { "--output", "-o"})
    @Nullable private String output;

    /** Do we output SAML2 metadata?
     * @return what.
     */
    public boolean isSaml2() {
        return saml2;
    }

    /** Do we output SAML1 metadata?
     * @return what.
     */
    public boolean isSaml1() {
        return saml1;
    }

    /** Do we output SAML SP metadata.
     * @return what.
     */
    public boolean isSamlSP() {
        return samlSP;
    }

    /** Do we output Logout metadata?
     * @return what.
     */
    public boolean isLogout() {
        return logout;
    }

    /** Do we output Artifact metadata?
     * @return what.
     */
    public boolean isArtifact() {
        return artifact;
    }

    /** Do we output Attribute Fetch metadata?
     * @return what.
     */
    public boolean isAttributeFetch() {
        return attributeFetch;
    }

    /** Where to put the data.
     * @return where
     */
    @Nullable public String getOutput() {
        return output;
    }

    /** {@inheritDoc}
     * We override this to add a property file of our own making for
     * the backchannel (if needed) and dnsname.
     * */
    public List<String> getPropertyFiles() {
        final List<String> fromCmdline = super.getPropertyFiles();
        if (dnsName == null && backChannelPath == null) {
            return fromCmdline;
        }

        final Properties props = new Properties(2);
        if (dnsName != null) {
            props.setProperty(DNS_NAME_PROPERTY, dnsName);
        }
        if (backChannelPath != null) {
            props.setProperty(BACKCHANNEL_PROPERTY, backChannelPath);
        }
        
        File file = null;
        try {
            file = File.createTempFile("MetadataGen", ".properties");
            file.deleteOnExit();
            try (final FileOutputStream out = new FileOutputStream(file)) {
                props.store(out, "created");
            }
        } catch (final IOException e) {
            getLog().error("Could not generate property file", e);
        }
        if (fromCmdline.isEmpty()) {
            return Collections.singletonList(file.getAbsolutePath());
        }
        final List<String> result = new ArrayList<>(fromCmdline.size() + 1);
        result.addAll(fromCmdline);
        result.add(file.getAbsolutePath());
        return result;
    }

    @Override
    public synchronized Logger getLog() {
        if (log == null) {
            log = LoggerFactory.getLogger(MetadataGenCommandLineArguments.class);
        }
        return log;
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (!saml2 && noSaml2) {
            saml2 = false;
        } else {
            saml2 = true;
        }
    }
    // Checkstyle: CyclomaticComplcity ON

    @Override
    public void printHelp(final PrintStream out) {
        super.printHelp(out);
        out.println(String.format("  %-20s %s", "+SAML1, +1",
                                  "Output SAML1 Metadata."));
        out.println(String.format("  %-20s %s", "-SAML2, -2",
                                  "do NOT Output SAML2 Metadata."));
        out.println(String.format("  %-20s %s", "+SP, +SAMLSP",
                                  "Output SAML2 SP Metadata."));
        out.println(String.format("  %-20s %s", "+logout",
                                  "Output Logout Metadata."));
        out.println(String.format("  %-20s %s", "+artifact",
                                  "Output SAML artifact Metadata (requires -bc),"));
        out.println(String.format("  %-20s %s", "+attributeFetch",
                                  "Output SAML attributeFetch Metadata  (requires -bc)."));
        out.println(String.format("  %-20s %s", "-bc, --backchannel <Path>",
                                  "Path to backchannel certificate"));
        out.println(String.format("  %-20s %s", "-d, --DNSName name",
                                  "DNS name to use in back channel addresses (default idp.example.org)"));
        out.println(String.format("  %-20s %s", "--output, -o",
                                  "Output location."));
        out.println();
    }
}
