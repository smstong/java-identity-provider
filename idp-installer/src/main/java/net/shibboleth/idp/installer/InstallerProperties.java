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

package net.shibboleth.idp.installer;

import javax.annotation.Nonnull;

import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * Class to contain the publicly visible (and api) properties used by the installer. 
 */
public final class InstallerProperties {
    /** The name of a property file to fill in some or all of the above. This file is deleted after processing. */
    @Nonnull @NotEmpty public static final String PROPERTY_SOURCE_FILE = "idp.property.file";

    /** The name of a property file to merge with idp.properties. */
    @Nonnull @NotEmpty public static final String IDP_PROPERTIES_MERGE = "idp.merge.properties";

    /** The name of a property file to merge with ldap.properties. */
    @Nonnull @NotEmpty public static final String LDAP_PROPERTIES_MERGE = "ldap.merge.properties";

    /** The LDAP Password (usually associated with a username in ldap.properties). */
    @Nonnull @NotEmpty public static final String LDAP_PASSWORD = "idp.LDAP.credential";

    /** Where to install to.  Default is basedir */
    @Nonnull @NotEmpty public static final String TARGET_DIR = "idp.target.dir";

    /** The entity ID. */
    @Nonnull @NotEmpty public static final String ENTITY_ID = "idp.entityID";

    /** Do we  cause a failure rather than a prompt. */
    @Nonnull @NotEmpty public static final String NO_PROMPT = "idp.noprompt";

    /** What is the installer host name?  */
    @Nonnull @NotEmpty public static final String HOST_NAME = "idp.host.name";

    /** The scope to assert.  */
    @Nonnull @NotEmpty public static final String SCOPE = "idp.scope";

    /** The keystore password to use.  */
    @Nonnull @NotEmpty public static final String KEY_STORE_PASSWORD = "idp.keystore.password";

    /** The sealer password to use.  */
    @Nonnull @NotEmpty public static final String SEALER_PASSWORD = "idp.sealer.password";

    /** The sealer alias to use.  */
    @Nonnull @NotEmpty public static final String SEALER_ALIAS = "idp.sealer.alias";

    /** The keysize for the sealer.  */
    @Nonnull @NotEmpty public static final String SEALER_KEYSIZE = "idp.sealer.keysize";

    /** The the key size to generate.  */
    @Nonnull @NotEmpty public static final String KEY_SIZE = "idp.keysize";

    /** Mode to set on credential *key files. */
    @Nonnull @NotEmpty public static final String MODE_CREDENTIAL_KEYS = "idp.conf.credentials.filemode";

    /** Group to set on files in the credential and conf directories. */
    @Nonnull @NotEmpty public static final String GROUP_CONF_CREDENTIALS = "idp.conf.credentials.group";

    /** Do we do any chgrp/chmod work? */
    @Nonnull @NotEmpty public static final String PERFORM_SET_MODE = "idp.conf.setmode";

    /** Whether to tidy up after ourselves. */
    @Nonnull @NotEmpty public static final String NO_TIDY = "idp.no.tidy";

    /** Key size for all installer-generated keys. */
    public static final int DEFAULT_KEY_SIZE = 3072;


}
