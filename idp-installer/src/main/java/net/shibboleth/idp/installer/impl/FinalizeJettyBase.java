/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.installer.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.annotation.Nonnull;

import net.shibboleth.idp.installer.InstallerSupport;
import net.shibboleth.idp.installer.PropertiesWithComments;

/**
 * Small class to do the post install work on an embedded jetty-base.
 * @deprecated  This is only used by the windows jetty-base installer
 * and will be removed when we use the plugin
 */
@Deprecated public final class FinalizeJettyBase {

    /** The IdP Installation dir. */
    @Nonnull private final Path idpHome;

    /** The jetty-base dir. */
    @Nonnull private final Path jettyBase;


    /** Constructor.
     * @throws IOException if we are incorrectly configured */
    private FinalizeJettyBase() throws IOException {
        final String home = System.getProperty("idp.home");
        if (home == null) {
            System.err.println("idp.home not specified");
            throw new IOException("idp.home not specified");
        }
        Path p = InstallerSupport.pathOf(home);
        assert p!=null;
        idpHome = p;
        if (!Files.exists(idpHome) || !Files.isDirectory(idpHome)) {
            final String msg = "'" + home + "' does not exist or is not a directory";
            System.err.println(msg);
            throw new IOException(msg);
        }
        p = idpHome.resolve("jetty-base");
        assert p!=null;
        jettyBase = p;
        if (!Files.exists(jettyBase) || !Files.isDirectory(jettyBase)) {
            final String msg = "'" + home + "/jetty-base' does not exist or is not a directory";
            System.err.println(msg);
            throw new IOException(msg);
        }
        p = jettyBase.resolve("start.d.dist");
        if (!Files.exists(p) || !Files.isDirectory(p)) {
            final String msg = "'" + home + "/jetty-base/start.d.dist' does not exist or is not a directory";
            System.err.println(msg);
            throw new IOException(msg);
        }

    }

    /** Do the work associated with finalizing this install.
     * @throws IOException if we encounter other issues
     */
    private void execute() throws IOException {
        createDirectories();
        final Path idpIni = jettyBase.resolve("start.d").resolve("idp.ini");
        if (Files.exists(idpIni)) {
            updateIdPini();
        } else {
            createP12IdPini();
        }
        final Path systemIniSrc = jettyBase.resolve("start.d.dist").resolve("idp-system.ini");
        final Path dest = jettyBase.resolve("start.d").resolve("idp-system.ini");
        assert systemIniSrc!=null && dest!=null;
        copyFile(systemIniSrc, dest);
        reprotect();
    }

    /**
     * If they don't exists create.
     * 
     * @throws IOException if we failed to create a directory
     */
    private void createDirectories() throws IOException {
        Files.createDirectories(jettyBase.resolve("start.d"));
        Files.createDirectories(jettyBase.resolve("logs"));
        Files.createDirectories(idpHome.resolve("static"));
    }

    /** Create the jetty.sslContext.keyStorePath as a copy
     * from the idp.backchannel.keyStorePath file.
     * The create start.d/idp.ini from the start.d.dist/idp.ini.windows
     * but replacing the two password properties
     * @throws IOException if we trip up.
     */
    private void createP12IdPini() throws IOException {
        final Path credentials = idpHome.resolve("credentials");
        final Path backChannelKeyStore = credentials.resolve("idp-backchannel.p12");
        final Path sslKeyStore = credentials.resolve("idp-userfacing.p12");
        assert backChannelKeyStore!=null && sslKeyStore!=null;

        if (!Files.exists(backChannelKeyStore)) {
            final String msg = backChannelKeyStore.toString() + " Does not exist";
            System.err.println(msg);
            throw new IOException(msg);
        }
        if (Files.exists(sslKeyStore)) {
            final String msg = sslKeyStore.toString() + " Exists";
            System.err.println(msg);
            throw new IOException(msg);
        }

        copyFile(backChannelKeyStore, sslKeyStore);

        final Properties passwords = new Properties();
        try (final FileInputStream in = new FileInputStream(credentials.resolve("secrets.properties").toFile())) {
            passwords.load(in);
        }
        final String p12Pass = passwords.getProperty("idp.backchannel.keyStorePassword");
        final Properties replace = new Properties(2);
        replace.setProperty("idp.backchannel.keyStorePassword", p12Pass);
        replace.setProperty("jetty.sslContext.keyStorePassword", p12Pass);

        final PropertiesWithComments idpIni = new PropertiesWithComments();
        final File inputIni = jettyBase.resolve("start.d.dist").resolve("idp.ini.windows").toFile();
        final File outputIni = jettyBase.resolve("start.d").resolve("idp.ini").toFile();
        try (final FileInputStream in = new FileInputStream(inputIni);
                final FileOutputStream out = new FileOutputStream(outputIni)) {
            idpIni.load(in);
            idpIni.replaceProperties(replace);
            idpIni.store(out);
        }
    }

    /** Rewrite any property names needed.
     * @throws IOException on a failed update
     */
    private void updateIdPini() throws IOException {
        final PropertiesWithComments props = new PropertiesWithComments();
        final File idpIni = jettyBase.resolve("start.d").resolve("idp.ini").toFile();
        final File replacementFile =
                jettyBase.resolve("start.d.dist").resolve("idp.ini.rewrite.property.names").toFile();
        try (final FileInputStream in = new FileInputStream(idpIni);
                final FileInputStream replacementStream = new FileInputStream(replacementFile)) {
            props.loadNameReplacement(replacementStream);
            props.load(in);
        }
        try (final FileOutputStream out = new FileOutputStream(idpIni)) {
            props.store(out);
        }
    }

    /** Copy one file to another.
     * @param fromFile from
     * @param toFile to
     * @throws IOException if it fails.
     */
    private void copyFile(@Nonnull final Path fromFile, @Nonnull final Path toFile) throws IOException {
        try (final FileOutputStream out = new FileOutputStream(toFile.toFile())) {
            Files.copy(fromFile, out);
        }
    }

    /** lock down the jetty base directory. */
    private void reprotect() {
        final Path libDir = jettyBase.resolve("lib");
        assert libDir!=null;
        InstallerSupport.setReadOnlyDir(libDir, false);
        InstallerSupport.setMode(libDir, "444", "*");
        final Path etcDir = jettyBase.resolve("etc");
        assert etcDir!=null;
        InstallerSupport.setReadOnlyDir(etcDir, false);
        InstallerSupport.setMode(etcDir, "444", "*");
        final Path webapps = jettyBase.resolve("webapps");
        assert webapps!=null;
        InstallerSupport.setReadOnlyDir(webapps, false);
        InstallerSupport.setMode(webapps, "444", "*");
    }

    /** Main entry.
     * @param args As supplied
     * @throws IOException if there is a problem with the jetty base.
     */
    public static void main(final String[] args) throws IOException {
        new FinalizeJettyBase().execute();
    }
}
