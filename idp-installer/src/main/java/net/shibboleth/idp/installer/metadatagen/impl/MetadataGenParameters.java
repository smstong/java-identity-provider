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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.springframework.core.io.Resource;

import net.shibboleth.idp.installer.metadata.impl.MetadataGeneratorParametersImpl;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * A simple bean for processing certificates in an easy to digest manner.
 * This is a cut down version of {@link MetadataGeneratorParametersImpl}.
 */
public class MetadataGenParameters extends AbstractInitializableComponent {

    /**
     * The file with the certificate the IDP uses to encrypt.
     */
    private File encryptionCert;

    /**
     * The strings with the encryption cert in them (to allow for multiline output).
     */
    private List<String> encryptionCerts;

    /**
     * The file with the certificate that TLS uses to 'sign'.
     */
    @Nullable private File backChannelCert;

    /**
     * The strings with the back channel cert in them (to allow for multiline output).
     */
    private List<String> backChannelCerts;

    /**
     * The file with the certificate the IDP uses to sign.
     */
    private File signingCert;

    /**
     * The strings with the signing certs in them (to allow for multiline output).
     */
    private List<String> signingCerts;

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        try {
            encryptionCerts = getCertificateContents(encryptionCert);
            signingCerts = getCertificateContents(signingCert);
            backChannelCerts = getCertificateContents(backChannelCert);
        } catch (final IOException e) {
            throw new ComponentInitializationException(e);
        }
    }

    /**
     * Get the (mutli-line) string representations of the encryption certs.
     *
     * @return Returns the encryption cert or null if none available.
     */
    @Nullable public List<String> getEncryptionCert() {
        return encryptionCerts;
    }

    /**
     * Set the encryption Certificate file.
     *
     * @param resource what to set.
     */
    public void setEncryptionCertResource(final Resource resource) {

        try {
            encryptionCert = resource.getFile();
        } catch (final IOException e) {
            encryptionCert = null;
        }
    }

    /**
     * Get the (mutli-line) string representation of the signing cert.
     *
     * @return Returns the signing cert or null if none available.
     */
    @Nullable public List<String> getSigningCert() {
        return signingCerts;
    }

    /**
     * Set the signing Certificate file.
     *
     * @param resource what to set.
     */
    public void setSigningCertResource(final Resource resource) {
        try {
            signingCert = resource.getFile();
        } catch (final IOException e) {
            signingCert = null;
        }
    }

    /**
     * Get the (mutli-line)string representation of the back channel cert.
     *
     * @return Returns the back channel cert or null if non available.
     */
    @Nullable public List<String> getBackchannelCert() {
        return backChannelCerts;
    }

    /**
     * Set the Backchannel Certificate file.
     *
     * @param file what to set.
     */
    public void setBackchannelCert(final File file) {
        backChannelCert = file;
    }
    
    /**
     * Set the Backchannel Certificate.
     *
     * @param resource what to set.
     */
    public void setBackchannelCertResource(@Nullable final Resource resource) {
        if (resource == null) {
           backChannelCert = null;
        } else {
            try {
                backChannelCert = resource.getFile();
            } catch (final IOException e) {
                backChannelCert = null;
            }
        }
    }

    /**
     * Open the file and return the contents and a list of lines.
     *
     * @param file the file
     * @return the contents
     * @throws IOException if we have issues with reading an existing file.
     */
    private List<String> getCertificateContents(@Nullable final File file) throws IOException {
        if (null == file || !file.exists()) {
            return null;
        }

        try (final FileReader fr = new FileReader(file);
                final BufferedReader reader = new BufferedReader(fr)) {
            final List<String> output = new ArrayList<>();
            String s = reader.readLine();
            while (s != null) {
                output.add(s);
                s = reader.readLine();
            }
            if ((output.size() > 0) && output.get(0).startsWith("----")) {
                output.remove(0);
            }
            final int last = output.size() - 1;
            if (last <= 0) {
                return null;
            }
            if (output.get(last).startsWith("----")) {
                output.remove(last);
            }
            return output;
        }
    }
}
