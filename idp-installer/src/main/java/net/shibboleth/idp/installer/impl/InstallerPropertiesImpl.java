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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.input.InputRequest;
import org.slf4j.Logger;

import net.shibboleth.idp.installer.InstallerProperties;
import net.shibboleth.idp.installer.InstallerSupport;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.codec.Base64Support;
import net.shibboleth.shared.codec.EncodingException;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.NonnullSupplier;
import net.shibboleth.shared.primitive.StringSupport;

/** Class which encapsulated all the properties/UI driven configuration of an install.

 NOTE Updated to this properties should be reflected in the "PropertyDriverInstallation" wiki page."/

*/
public class InstallerPropertiesImpl  {

    /** Which modules to enable on initial install.
     * @since 4.1.0 */
    @Nonnull @NotEmpty public static final String INITIAL_INSTALL_MODULES = "idp.initial.modules";

    /** Those modules which are "core". */
    @Nonnull public static final Set<String> CORE_MODULES =
            CollectionSupport.setOf("idp.Core", "idp.EditWebApp", "idp.CommandLine");

    /** Those modules enabled by default. */
    @Nonnull public static final Set<String> DEFAULT_MODULES =
            CollectionSupport.setOf("idp.authn.Password", "idp.admin.Hello");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InstallerPropertiesImpl.class);

    /** The properties driving the install. */
    @NonnullAfterInit private Properties installerProperties;

    /** The target Directory. */
    @Nullable private Path targetDir;

    /** The sourceDirectory. */
    @Nonnull private final Path srcDir;

    /** Do we allow prompting?*/
    private boolean noPrompt;

    /** The entity ID. */
    @Nullable private String entityID;

    /** Hostname. */
    @Nullable private String hostname;

    /** scope. */
    @Nullable private String scope;

    /** Keystore Password. */
    @Nullable private String keyStorePassword;

    /** Sealer Password. */
    @Nullable private String sealerPassword;

    /** Sealer Alias. */
    @Nullable private String sealerAlias;

    /** Key Size. (for signing, encryption and backchannel). */
    private int keySize;

    /** whether to tidy up. */
    private boolean tidy = true;

    /** whether to tidy up. */
    private boolean setGroupAndMode = true;

    /** credentials key file mode. */
    @Nullable private String credentialsKeyFileMode;

    /** Input handler from the prompting. */
    @Nonnull private final InputHandler inputHandler;

    /**
     * Constructor.
     * 
     * @param sourceDir Where the *source* installation is
     */
    public InstallerPropertiesImpl(@Nonnull final Path sourceDir) {
        srcDir = sourceDir;
        inputHandler = getInputHandler();
    }

    /**
     * Get an {@link InputHandler} for the prompting.
     * 
     * @return an input handler
     */
    @Nonnull protected InputHandler getInputHandler() {
        return new DefaultInputHandler() {
            // we want the prompts to be more obviously prompts
            protected String getPrompt(final InputRequest request) {
                return super.getPrompt(request) + " ? ";
            }
        };
    }

    /**
     * Initialization routine.
     * 
     * @throws ComponentInitializationException if initialization fails
     */
// CheckStyle: CyclomaticComplexity OFF
    protected void doInitialize() throws ComponentInitializationException {
        installerProperties = new Properties(System.getProperties());

        if (!Files.exists(srcDir)) {
            log.error("Source dir {} did not exist", srcDir.toAbsolutePath());
            throw new ComponentInitializationException(srcDir.toString() + " must exist");
        }
        log.debug("Source dir {}", srcDir);

        final Path propertyFile = getMergeFile(InstallerProperties.PROPERTY_SOURCE_FILE);
        if (propertyFile != null) {
            /* The file specified in the system file idp.property.file (if present). */
            final File idpPropertyFile = propertyFile.toFile();
            try(final FileInputStream stream = new FileInputStream(idpPropertyFile)) {
                installerProperties.load(stream);
            } catch (final IOException e) {
                log.error("Could not load {}: {}", propertyFile.toAbsolutePath(), e.getMessage());
                throw new ComponentInitializationException(e);
            }
            if (!isNoTidy()) {
                idpPropertyFile.deleteOnExit();
            }
        }

        final String noTidy = installerProperties.getProperty(InstallerProperties.NO_TIDY);
        tidy = noTidy == null;
        final String setModeString = installerProperties.getProperty(InstallerProperties.PERFORM_SET_MODE);
        if (setModeString != null) {
            setGroupAndMode = Boolean.valueOf(setModeString);
        }

        String value = installerProperties.getProperty(InstallerProperties.NO_PROMPT);
        noPrompt = value != null;

        value = installerProperties.getProperty(InstallerProperties.KEY_SIZE);
        if (value == null) {
            keySize = InstallerProperties.DEFAULT_KEY_SIZE;
        } else {
            keySize = Integer.parseInt(value);
        }
    }
// CheckStyle: CyclomaticComplexity ON

    /**
     * Lookup a property; if it isn't defined then ask the user (if we are allowed).
     * 
     * <p>This is used by most (but all) getters that redirect through a property.</p>
     * 
     * @param propertyName the property to lookup
     * @param prompt what to say to the user
     * @param defaultSupplier how to get the default value.  Using a Supplier allows this
     *      to be a reasonably heavyweight operation
     *      
     * @return the value
     * 
     * @throws BuildException of anything goes wrong
     */
    @Nonnull protected String getValue(final String propertyName,
            final String prompt, final NonnullSupplier<String> defaultSupplier) throws BuildException {
        String value = installerProperties.getProperty(propertyName);
        if (value != null) {
            return value;
        }
        if (noPrompt) {
            throw new BuildException("No value for " + propertyName + " specified");
        }

        final InputRequest request = new InputRequest(prompt);
        final String defaultValue = defaultSupplier.get();
        request.setDefaultValue(defaultValue);

        inputHandler.handleInput(request);
        value = request.getInput();
        if (value == null || "".contentEquals(value)) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Lookup a property; if it isn't defined then ask the user (if we are allowed) via
     * a no-echo interface.
     * 
     * <p>Note that this does not work within a debugger.</p>
     * 
     * @param propertyName the property to lookup
     * @param prompt what to say to the user
     * 
     * @return the value (this is not echoed to the terminal)
     * 
     * @throws BuildException of anything goes wrong
     */
    @Nonnull protected String getPassword(final String propertyName, final String prompt) throws BuildException {
        final String value = installerProperties.getProperty(propertyName);
        if (value != null) {
            return value;
        }
        try {
            final byte key[] = new byte[32];
            SecureRandom.getInstance("SHA1PRNG").nextBytes(key);
            final String s = Base64Support.encode(key, false).substring(0, 32);
            assert s != null;
            return s;
        } catch (final NoSuchAlgorithmException|EncodingException e) {
            log.error("Password Generation failed", e);
            throw new BuildException("Password Generation failed", e);
        }
    }

    /**
     * Get where we are installing/updating/building the war.
     * 
     * <p>This is slightly complicated because the default depends on what we are doing.</p>
     * 
     * @return the target directory
     * 
     * @throws BuildException if something goes awry
     */
    @Nonnull public Path getTargetDir() throws BuildException {
        if (targetDir != null) {
            return targetDir;
        }
        final Path td = targetDir =
                Path.of(getValue(InstallerProperties.TARGET_DIR, "Installation Directory:", () -> "/opt/shibboleth-idp"));
        assert td != null;
        return td;
    }

    /**
     * Where is the install coming from?
     * 
     * @return the source directory
     */
    @Nonnull public Path getSourceDir() {
        return srcDir;
    }

    /**
     * Get the EntityId for this install.
     * 
     * @return the name
     */
    @Nonnull public String getEntityID() {
        String result = entityID;
        if (result == null) {
            entityID = result =
                    getValue(InstallerProperties.ENTITY_ID, "SAML EntityID:", () -> "https://" + getHostName() + "/idp/shibboleth");
        }
        return result;
    }

    /**
     * Does the user want us to *not* tidy up?
     * 
     * @return do we not tidy up?
     */
    public boolean isNoTidy() {
        return !tidy;
    }

    /**
     * Get the host name for this install.
     *
     * <p>Defaults to information pulled from the network.</p>
     *
     * @return the host name.
     */
    @Nonnull public String getHostName() {
        String result = hostname;
        if (result == null) {
            result = hostname = getValue(InstallerProperties.HOST_NAME, "Host Name:", () -> InstallerSupport.getBestHostName());
        }
        return result;
    }

    /**
    * Mode to set on all files in credentials.
    * 
    * @return the mode
    */
    @Nonnull public String getCredentialsKeyFileMode() {
        String result = credentialsKeyFileMode;
        if (result != null) {
            return result;
        }
        result = credentialsKeyFileMode = installerProperties.getProperty(InstallerProperties.MODE_CREDENTIAL_KEYS, "600");
        assert result != null;
        return result;
    }

    /**
    * Group to set on all files in credentials and conf.
    * 
    * @return the mode or null if none to be set
    */
    @Nullable public String getCredentialsGroup() {
        return installerProperties.getProperty(InstallerProperties.GROUP_CONF_CREDENTIALS);
    }

    /**
    * Do we set the mode?
    * 
    * @return do we the mode
    */
    public boolean isSetGroupAndMode() {
        return setGroupAndMode;
    }

    /**
     * Evaluate the default scope value.
     * 
     * @return everything after the first '.' in {@link #getHostName()}
     */
    @Nonnull protected String defaultScope() {
        final String host = getHostName();
        final int index = host.indexOf('.');
        if (index > 1) {
            final String result =host.substring(index+1);
            assert result != null;
            return result;
        }
        return "localdomain";
    }

    /**
     * Get the scope for this installation.
     * 
     * @return the scope
     */
    @Nonnull public String getScope() {
        String result = scope;
        if (result  == null) {
            result = scope = getValue(InstallerProperties.SCOPE, "Attribute Scope:", () -> defaultScope());
        }
        return result;
    }

    /**
    * Get the LDAP password iff one was provided. DO NOT PROMPT
    *
    * @return the password if provided by a properties
    * @throws BuildException  if badness happens
    */
    @Nullable public String getLDAPPassword() throws BuildException {
        return installerProperties.getProperty(InstallerProperties.LDAP_PASSWORD);
    }

    /**
     * Get the SubjectAltName for the certificates.
     * 
     * @return the SubjectAltName
     */
    @Nonnull public String getSubjectAltName() {
        return "https://" + getHostName() + "/idp/shibboleth";
    }

    /**
     * Get the password for the keystore for this installation.
     * 
     * @return the password.
     */
    @Nonnull public String getKeyStorePassword() {
        String result = keyStorePassword;
        if (keyStorePassword == null) {
            result = keyStorePassword = getPassword(InstallerProperties.KEY_STORE_PASSWORD, "Backchannel PKCS12 Password:");
        }
        assert result != null;
        return result;
    }

    /**
     * Get the password for the sealer for this installation.
     * 
     * @return the password.
     */
    @Nonnull public String getSealerPassword() {
        String result = sealerPassword;
        if (result == null) {
            result = sealerPassword = getPassword(InstallerProperties.SEALER_PASSWORD, "Cookie Encryption Key Password:");
        }
        return result;
    }

    /**
     * Get the modules to enable after first install.
     * 
     * @return the modules
     */
    @Nonnull @NotLive @Unmodifiable public Set<String> getModulesToEnable() {
        String prop = StringSupport.trimOrNull(installerProperties.getProperty(INITIAL_INSTALL_MODULES));
        if (prop == null) {
            return InstallerPropertiesImpl.DEFAULT_MODULES;
        }
        final boolean additive = prop.startsWith("+");
        if (additive) {
            prop = prop.substring(1);
        }
        final String[] modules = prop.split(",");
        assert modules != null;
        if (!additive) {
            final Set<String> result = CollectionSupport.copyToSet(CollectionSupport.arrayAsList(modules));
            return result;
        }
        final Set<String> result = new HashSet<>(modules.length + InstallerPropertiesImpl.DEFAULT_MODULES.size());
        result.addAll(InstallerPropertiesImpl.DEFAULT_MODULES);
        result.addAll(Arrays.asList(modules));
        return CollectionSupport.copyToSet(result);
    }
    
    /** Get the modules to enable before ant install.
     * @return the modules
     */
    @Nonnull @NotLive @Unmodifiable public Set<String> getCoreModules() {
        return InstallerPropertiesImpl.CORE_MODULES;
    }

    /** 
     * Return the sealer key size, if this has been specified.
     * 
     * @return the key size or null if non specified
     * 
     * @throws BuildException if the size was not an integer 
     */
    @Nullable Integer getSealerKeySize() throws BuildException {
        final String val = installerProperties.getProperty(InstallerProperties.SEALER_KEYSIZE);
        if (val == null) {
            return null;
        }
        final Integer result;
        try {
            result = Integer.valueOf(val);
        }
        catch (final NumberFormatException e) {
            log.error("Provided value for property {} ({}') was not an integer", InstallerProperties.SEALER_ALIAS, val);
            throw new BuildException(e);
        }
        return result;
    }

    /** 
     * Get the alias for the sealer key.
     * 
     * @return the alias
     */
    @Nonnull public String getSealerAlias() {
        String result = sealerAlias;
        if (result == null) {
            result = sealerAlias = installerProperties.getProperty(InstallerProperties.SEALER_ALIAS);
        }
        if (result == null) {
            result = sealerAlias = "secret";
        }
        return result;
    }

    /** 
     * Get the key size for signing, encryption and backchannel
     * 
     * @return the keysize, default is {@value #DEFAULT_KEY_SIZE}.
     */
    public int getKeySize() {
        return keySize;
    }

    /**
     * Get the file specified as the property as a File, or null if it doesn't exist.
     * 
     * @param propName the name to lookup
     * 
     * @return null if the property is not provided a {@link Path} otherwise
     * 
     * @throws BuildException if the property is supplied but the file doesn't exist.
     */
    @Nullable protected Path getMergeFile(final String propName) throws BuildException {
        final String propValue = installerProperties.getProperty(propName);
        if (propValue == null) {
            return null;
        }
        Path path = Path.of(propValue);
        if (Files.exists(path)) {
            log.debug("Property '{}' had value '{}' Path exists ", propName, propValue);
        } else {
            path = srcDir.resolve(path);
            if (!Files.exists(path)) {
                log.debug("Property '{}' had value '{}' neither '{}' nor '{}' exist", propName, propValue, path);
                log.error("Path '{}' supplied for '{}' does not exist", propValue, propName);
                throw new BuildException("Property file not found");
            }
            log.debug("Property '{}' had value '{}' Path {} exists ", propName, propValue, path);
        }
        if (Files.isDirectory(path)) {
            log.error("Path '{}' supplied by property '{}' was not a file", path, propName);
            throw new BuildException("No a file");
        }                
        return path;
    }

    /**
    * Get the a file to merge with idp.properties or null.
    *
    * @return the file or null if it none required
    * 
    * @throws BuildException if badness happens
    */
    @Nullable public Path getIdPMergeProperties() throws BuildException {
        return getMergeFile(InstallerProperties.IDP_PROPERTIES_MERGE);
    }

    /**
    * Get the a file to merge with ldap.properties or null.
    *
    * @return the path or null if it none required
    * 
    * @throws BuildException  if badness happens
    */
    @Nullable public Path getLDAPMergeProperties() throws BuildException {
        return getMergeFile(InstallerProperties.LDAP_PROPERTIES_MERGE);
    }
}
