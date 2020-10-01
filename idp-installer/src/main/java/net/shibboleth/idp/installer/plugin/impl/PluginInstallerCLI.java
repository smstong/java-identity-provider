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

package net.shibboleth.idp.installer.plugin.impl;

import java.nio.file.Path;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import net.shibboleth.ext.spring.cli.AbstractCommandLine;
import net.shibboleth.idp.Version;
import net.shibboleth.idp.cli.AbstractIdPHomeAwareCommandLine;
import net.shibboleth.idp.installer.plugin.impl.PluginState.VersionInfo;
import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.idp.plugin.PluginSupport.SupportLevel;
import net.shibboleth.idp.plugin.PluginVersion;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Command line for Plugin Installation.
 */
public final class PluginInstallerCLI extends AbstractIdPHomeAwareCommandLine<PluginInstallerArguments> {

    /** Class logger. */
    @Nullable private Logger log;
    
    /** A Plugin Installer to use. */
    @Nullable private PluginInstaller installer;

    /**
     * Constrained Constructor.
     */
    private PluginInstallerCLI() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected Logger getLogger() {
        if (log == null) {
            log = LoggerFactory.getLogger(PluginInstallerCLI.class);
        }
        return log;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected Class<PluginInstallerArguments> getArgumentClass() {
        return PluginInstallerArguments.class;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected String getVersion() {
        return Version.getVersion();
    }
    
    /** {@inheritDoc} */
    @Nonnull @NonnullElements protected List<Resource> getAdditionalSpringResources() {
        return List.of(
               new ClassPathResource("net/shibboleth/idp/conf/http-client.xml"));
    }
    
    /** {@inheritDoc} */
    //CheckStyle: CyclomaticComplexity OFF
    protected int doRun(final PluginInstallerArguments args) {
        
        if (args.getHttpClientName() == null) {
            args.setHttpClientName("shibboleth.InternalHttpClient");
        }

        final int ret = super.doRun(args);
        if (ret != RC_OK) {
            return ret;
        }
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        try (final PluginInstaller inst = new PluginInstaller()){
            constructPluginInstaller(inst, args);

            switch (args.getOperation()) {
                case LIST:
                    doList(args.isFullList(), args.getPluginId());
                    break;

                case INSTALLDIR:
                    if (args.getPluginId() != null) {
                        installer.setPluginId(args.getPluginId());
                    }
                    installer.installPlugin(args.getInputDirectory(), args.getInputFileName());
                    break;

                case INSTALLREMOTE:
                    if (args.getPluginId() != null) {
                        installer.setPluginId(args.getPluginId());
                    }
                    installer.installPlugin(args.getInputURL(), args.getInputFileName());
                    break;

                case UPDATE:
                    doUpdate(args.getPluginId() , args.getUpdateVersion());
                    break;

                case REMOVEJARS:
                    installer.setPluginId(args.getPluginId());
                    installer.removeJars();
                    break;

                default:
                    getLogger().error("Invalid operation");
                    return RC_INIT;
            }

        } catch (final BeansException e) {
            getLogger().error("Plugin Install failed", e);
            return RC_INIT;
        } catch (final ComponentInitializationException | BuildException e) {
            getLogger().error("Plugin Install failed:", e);
            return RC_IO;
        }
        return ret;
    }
    //CheckStyle: CyclomaticComplexity OM

    /** Build the installer.
     * @param inst the newly created installed
     * @param args the arguments
     * @throws ComponentInitializationException as required
     */
    private void constructPluginInstaller(final PluginInstaller inst,
            final PluginInstallerArguments args) throws ComponentInitializationException {
        inst.setIdpHome(Path.of(getApplicationContext().getEnvironment().getProperty("idp.home")));
        if (!args.isUnattended()) {
            inst.setAcceptCert(new InstallerQuery("Accept this Certificate"));
            inst.setAcceptDownload(new InstallerQuery("Download from"));
        }
        inst.setTrustore(args.getTruststore());
        if (getHttpClient()!= null) {
            inst.setHttpClient(getHttpClient());
        }
        inst.initialize();
        installer = inst;
    }
    
    /** Print our more information about a plugin.
     * Helper method for {@link #doList(boolean, String)}
     * @param plugin what we are interested in.
     */
    private void printDetails(final IdPPlugin plugin) {
        log.debug("Interrogating {} ", plugin.getPluginId());
        final PluginState state =  new PluginState(plugin);
        if (getHttpClient() != null) {
            state.setHttpClient(getHttpClient());
        }
        try {
            state.initialize();
        } catch (final ComponentInitializationException e) {
            log.error("Could not interrogate plugin {}", plugin.getPluginId(), e);
            return;
        }
        final Map<PluginVersion, VersionInfo> versions = state.getAvailableVersions();
        System.out.println("\tVersions ");
        for (final Entry<PluginVersion, VersionInfo> entry  : versions.entrySet()) {
            final String downLoadDetails;
            if (state.getUpdateBaseName(entry.getKey()) == null || state.getUpdateURL(entry.getKey())==null ) {
                downLoadDetails = " - No download available";
            } else {
                downLoadDetails = "";
            }
            System.out.println(String.format("\t%s:\tMin=%s\tMax=%s\tSupport level: %s%s",
                    entry.getKey(),
                    entry.getValue().getMinSupported(),
                    entry.getValue().getMaxSupported(),
                    entry.getValue().getSupportLevel(),
                    downLoadDetails));
        }
    }

    /** List all installed plugins (or just one if provided).
     * @param fullList whether to do full deatils
     * @param pluginId the pluginId or null.
     */
    private void doList(final boolean fullList, @Nullable final String pluginId) {
        boolean list = false;
        final List<IdPPlugin> plugins = installer.getInstalledPlugins();
        for (final IdPPlugin plugin: plugins) {
            if (pluginId == null || pluginId.equals(plugin.getPluginId())) {
                list = true;
                System.out.println(String.format("Plugin: %-22s\tCurrent Version: %d.%d.%d",
                       plugin.getPluginId(),
                       plugin.getMajorVersion(),plugin.getMinorVersion(), plugin.getPatchVersion()));
                if (fullList) {
                    printDetails(plugin);
                }
            }
        }
        if (!list) {
            if (pluginId == null) {
                System.out.println("No plugins installed");
            } else {
                System.out.println("Plugin " + pluginId + " not installed");
            }
        }
    }

    /** Find the best update version.  Helper function for {@linkplain #doUpdate(String, PluginVersion)}.
     * @param plugin The Plugin
     * @param state all about the plugin
     * @return the best version (or null)
     */
    @Nullable private PluginVersion getBestVersion(final IdPPlugin plugin, final PluginState state) {

        final String idpVersionString = net.shibboleth.idp.Version.getVersion();
        final PluginVersion myVersion = new PluginVersion(plugin.getMajorVersion(),
                plugin.getMinorVersion(), plugin.getPatchVersion());

        final PluginVersion idPVersion;
        if (idpVersionString == null) {
            idPVersion = new PluginVersion(4,1,0);
            log.error("Could not determine IdP Version.  Assuming 4.1.0");
        } else {
            idPVersion = new PluginVersion(idpVersionString);
        }

        final List<PluginVersion> availableVersions = new ArrayList<>(state.getAvailableVersions().keySet());
        availableVersions.sort(null);
        log.debug("Considering versions {}", availableVersions);

        for (int i = availableVersions.size()-1; i >= 0; i--) {
            final PluginVersion version = availableVersions.get(i);
            if (version.compareTo(myVersion) <= 0) {
                log.debug("Version {} is less than or the same as {}. All done", version, myVersion);
                return null;
            }
            final VersionInfo versionInfo = state.getAvailableVersions().get(version);
            if (versionInfo.getSupportLevel() != SupportLevel.Current) {
                log.debug("Version {} has suppprt level {}, ignoring", version, versionInfo.getSupportLevel());
                continue;
            }
            if (!state.isSupportedWithIdPVersion(version, idPVersion)) {
                log.debug("Version {} is not supported with idpVersion {}", version, idPVersion);
                continue;
            }
            log.debug("Version {} is supported with idpVersion {}", version, idPVersion);
            if (state.getUpdateURL(version) == null || state.getUpdateBaseName(version) == null) {
                log.debug("Version {} is does not have update information", version);
                continue;
            }
            return version;
        }
        return null;
    }

    /** Update the plugin.
     * @param pluginId the pluginId or null.
     * @param pluginVersion (optionally) the version to update to.
     */
    private void doUpdate(@Nonnull final String pluginId, @Nullable final PluginVersion pluginVersion) {
        final List<IdPPlugin> plugins = installer.getInstalledPlugins();
        for (final IdPPlugin plugin: plugins) {
            if (pluginId.equals(plugin.getPluginId())) {
                log.debug("Interrogating {} ", plugin.getPluginId());
                final PluginState state =  new PluginState(plugin);
                if (getHttpClient() != null) {
                    state.setHttpClient(getHttpClient());
                }
                try {
                    state.initialize();
                } catch (final ComponentInitializationException e) {
                    log.error("Could not interrogate plugin {}", plugin.getPluginId(), e);
                    return;
                }
                final PluginVersion installVersion;
                if (pluginVersion == null) {
                    installVersion = getBestVersion(plugin, state);
                    if (installVersion == null) {
                        log.info("No Suitable update version available");
                        break;
                    }
                } else {
                    installVersion = pluginVersion;
                    final Map<PluginVersion, VersionInfo> versions = state.getAvailableVersions();
                    if (!versions.containsKey(installVersion)) {
                        log.error("Specified version {} could not be found.  Available versions {}",
                                installVersion, versions.keySet());
                        return;
                    }
                }
                // just use the tgz version - its an update so it should be jar files only
                installer.installPlugin(state.getUpdateURL(installVersion),
                        state.getUpdateBaseName(installVersion) + ".tar.gz");
            }
        }
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
        final PluginInstallerCLI cli = new PluginInstallerCLI();

        return cli.run(args);
    }
    
    /**
     * CLI entry point.
     * @param args arguments
     */
    public static void main(@Nonnull final String[] args) {
        System.exit(runMain(args));
    }

    /** Predicate to ask the user if they want to install the trust store provided. */
    private static class InstallerQuery implements Predicate<String> {

        /** What to say. */
        @Nonnull
        private final String promptText;

        /**
         * Constructor.
         * @param text What to say before the prompt information
         */
        public InstallerQuery(@Nonnull final String text) {
            promptText = Constraint.isNotNull(text, "Text should not be null");
        }

        /** {@inheritDoc} */
        public boolean test(final String certString) {
            System.console().printf("%s:\n%s [yN] ", promptText, certString);
            System.console().flush();
            final String result  = StringSupport.trimOrNull(System.console().readLine());
            return result != null && "y".equalsIgnoreCase(result.substring(0, 1));
        }
    }
}
