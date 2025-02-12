// CheckStyle:  FileLength|Header OFF
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

package net.shibboleth.idp.installer.plugin.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.tools.ant.BuildException;
import org.opensaml.security.httpclient.HttpClientSecurityContextHandler;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.slf4j.Logger;

import net.shibboleth.idp.Version;
import net.shibboleth.idp.installer.InstallerSupport;
import net.shibboleth.idp.installer.impl.BuildWar;
import net.shibboleth.idp.installer.plugin.impl.TrustStore.Signature;
import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.profile.installablecomponent.InstallableComponentVersion;
import net.shibboleth.profile.module.Module.ModuleResource;
import net.shibboleth.profile.module.Module.ResourceResult;
import net.shibboleth.profile.module.ModuleContext;
import net.shibboleth.profile.module.ModuleContext.OperationType;
import net.shibboleth.profile.module.ModuleException;
import net.shibboleth.profile.plugin.Plugin.Package;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.PredicateSupport;
import net.shibboleth.shared.primitive.DeprecationSupport;
import net.shibboleth.shared.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.shared.spring.httpclient.resource.HTTPResource;

/**
 *  The class where the heavy lifting of managing a plugin happens. 
 */
public final class PluginInstaller extends AbstractInitializableComponent implements AutoCloseable {

    /** Class logger. */
    @Nonnull private static final Logger LOG = LoggerFactory.getLogger(PluginInstaller.class);

    /** Property Name for version. */
    @Nonnull @NotEmpty private static final String PLUGIN_VERSION_PROPERTY ="idp.plugin.version";

    /** Property Prefix for install files . */
    @Nonnull @NotEmpty private static final String PLUGIN_FILE_PROPERTY_PREFIX = "idp.plugin.file.";

    /** Property Name for whether paths are relative. */
    @Nonnull @NotEmpty private  static final String PLUGIN_RELATIVE_PATHS_PROPERTY = "idp.plugin.relativePaths";

    /** Where we are installing to. */
    @NonnullAfterInit private Path idpHome;
    
    /** What we are dealing with. */
    private String pluginId;
    
    /** Where we have unpacked into. */
    private Path unpackDirectory;
    
    /** Where we have downloaded. */
    private Path downloadDirectory;
    
    /** The plugin's story about itself. */
    private IdPPlugin description;

    /** The callback before we install a key into the TrustStore. */
    @Nonnull private Predicate<String> acceptKey = PredicateSupport.alwaysFalse();

    /** The actual distribution. */
    private Path distribution;
    
    /** Where to get the keys from if not defaulted. */
    private String truststore;

    /** What to use to download things. */
    @Nonnull private final HttpClient httpClient;

    /** If overridden these are the urls to us for update (rather than what the plugin asks for. */
    @Nonnull private List<URL> updateOverrideURLs = CollectionSupport.emptyList();

    /** Dumping space for renamed files. */
    @NonnullAfterInit private Path workspacePath;

    /** DistDir. */
    @NonnullAfterInit private Path distPath;

    /** Pluginss webapp. */
    @NonnullAfterInit private Path pluginsWebapp;

    /** Plugins webapp. */
    @NonnullAfterInit private Path pluginsContents;

    /** The absolute paths of what was installed - this is setup by {@link #loadCopiedFiles()}. */
    @Nullable private List<Path> installedContents;

    /** The version from the contents file, or null if it isn't loaded. */
    @Nullable private String installedVersionFromContents;

    /** The Module Context. */
    @NonnullAfterInit private ModuleContext moduleContext;

    /** The names of those plugin modules enabled by rule. */
    @Nonnull private Set<String> reenabledPluginModules = new HashSet<>();

    /** Module Changes.*/
    @Nonnull private final  Map<ModuleResource,ResourceResult> moduleChanges = new HashMap<>();

    /** The "plugins" classpath loader. AutoClosed. */
    @Nullable private URLClassLoader installedPluginsLoader;

    /** The "plugin under construction" classpath loader. AutoClosed. */
    @Nullable private URLClassLoader installingPluginLoader;

    /** The securityParams for the module context. */
    @Nullable private HttpClientSecurityParameters securityParams;

    /** Do we rebuild? */
    private boolean rebuildWar = true;

    /**
     * Constructor.
     * @param client - the HttpClient to use
     */
    public PluginInstaller(@Nonnull final HttpClient client) {
        httpClient = client;
    }

    /** Set IdP Home.
     * @param home Where we are working from
     */
    public void setIdpHome(@Nonnull final Path home) {
        checkSetterPreconditions();
        idpHome = Constraint.isNotNull(home, "IdPHome should be non-null");
    }

    /** Get a null safe idpHome.
     * @return idpHome
     */
    @Nonnull private Path getIdpHome() {
        assert idpHome!=null;
        return idpHome;
    }

    /** Set the plugin id.
     * @param id what to set.
     */
    public void setPluginId(@Nonnull @NotEmpty final String id) {
        pluginId = Constraint.isNotNull(StringSupport.trimOrNull(id), "Plugin id should be be non-null");
    }

    /** Set the truststore.
     * @param loc what set.
     */
    public void setTrustore(@Nullable final String loc) {
        truststore = StringSupport.trimOrNull(loc);
    }

    /** Set the acceptKey predicate.
     * @param what what to set.
     */
    public void setAcceptKey(@Nonnull final Predicate<String> what) {
        acceptKey = Constraint.isNotNull(what, "Accept Key Predicate should be non-null");
    }

    /** Set the override URLS.
     * @param urls The updateOverrideURLs to set.
     */
    public void setUpdateOverrideURLs(@Nonnull final List<URL> urls) {
        updateOverrideURLs = Constraint.isNotNull(urls, "Override URLS must be non null");
    }

    /** Set the Module Context security parameters.
     * @param params what to set.
     */
    public void setModuleContextSecurityParams(@Nullable final HttpClientSecurityParameters params) {
        checkSetterPreconditions();
        securityParams =  params;
    }

    /** Set whether  we rebuild the war.
     * @param what - whether we will or not
     */
    public void setRebuildWar(final boolean what) {
        rebuildWar = what;
    }

    /** Do we rebuild the war?
     * @return true if we are going to.
     */
    public boolean isRebuildWar() {
        return rebuildWar;
    }

    /** Install the plugin from the provided URL.  Involves downloading
     *  the file and then doing a {@link #installPlugin(Path, String, boolean)}.
     * @param baseURL where we get the files from
     * @param fileName the name
     * @param checkVersion do we want to check vs the IdP Version?
     * @throws BuildException if badness is detected.
     */
    public void installPlugin(@Nonnull final URL baseURL,
                              @Nonnull @NotEmpty final String fileName,
                              final boolean checkVersion) throws BuildException {
        download(baseURL, fileName);
        assert downloadDirectory != null;
        installPlugin(downloadDirectory, fileName, checkVersion);
    }

    /** Install the plugin from a local path.
     * <ul><li> Check signature</li>
     * <li>Unpack to temp folder</li>
     * <li>Install from the folder</li></ul>
     * @param base the directory where the files are
     * @param fileName the name
     * @param checkVersion do we want to check vs the IdP Version?
     * @throws BuildException if badness is detected.
     */
    public void installPlugin(@Nonnull final Path base,
                              @Nonnull @NotEmpty final String fileName,
                              final boolean checkVersion) throws BuildException {
        if (!Files.exists(base.resolve(fileName))) {
            LOG.error("Could not find distribution {}", base.resolve(fileName));
            throw new BuildException("Could not find distribution");
        }
        if (!Files.exists(base.resolve(fileName + ".asc"))) {
            LOG.error("Could not find distribution {}", base.resolve(fileName + ".asc"));
            throw new BuildException("Could not find signature for distribution");
        }

        unpack(base, fileName);
        setupPluginId();
        checkSignature(base, fileName);
        setupDescriptionFromDistribution();
        //
        // the above line guarantees a non null description
        //
        if (checkVersion) {
            final PluginState state = new PluginState(getDescription(), updateOverrideURLs);
            state.setHttpClient(httpClient);
            state.setHttpClientSecurityParameters(securityParams);
            try {
                state.initialize();
            } catch (final ComponentInitializationException e) {
               throw new BuildException(e);
            }
            final InstallableComponentVersion pluginVersion = new InstallableComponentVersion(getDescription());
            final InstallableComponentVersion idpVersion = getIdPVersion();
            if (!state.getPluginInfo().isSupportedWithIdPVersion(pluginVersion, idpVersion)) {
                LOG.error("Plugin {} version {} is not supported with IdP version {}",
                        pluginId, pluginVersion, idpVersion);
                throw new BuildException("Version Mismatch");
            }
        }
        LOG.info("Installing Plugin '{}' version {}.{}.{}", pluginId,
                getDescription().getMajorVersion(),getDescription().getMinorVersion(),
                getDescription().getPatchVersion());

        final Set<String> loadedModules = getLoadedModules();
        if (getVersionFromContents() == null) {
            getModuleContext().setOperationType(OperationType.Install);
        } else {
            getModuleContext().setOperationType(OperationType.Upgrade);
        }
        getModuleContext().setPluginId(pluginId);
        try (final RollbackPluginInstall rollBack = new RollbackPluginInstall(getModuleContext(), moduleChanges)) {
            uninstallOld(rollBack);

            checkRequiredModules(loadedModules);
            handlePackages(rollBack);
            installNew(rollBack);
            reEnableModules(loadedModules);

            saveCopiedFiles(rollBack.getFilesCopied());
            rollBack.completed();
        }

        if (isRebuildWar()) {
            final BuildWar builder = new BuildWar(getIdpHome());
            builder.execute();
        } else {
            LOG.info("WAR file not rebuilt.");
        }
        emitModuleChanges();
    }

    /** Remove the jars for this plugin and rebuild the war.
     * @throws BuildException if badness occurs. */
    public void uninstall() throws BuildException {

        String moduleId = null;
        final String pId = pluginId;
        assert pId != null;
        getModuleContext().setPluginId(pId);
        getModuleContext().setOperationType(OperationType.Uninstall);
        description = getInstalledPlugin(pId);
        if (description == null) {
            LOG.warn("Description for {} not found", pluginId);
        } else {
            try (final RollbackPluginInstall rollback = new RollbackPluginInstall(getModuleContext(), moduleChanges)){
                for (final IdPModule module: getDescription().getDisableOnRemoval()) {
                    moduleId = module.getId();
                    captureChanges(module.disable(getModuleContext(), false));
                    rollback.getModulesDisabled().add(module);
                }
                rollback.completed();
            } catch (final ModuleException e) {
                LOG.error("Uninstalling {}. Could not disable {}", pluginId, moduleId, e);
                LOG.error("Fix this and rerun");
                throw new BuildException(e);
            }
        }
        if (getVersionFromContents() == null) {
            LOG.warn("Installed contents for {} not found", pluginId);
        } else {
            for (final Path content: getInstalledContents()) {
                assert content!=null;
                if (!Files.exists(content)) {
                    continue;
                }
                try {
                    InstallerSupport.setReadOnly(content, false);
                    Files.deleteIfExists(content);
                } catch (final IOException e) {
                    LOG.warn("Could not delete {}, deferring the delete", content.toString(), e);
                    content.toFile().deleteOnExit();
                }
            }

            if (isRebuildWar()) {
                final BuildWar builder = new BuildWar(getIdpHome());
                builder.execute();
                LOG.info("Removed resources for {} from the WAR file.", pluginId);
            } else {
                LOG.info("Removed resources for {}. WAR file not rebuilt.", pluginId);
            }
        }
        pluginsContents.resolve(pluginId).toFile().deleteOnExit();
        emitModuleChanges();
    }

    /** Get hold of the {@link IdPPlugin} for this plugin.
     * @throws BuildException if badness is happens.
     */
    private void setupDescriptionFromDistribution() throws BuildException {
       final ServiceLoader<IdPPlugin> plugins;
           plugins = ServiceLoader.load(IdPPlugin.class, getDistributionLoader());
       final Optional<IdPPlugin> first = plugins.findFirst();
       if (first.isEmpty()) {
           LOG.error("No Plugin services found in plugin distribution");
           throw new BuildException("No Plugin services found in plugin distribution");
       }
       for (final IdPPlugin plugin:plugins) {
           LOG.debug("Found Service announcing itself as {}", plugin.getPluginId() );
           if (pluginId.equals(plugin.getPluginId())) {
               description = plugin;
               return;
           }
           LOG.trace("Did not match {}", pluginId);
       }
       LOG.error("Looking in plugin distibution for a plugin called {}, but found a plugin called {}.",
              pluginId, first.get().getPluginId());
       throw new BuildException("Could not locate PluginDescription");
    }

    /** What files were installed to webapp for this plugin?
     * @return a list of the absolute paths of the installed contents,
     * may be empty if nothing is installed or the plugin didn't install anything.
     */
    @Nonnull public List<Path> getInstalledContents() {
        loadCopiedFiles();
        assert installedContents != null;
        return installedContents;
    }

    /** return the version that the contents page thinks is installed.
     * @return the version, or null if it is not found.
     */
    @Nullable public String getVersionFromContents() {
        loadCopiedFiles();
        return installedVersionFromContents;
    }
    
    /** Check for initialized and if so return the {@link #moduleContext}.
     * @return the {@link #moduleContext}.
     */
    @Nonnull private ModuleContext getModuleContext() {
        checkComponentActive();
        assert moduleContext!=null;
        return moduleContext;
    }
    
    /** Check for initialized and if so return the {@link #pluginsWebapp}.
     * @return the {@link #pluginsWebapp}.
     */
    @Nonnull private Path getPluginsWebapp() {
        checkComponentActive();
        assert pluginsWebapp!=null;
        return pluginsWebapp;
    }

    /** Check for non null and then if so return the {@link #description}.
     * @return the {@link #description}
     */
    @Nonnull private IdPPlugin getDescription() {
        Constraint.isTrue(description != null, "Invalid Plugin Id in Description");
        assert description!=null;
        return description;
    }


    /** What modules (on the installed plugins Classpath) are currently loaded?
     * @return a set of the names of the currently enabled Modules.
     * @throws BuildException on loading a module
     */
    @Nonnull public Set<String> getLoadedModules() throws BuildException {
        final @Nonnull Set<String> enablededModules = new HashSet<>();
        final Iterator<IdPModule> modules = ServiceLoader.load(IdPModule.class, getInstalledPluginsLoader()).iterator();
        while (modules.hasNext()) {
            try {
                final IdPModule module = modules.next();
                if (module.isEnabled(getModuleContext())) {
                    LOG.debug("Found Enabled Module {}",module.getId());
                    enablededModules.add(module.getId());
                }
            } catch (final ServiceConfigurationError e) {
                LOG.error("Unable to instantiate IdPModule", e);
                throw new BuildException(e);
            }
        }
        return enablededModules;
    }

    /**
     * Police that required modules for plugin installation are enabled.
     * @param loadedModules the modules we know to be enabled
     * @throws BuildException if any required modules are missing or disabled
     */
    private void checkRequiredModules(@Nonnull final Set<String> loadedModules) throws BuildException  {
        for (final String moduleId: getDescription().getRequiredModules()) {
            if (!loadedModules.contains(moduleId)) {
                LOG.warn("Required module {} is missing or not enabled ", moduleId);
                final Pair<String, String> pluginInfo = getModulePluginInfo(moduleId);
                if (pluginInfo != null) {
                    LOG.warn("Module {} provided by plugin {} version {}", moduleId, pluginInfo.getFirst(), pluginInfo.getSecond());
                }
                throw new BuildException("One or more required modules are not enabled");
            }
        }
    }

    /** If we have the information, get the Plugin Id and version which supplies the module id.
     * @param moduleId The module ID
     * @return a Pair of string the first being the plugin ID and the second its version
     */
    private Pair<String, String> getModulePluginInfo(final String moduleId) throws BuildException {
        try {
            final List<URL> sources = description.getModuleInfoSources();
            Properties props = null;
            for (final URL url : sources) {
                LOG.debug("Loading info from {}", url);
                if (url == null) {
                    continue;
                }
                try {
                    final HTTPResource httpResource = new HTTPResource(httpClient, url);
                    final HttpClientSecurityContextHandler handler = new HttpClientSecurityContextHandler();
                    handler.setHttpClientSecurityParameters(securityParams);
                    handler.initialize();
                    httpResource.setHttpClientContextHandler(handler);
                    props = new Properties();
                    props.load(httpResource.getInputStream());
                    LOG.debug("Loaded {} properties", props.size());
                    break;
                } catch (final IOException e) {
                    LOG.error("Could not open Module Resource at {} :", url, e);
                    continue;
                }
            }
            //
            // We got properties or we didn't
            //
            if (props == null || props.isEmpty()) {
                return null;
            }
            final String version = props.getProperty(moduleId + ".version");
            final String plugin =  props.getProperty(moduleId + ".plugin");
            LOG.debug("Looked up {}, found {}, {}", moduleId, version, plugin);
            return new Pair<>(plugin, version);
        } catch (final IOException | ComponentInitializationException  e) {
            LOG.error("Could not lookup up infomationa about {}, continuing", moduleId, e);
            return null;
        }
    }

    /** Re-enabled the listed modules iff then are implemented by the plugin we just installed.
     * @param loadedModules the modules to enable
     * @throws BuildException on errors finding or enabling the modules
     */
    private void reEnableModules(final Set<String> loadedModules) throws BuildException  {
        try {
            final Iterator<IdPModule> modules = ServiceLoader.load(IdPModule.class, getDistributionLoader()).iterator();
            LOG.debug("Reloading Plugin-supplied modules (if any)");
            while (modules.hasNext()) {
                final IdPModule module = modules.next();
                if (pluginId.equals(module.getOwnerId())) {
                    // IdP-2340
                    final String moduleId = module.getId();
                    if (reenabledPluginModules.contains(moduleId)) {
                        LOG.debug("Module {} already enabled by Plugin rule", moduleId);
                    } else if (!loadedModules.contains(module.getId())) {
                        LOG.debug("Module {} not previously enabled", moduleId);
                    } else {
                        LOG.debug("Re-enabling module {}", moduleId);
                        captureChanges(module.enable(getModuleContext(), true));
                    }
                } else {
                    LOG.debug("Module {}, not provided by this plugin", module.getId());
                }
            }
        } catch (final ServiceConfigurationError | ModuleException e) {
            LOG.error("Unable to instantiate IdPModule", e);
            throw new BuildException(e);
        }
    }

    /** Deploy any required packages.
     *
     * @param rollback Roll Back Context
     * @throws BuildException if badness is detected.
     */
    private void handlePackages(@Nonnull final RollbackPluginInstall rollback) throws BuildException {
        final Path renameTarget = workspacePath.resolve("dirRollback");
        assert renameTarget!= null;
        for (final Package pack: getDescription().getPackages()) {

            if (SystemUtils.IS_OS_WINDOWS && !pack.isWindows()) {
                LOG.debug("Package: skipping non-Windows resource {}", pack.getDestinationName());
                continue;
            }
            if (!SystemUtils.IS_OS_WINDOWS && !pack.isNonWindows()) {
                LOG.debug("Package: skipping Windows resource {}", pack.getDestinationName());
                continue;
            }

            try {
                final Path to = idpHome.resolve(pack.getDestinationName());
                assert to != null;
                if (!to.startsWith(idpHome.toString())) {
                    LOG.error("Package {} attempted to create file outside of IdP installation: {}", pack.getDestinationName(), to);
                    throw new BuildException("Plugin package asked to create file outside of IdP installation");
                }
                
                if (Files.exists(to)) {
                    InstallerSupport.renameToTree(getIdpHome(),
                                                  renameTarget,
                                                  CollectionSupport.singletonList(to),
                                                  rollback.getFilesRenamedAway());
                }
                final Path fromPackage = distribution.resolve(pack.getSourceName());
                assert fromPackage != null;
                unpack(to, fromPackage, isZip(pack.getSourceName()), pack.isStripTopLevelDir());
            } catch (final IOException e) {
                LOG.error("Unable to handle package : {} ", pack.getDestinationName(), e);
                throw new BuildException(e);
            }
        }
    }

    /** Copy the webapp folder from the distribution to the per plugin
     * location inside dist.
     * @param rollBack Roll Back Context
     * @throws BuildException if badness is detected.
     */
    private void installNew(@Nonnull final RollbackPluginInstall rollBack) throws BuildException {
        final Path from = distribution.resolve("webapp");
        assert from != null;
        if (InstallerSupport.detectDuplicates(from, getPluginsWebapp())) {
            throw new BuildException("Install would overwrite files");
        }
        InstallerSupport.copyWithLogging(from, getPluginsWebapp(), rollBack.getFilesCopied());

        String moduleId = null;
        try {
            for (final IdPModule module: getDescription().getEnableOnInstall()) {
                moduleId = module.getId();
                if (!module.isEnabled(getModuleContext())) {
                    LOG.debug("Enabling Module {}", moduleId);
                    captureChanges(module.enable(getModuleContext(), false));
                    rollBack.getModulesEnabled().add(module);
                } else {
                    LOG.debug("Re-enabling Module {}", moduleId);
                    captureChanges(module.enable(getModuleContext(), true));
                }
                reenabledPluginModules.add(moduleId);
            }
        } catch (final ModuleException e) {
            LOG.error("Error enabling {}", moduleId);
            throw new BuildException(e);
        }
    }

    /** Uninstall the old version of the plugin.
     * @param rollback Rollback Context
     * @throws BuildException on IO or module errors */
    private void uninstallOld(@Nonnull final RollbackPluginInstall rollback) throws BuildException {

        final String oldVersion =getVersionFromContents(); 
        if (oldVersion == null) {
            LOG.debug("{} not installed. files renamed", pluginId);
        } else {
            try {
                final Path rollbackDir = workspacePath.resolve("rollback");
                assert rollbackDir != null;
                LOG.debug("Uninstalling version {} of {}", oldVersion, pluginId);
                InstallerSupport.renameToTree(getPluginsWebapp(),
                        rollbackDir,
                        getInstalledContents(),
                        rollback.getFilesRenamedAway());
            } catch (final IOException e) {
                LOG.error("Error uninstalling plugin", e);
                throw new BuildException(e);
            }
        }
    }

    /** Stream the copy list to a property file and empty it.
     * @param copiedFiles The copied files
     * @throws BuildException If we hit an IO exception
     */
    private void saveCopiedFiles(@Nonnull final List<Path> copiedFiles) throws BuildException {
        try {
            Files.createDirectories(pluginsContents);
            final Properties props = new Properties(1+copiedFiles.size());
            props.setProperty(PLUGIN_VERSION_PROPERTY, new InstallableComponentVersion(getDescription()).toString());
            props.setProperty(PLUGIN_RELATIVE_PATHS_PROPERTY, "true");
            int count = 1;
            for (final Path p: copiedFiles) {
                final Path relPath = getIdpHome().relativize(p);
                props.setProperty(PLUGIN_FILE_PROPERTY_PREFIX+Integer.toString(count++), relPath.toString());
            }
            final File outFile = pluginsContents.resolve(pluginId).toFile();
            try (final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile))) {
                props.store(out, "Files Copied "  + Instant.now());
            }
        } catch (final IOException e) {
            LOG.error("Error saving list of copied files.", e);
            throw new BuildException(e);
        }
    }

    /** Infer where the properties were installed to.
     * @param props The property files
     * @return the idpHome it was installed to or null if no files installed
     */
    @Nullable private Path inferInstalledIdpHome(@Nonnull final Properties props) {
        if (props.get(PLUGIN_FILE_PROPERTY_PREFIX+"1") == null) {
            // No files
            return null;
        }
        int count = 1;
        LOG.debug("Inferring IdP Home");
        String val = props.getProperty(PLUGIN_FILE_PROPERTY_PREFIX+Integer.toString(count++));
        while (val != null) {
            LOG.debug("Looking at {}", val);
            int index = val.indexOf("/dist/plugin-webapp/");
            if (index < 0) {
                // try windows
                index = val.indexOf("\\dist\\plugin-webapp\\");
            }
            if (index >= 0) {
                final String s = val.substring(0, index);
                if (getIdpHome().toString().equals(s)) {
                    LOG.debug("Inferred install to {}", s);
                } else {
                    LOG.info("Inferred initial install to {}", s);
                }
                assert s != null;
                return InstallerSupport.pathOf(s);
            }
            val = props.getProperty(PLUGIN_FILE_PROPERTY_PREFIX+Integer.toString(count++));
        }
        LOG.error("Could no infer IDPHOME from previous contents");
        return null;
    }

    /** Load the contents for this plugin from the properties file used during
     * installation.
     * @throws BuildException if the load fails
     */
    private void loadCopiedFiles() throws BuildException {
        if (installedContents != null) {
            return;
        }
        final Properties props = new Properties();
        final File inFile = pluginsContents.resolve(pluginId).toFile();
        if (!inFile.exists()) {
            LOG.debug("Contents file for plugin {} ({}) does not exist", pluginId, inFile.getAbsolutePath());
            installedContents = CollectionSupport.emptyList();
            return;
        }
        try (final BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(inFile))) {
            props.load(inStream);
        } catch (final IOException e) {
            LOG.error("Error loading list of copied files from {}.", inFile, e);
            throw new BuildException(e);
        }
        LOG.debug("Property file {}", props);
        final List<Path> result = new ArrayList<>(props.size());
        installedVersionFromContents = StringSupport.trimOrNull(props.getProperty(PLUGIN_VERSION_PROPERTY));
        final boolean relativePaths = props.get(PLUGIN_RELATIVE_PATHS_PROPERTY) != null;
        final Path installedIdPHome;
        if (relativePaths) {
            installedIdPHome = null;
        } else {
            installedIdPHome = inferInstalledIdpHome(props);
        }
        int count = 1;
        String val = props.getProperty(PLUGIN_FILE_PROPERTY_PREFIX+Integer.toString(count++));
        while (val != null) {
            final Path valAsPath = InstallerSupport.pathOf(val);
            if (relativePaths || installedIdPHome == null) {
                result.add(getIdpHome().resolve(valAsPath));
            } else {
                final Path relPath = installedIdPHome.relativize(valAsPath);
                final Path newPath = getIdpHome().resolve(relPath);
                result.add(newPath);
            }
            val = props.getProperty(PLUGIN_FILE_PROPERTY_PREFIX+Integer.toString(count++));
        }
        installedContents = result;
    }

    /** Method to download a zip file to the {{@link #downloadDirectory}.
     * @param baseURL Where the zip/tgz and signature file is
     * @param fileName the name.
     * @throws BuildException if badness is detected.
     */
    private void download(@Nonnull final URL baseURL, @Nonnull final String fileName) throws BuildException {
        try {
            final Path dir = downloadDirectory = Files.createTempDirectory("plugin-installer-download");
            assert dir != null;
            final HTTPResource baseResource = new HTTPResource(httpClient, baseURL);
            final HttpClientSecurityContextHandler handler = new HttpClientSecurityContextHandler();
            handler.setHttpClientSecurityParameters(securityParams);
            handler.initialize();
            baseResource.setHttpClientContextHandler(handler);
            InstallerSupport.download(baseResource, handler, dir,  fileName);
            InstallerSupport.download(baseResource, handler, dir,  fileName + ".asc");
        } catch (final IOException | ComponentInitializationException e) {
            LOG.error("Error in download", e);
            throw new BuildException(e);
        }
    }

    /** Capture module changes.
     * @param changes what has changed */
    private void captureChanges(@Nonnull final Map<ModuleResource,ResourceResult> changes) {
        for (final Entry<ModuleResource, ResourceResult> entry: changes.entrySet()) {
            moduleChanges.put(entry.getKey(), entry.getValue());
        }
    }

    /** Emit module changes. */
    private void emitModuleChanges() {
        if (!moduleChanges.isEmpty()) {
            LOG.info("Module file changes as a result of this install");
            moduleChanges.forEach(this::doReportOperation);
        }
    }

    /**
     * Report on a resource result.
     *
     * @param resource resource
     * @param result result of operation
     */
    private void doReportOperation(@Nonnull final ModuleResource resource, @Nonnull final ResourceResult result) {
        final String dest = resource.getDestination().toString();
        switch (result) {
            case CREATED:
                LOG.info("\t{} created", dest);
                break;

            case REPLACED:
                LOG.info("\t{} replaced, {}.idpsave created", dest, dest);
                break;

            case ADDED:
                LOG.info("\t{}.idpnew created", dest);
                break;

            case REMOVED:
                LOG.info("\t{} removed", dest);
                break;

            case SAVED:
                LOG.info("\t{} renamed to, {}.idpsave", dest, dest);
                break;

            case MISSING:
                LOG.info("\t{} missing, nothing to do", dest);
                break;

            default:
        }
    }

    /** Helper Method for unpacking the distribution.
     * Populates {@link #unpackDirectory} and{@link #distribution}
     * @param base Where the zip/tgz file is
     * @param fileName the name.
     * @throws BuildException if badness is detected.
     */
    private void unpack(@Nonnull final Path base, @Nonnull final String fileName) throws BuildException {
        final Path fullName = base.resolve(fileName);
        assert fullName!=null;

        Constraint.isNull(unpackDirectory, "cannot unpack multiple times");
        try {
            final Path upDir = unpackDirectory = Files.createTempDirectory("plugin-installer-unpack");
            assert upDir != null;
            final boolean isZip = isZip(fileName);
            if (isZip) {
                DeprecationSupport.warnOnce(ObjectType.CLI_OPTION, "zip Plugin Distributions", null, "tar.gz distribution");
            }
            unpack(upDir, fullName, isZip, false);
            try (final DirectoryStream<Path> unpackDirStream = Files.newDirectoryStream(upDir)) {
                final Iterator<Path> contents = unpackDirStream.iterator();
                if (!contents.hasNext()) {
                    LOG.error("No contents unpacked from {}", fullName);
                    throw new BuildException("Distro was empty");
                }
                final Path next = contents.next();
                assert next != null;
                distribution  = InstallerSupport.canonicalPath(next);
                if (contents.hasNext()) {
                    LOG.error("Too many packages in distributions {}", fullName);
                    throw new BuildException("Too many packages in distributions");
                }
            }
        } catch (final IOException e) {
            throw new BuildException(e);
        }
    }
    
    /** Method to unpack a zip or tgz file into our {{@link #unpackDirectory}.
     * @param unpackTo Where to unpack to
     * @param fullName what to unzip
     * @param isZip is this a zip file?
     * @param stripFirst do we remove the first "directory" in the path (allows install in place)
     * @throws BuildException if badness is detected.
     */
    private static void unpack(@Nonnull final Path unpackTo, @Nonnull final Path fullName, final boolean isZip, boolean stripFirst) throws BuildException {
        LOG.debug("Unpacking {} to {}", fullName, unpackTo);

        try (final ArchiveInputStream<?> inStream = getStreamFor(fullName, isZip)) {
            ArchiveEntry entry = null;
            while ((entry = inStream.getNextEntry()) != null) {
                if (!inStream.canReadEntryData(entry)) {
                    LOG.warn("Could not read next entry from {}", inStream);
                    continue;
                }
                Path fromPath = unpackTo;
                if (stripFirst) {
                    final String names[] = entry.getName().split("/");
                    for (int i = 1; i < names.length; i++) {
                        fromPath = fromPath.resolve(names[i]);
                    }
                } else {
                    fromPath = fromPath.resolve(entry.getName());
                }
                final File output = unpackTo.resolve(fromPath).toFile();
                LOG.trace("Unpacking {} to {}", entry.getName(), output);
                if (entry.isDirectory()) {
                    if (!output.isDirectory() && !output.mkdirs()) {
                        LOG.error("Failed to create directory {}", output);
                        throw new BuildException("failed to create unpacked directory");
                    }
                } else {
                    final File parent = output.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        LOG.error("Failed to create parent directory {}", parent);
                        throw new BuildException("failed to create unpacked directory");
                    }
                    try (OutputStream outStream = Files.newOutputStream(output.toPath())) {
                        IOUtils.copy(inStream, outStream);
                    }
                }
            }
        } catch (final IOException e) {
            throw new BuildException(e);
        }
    }
    
    /** does the file name end in .zip?
     * @param fileName the name to consider
     * @return true if it ends with .zip
     * @throws BuildException if the name is too short
     */
    private static boolean isZip(@Nonnull final String fileName) throws BuildException {
        if (fileName.length() <= 7) {
            LOG.error("Improbably small file name: {}", fileName);
            throw new BuildException("Improbably small file name");
        }
        if (".zip".equalsIgnoreCase(fileName.substring(fileName.length()-4))) {
            return true;
        }
        if (!".tar.gz".equalsIgnoreCase(fileName.substring(fileName.length()-7))) {
            LOG.warn("FileName {} did not end with .zip or .tar.gz, assuming tar-gz", fileName);
        }
        return false;
    }

    /** Create the correct {@link ArchiveInputStream} for the input.
     * @param fullName the path of the zip file to unpack.
     * @param isZip if true then this is a zip file, otherwise a tgz file
     * @return the the appropriate  {@link ArchiveInputStream} 
     * @throws IOException  if we trip over an unpack
     */
    @Nonnull private static ArchiveInputStream<?> getStreamFor(@Nonnull final Path fullName, final boolean isZip)
            throws IOException {
        final InputStream inStream = new BufferedInputStream(new FileInputStream(fullName.toFile()));
        if (isZip) {
            return new ZipArchiveInputStream(inStream);
        }
        return new TarArchiveInputStream(new GzipCompressorInputStream(inStream));
    }

    /** Look into the distribution and suck out the plugin id.
     * @throws BuildException if badness is detected.
     */
    private void setupPluginId() throws BuildException {
        final File propertyFile = distribution.resolve("bootstrap").resolve("plugin.properties").toFile();
        if (!propertyFile.exists()) {
            LOG.error("Could not locate identity of plugin. "
                    + "Identity file 'bootstrap/plugin.properties' not present in plugin distribution.");
            throw new BuildException("Could not locate identity of plugin");
        }
        try (final InputStream inStream = new BufferedInputStream(new FileInputStream(propertyFile))) {
            final Properties idProperties = new Properties();
            idProperties.load(inStream);
            final String id = StringSupport.trimOrNull(idProperties.getProperty("plugin.id"));
            if (id == null) {
                LOG.error("Identity property file 'bootstrap/id.property' did not contain 'pluginid' property");
                throw new BuildException("No property in ID file");
            }
            if (pluginId != null && !pluginId.equals(id)) {
                LOG.error("Downloaded plugin id {} overriden by provided id {}", id, pluginId);
            } else {
                setPluginId(id);
            }
        } catch (final IOException e) {
            LOG.error("Could not load plugin identity file 'bootstrap/id.property'", e);
            throw new BuildException(e);
        }
    }

    /** Check the signature of the plugin.
     * @param base Where the zip/tgz file is
     * @param fileName the name.
     * @throws BuildException if badness is detected.
     */
    private void checkSignature(@Nonnull final Path base, @Nonnull final String fileName) throws BuildException {
        try (final InputStream sigStream = new BufferedInputStream(
                new FileInputStream(base.resolve(fileName + ".asc").toFile()))) {
            final TrustStore trust = new TrustStore();
            trust.setIdpHome(getIdpHome());
            trust.setTrustStore(truststore);
            trust.setPluginId(pluginId);
            trust.initialize();
            final Signature sig = TrustStore.signatureOf(sigStream);
            if (!trust.contains(sig)) {
                LOG.info("TrustStore does not contain signature {}", sig);
                final File keys = distribution.resolve("bootstrap").resolve("keys.txt").toFile();
                if (!keys.exists()) {
                    LOG.info("No embedded keys file, signature check fails");
                    throw new BuildException("No key found to check signiture of distribution");
                }
                try (final InputStream keysStream = new BufferedInputStream(
                        new FileInputStream(keys))) {
                    trust.importKeyFromStream(sig, keysStream, acceptKey);
                }
                if (!trust.contains(sig)) {
                    LOG.info("Key not added to Trust Store");
                    throw new BuildException("Could not check signature of distribution");
                }
            }

            try (final InputStream distroStream = new BufferedInputStream(
                new FileInputStream(base.resolve(fileName).toFile()))) {
                if (!trust.checkSignature(distroStream, sig)) {
                    LOG.info("Signature checked for {} failed", fileName);
                    throw new BuildException("Signature check failed");
                }
            }

        } catch (final ComponentInitializationException | IOException e) {
            LOG.error("Could not manage truststore for [{}, {}] ", idpHome, pluginId, e);
            throw new BuildException(e);
        }
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        Path myIdpHome = idpHome;
        if (myIdpHome == null) {
            throw new ComponentInitializationException("idp.home property must be set");
        }
        try {
            idpHome = myIdpHome = InstallerSupport.canonicalPath(myIdpHome);
        } catch (final IOException e) {
            LOG.error("Could not canonicalize idp home", e);
            throw new ComponentInitializationException(e);
        }
        final String idpHomeString = myIdpHome.toString();
        assert idpHomeString!= null;
        moduleContext = new ModuleContext(idpHomeString);
        moduleContext.setHttpClientSecurityParameters(securityParams);
        moduleContext.setHttpClient(httpClient);
        distPath = idpHome.resolve("dist");
        final Path wsp;
		try {
			wsp = workspacePath = Files.createTempDirectory("plugin-installer-workspace");
		} catch (final IOException e) {
			throw new ComponentInitializationException(e);
		}
        final Path pwp = pluginsWebapp = distPath.resolve("plugin-webapp");
        final Path pcp = pluginsContents = distPath.resolve("plugin-contents");
        assert wsp!=null && pwp!=null && pcp!=null && distPath!=null;
        InstallerSupport.setReadOnly(distPath, false);
        // Just in case they have been protected
        InstallerSupport.setMode(wsp, "640", "**/*");
        InstallerSupport.setMode(pwp, "640", "**/*");
        InstallerSupport.setMode(pcp, "640", "**/*");
    }

    /** Generate a {@link URLClassLoader} which looks at the
     * installed WEB-INF/lib in addition to the dist webapp and bin/lib directories.
     * As a side effect, it also copies the libs (since they may well be overwritten)
     * @return an appropriate loader
     * @throws BuildException if a directory traversal fails.
     */
    @Nonnull private synchronized URLClassLoader getInstalledPluginsLoader() throws BuildException {

        if (installedPluginsLoader != null) {
            return installedPluginsLoader;
        }
        final URL[] urls;
        final Path libs = getPluginsWebapp().resolve("WEB-INF").resolve("lib");
        if (Files.exists(libs)) {
            try {
                if (!Files.exists(workspacePath)) {
                    Files.createDirectories(workspacePath);
                }
                final Path pathToDir = Files.createTempDirectory(workspacePath, "classpath");
                assert libs!=null && pathToDir!=null;
                final LoggingVisitor visitor = new LoggingVisitor(libs, pathToDir);
                try (final DirectoryStream<Path> webInfLibs = Files.newDirectoryStream(libs)) {
                    for (final Path jar : webInfLibs) {
                        visitor.visitFile(jar, null);
                    }
                }
                urls = visitor.
                        getCopiedList().
                        stream().
                        map( path -> {
                            try {
                                return path.toUri().toURL();
                            } catch (final MalformedURLException e1) {
                                throw new BuildException(e1);
                            }
                        }).
                        toArray(URL[]::new);
            } catch (final IOException e) {
                LOG.error("Error finding Plugins' classpath");
                throw new BuildException(e);
            }
        } else {
            urls = new URL[0];
        }
        installedPluginsLoader = new URLClassLoader(urls);
        return installedPluginsLoader;        
    }

    /** Generate a {@link URLClassLoader} which looks at the
     * installing WEB-INF.
     * @return an appropriate loader
     * @throws BuildException if a directory traversal fails.
     */
    @Nonnull private synchronized URLClassLoader getDistributionLoader() throws BuildException {
        if (installingPluginLoader!= null) {
            return installingPluginLoader;
        }
        try {
            final List<URL> urls = new ArrayList<>();
            final Path libDir = distribution.resolve("webapp").resolve("WEB-INF").resolve("lib");
            try (final DirectoryStream<Path> libDirPaths = Files.newDirectoryStream(libDir)){
                for (final Path jar : libDirPaths) {
                    urls.add(jar.toUri().toURL());
                }
                installingPluginLoader  = new URLClassLoader(urls.toArray(URL[]::new));
                return installingPluginLoader;
            }
        } catch (final IOException e) {
            LOG.error("Error building Plugin Installation ClassPathLoader");
            throw new BuildException(e);
        }

    }

    /**
     * Return a list of the installed plugins.
     * @return All the plugins.
     * @throws BuildException if loafing the classpath fails.
     */
    @Nonnull public List<IdPPlugin> getInstalledPlugins() throws BuildException {
       final Stream<Provider<IdPPlugin>> loaderStream =
               ServiceLoader.load(IdPPlugin.class, getInstalledPluginsLoader()).stream();
       return loaderStream.map(ServiceLoader.Provider::get)
               .collect(CollectionSupport.nonnullCollector(Collectors.toList())).get();
    }

    /** Find the {@link IdPPlugin} with the provided Id.
     * @param name what to find
     * @return the {@link IdPPlugin} or null if not found.
     */
    @Nullable public IdPPlugin getInstalledPlugin(@Nonnull final String name) {
        final List<IdPPlugin> plugins = getInstalledPlugins();
        for (final IdPPlugin plugin: plugins) {
            if (plugin.getPluginId().equals(name)) {
                return plugin;
            }
        }
        return null;
    }

    /** close, ignoring errors.
     * @param what what to close
     */
    private void closeSilently(@Nullable final AutoCloseable what) {
        if (what == null) {
            return;
        }
        try {
            what.close();
        } catch (final Exception e) {
            LOG.error("Autoclose of {} failed", what, e);
        }
    }

    /** {@inheritDoc} */
    public void close() {
        closeSilently(installedPluginsLoader);
        closeSilently(installingPluginLoader);
        InstallerSupport.deleteTree(downloadDirectory);
        InstallerSupport.deleteTree(unpackDirectory);
        InstallerSupport.deleteTree(workspacePath);
        assert distPath!=null;
        InstallerSupport.setReadOnly(distPath, true);
    }
    
    /** Return a version we can use in a test proof manner.
     * @return the IdP version or a fixed value
     */
    @Nonnull protected static InstallableComponentVersion getIdPVersion() {
        final String version  = Version.getVersion();

        if (version == null) {
            LOG.error("Could not determine IdP Version. Assuming 5.0.0");
            LOG.error("You should never see this outside a test environment/");
            return new InstallableComponentVersion(5,0,0);
        } 
        return new InstallableComponentVersion(version);
    }

}