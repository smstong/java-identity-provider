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

package net.shibboleth.idp.authn.spnego.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.slf4j.Logger;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Kerberos login utility for the context acceptor, encapsulates a number of special options
 * used to create a security context for the GSS acceptor, usually based on a keytab file.
 */
public class GSSAcceptorLoginModule {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(GSSAcceptorLoginModule.class);

    /** The JAAS login module to use. */
    @Nullable private LoginModule krbModule;

    /** Hashtable to hold state of the JAAS login module. */
    @Nonnull private Map<String, String> state = new HashMap<>();

    /** Options for the JAAS login module. */
    @Nonnull private Map<String, String> options = new HashMap<>();

    /** The realm settings. */
    @Nonnull private KerberosRealmSettings realm;

    /**
     * Constructor.
     * 
     * @param realmSettings the settings of the realm
     * @param refreshKrb5Config whether to set the JAAS login module's option "refreshKrb5Config"
     * @param loginModuleClassName the JAAS login module to use
     */
    public GSSAcceptorLoginModule(@Nonnull final KerberosRealmSettings realmSettings, final boolean refreshKrb5Config,
            @Nonnull @NotEmpty final String loginModuleClassName) {
        realm = Constraint.isNotNull(realmSettings, "KerberosRealmSettings cannot be null");

        options.put("refreshKrb5Config", Boolean.valueOf(refreshKrb5Config).toString());
        options.put("useKeyTab", "true");
        options.put("keyTab", realmSettings.getKeytab());
        options.put("principal", realmSettings.getServicePrincipal());
        options.put("isInitiator", realmSettings.getPassword() != null ? "true" : "false");
        options.put("storeKey", "true");

        try {
            krbModule = (LoginModule) Class.forName(loginModuleClassName).getDeclaredConstructor().newInstance();
        } catch (final InstantiationException | IllegalAccessException | ClassNotFoundException |
                IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            log.error("Unable to instantiate JAAS module for Kerberos", e);
            // no module available; login() will throw an exception later
            krbModule = null;
        }
    }

    /**
     * Execute the login and return a Subject for the acceptor identity.
     * 
     * @return the GSS acceptor Subject
     * @throws LoginException if an error occurs
     */
    public Subject login() throws LoginException {
        final LoginModule module = krbModule;
        if (module == null) {
            throw new LoginException("No JAAS module for Kerberos available");
        }

        // Set a CallbackHandler to inform user/password if the keytab file fails/not used.
        final UsernamePasswordCallbackHandler callbackH =
                new UsernamePasswordCallbackHandler(realm.getServicePrincipal(), realm.getPassword());
        final Subject subject = new Subject();
        module.initialize(subject, callbackH, state, options);
        if (module.login()) {
            module.commit();
        }
        return subject;
    }

    /**
     * Perform a JAAS logout.
     * 
     * @throws LoginException if an error occurs
     */
    public void logout() throws LoginException {
        final LoginModule module = krbModule;
        if (module != null) {
            module .logout();
        }
    }

    /**
     * A JAAS username and password CallbackHandler.
     * 
     * <p>This is only used in the case that a keytab isn't.</p>
     */
    private class UsernamePasswordCallbackHandler implements CallbackHandler {
        /** The name to use. */
        @Nullable private String name;

        /** The password to use. */
        @Nullable private String password;

        /**
         * Constructor.
         * 
         * @param theName the name to use
         * @param thePassword the password to use
         */
        public UsernamePasswordCallbackHandler(@Nullable final String theName, @Nullable final String thePassword) {
            name = theName;
            password = thePassword;
        }

        /** {@inheritDoc} */
        @Override
        public void handle(@Nullable final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            if (callbacks != null && callbacks.length > 0) {
                final String nm = name;
                if (nm == null || nm.length() == 0) {
                    throw new IllegalArgumentException("No username provided");
                }

                final String pwd = password;
                if (pwd == null || pwd.length() == 0) {
                    throw new IllegalArgumentException("No password provided");
                }

                for (final Callback c : callbacks) {
                    if (c instanceof NameCallback) {
                        ((NameCallback) c).setName(nm);
                    } else if (c instanceof PasswordCallback) {
                        ((PasswordCallback) c).setPassword(pwd.toCharArray());
                    } else {
                        throw new UnsupportedCallbackException(c);
                    }
                }
            }
        }
    }

}
