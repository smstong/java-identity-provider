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

import javax.annotation.Nonnull;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.shared.component.ComponentInitializationException;

@SuppressWarnings("javadoc")
public class KerberosRealmSettingsTest {

    @Nonnull protected static final String SERVICE_PRINCIPAL = "HTTP/aai-logon.domain_a.com@DOMAIN_A.COM";

    @Nonnull protected static final String KEYTAB = "/opt/kerberos/http_domainA.keytab";

    @Nonnull protected static final String PASSWORD = "secret";
    
    private Object nullObj;

    @BeforeMethod
    public void setUp() throws Exception {
    }

    @Test
    public void testRequiredValues() throws Exception {
        KerberosRealmSettings realm;

        realm = new KerberosRealmSettings();
        realm.setServicePrincipal(SERVICE_PRINCIPAL);
        realm.setKeytab(KEYTAB);

        realm.initialize();

        Assert.assertEquals(realm.getServicePrincipal(), SERVICE_PRINCIPAL, "Value for service principal is invalid.");
        Assert.assertEquals(realm.getKeytab(), KEYTAB, "Value for keytab is invalid.");
        Assert.assertNull(realm.getPassword(), "Value for password is not null.");

        realm = new KerberosRealmSettings();
        realm.setServicePrincipal(SERVICE_PRINCIPAL);
        realm.setPassword(PASSWORD);

        realm.initialize();

        Assert.assertEquals(realm.getServicePrincipal(), SERVICE_PRINCIPAL, "Value for principal is invalid.");
        Assert.assertNull(realm.getKeytab(), "Value for keytab is not null.");
        Assert.assertEquals(realm.getPassword(), PASSWORD, "Value for password is invalid.");
    }

    @Test
    public void testMutuallyExclusiveValues() throws Exception {
        KerberosRealmSettings realm;

        try {
            realm = new KerberosRealmSettings();
            realm.setServicePrincipal(SERVICE_PRINCIPAL);
            realm.setKeytab(KEYTAB);
            realm.setPassword(PASSWORD);

            realm.initialize();

            Assert.fail("Both keytab and password are set, but they are mutually exclusive.");
        } catch (ComponentInitializationException ex) {
            // OK
        }
    }

    @Test
    public void testNotSettingKeytabOrPassword() throws Exception {
        KerberosRealmSettings realm;

        // unset values
        try {
            realm = new KerberosRealmSettings();
            realm.setServicePrincipal(SERVICE_PRINCIPAL);

            realm.initialize();

            Assert.fail("Neither keytab nor password is set.");
        } catch (Exception ex) {
            // OK
        }

        // set to null values
        try {
            realm = new KerberosRealmSettings();
            realm.setServicePrincipal(SERVICE_PRINCIPAL);
            realm.setKeytab(null);
            realm.setPassword(null);

            realm.initialize();

            Assert.fail("keytab and password must not both be set to null.");
        } catch (Exception ex) {
            // OK
        }

        // unset values
        try {
            realm = new KerberosRealmSettings();
            realm.setServicePrincipal(SERVICE_PRINCIPAL);
            realm.setKeytab("   ");
            realm.setPassword("   ");

            realm.initialize();

            Assert.fail("keytab and password must not both be set to empty string.");
        } catch (Exception ex) {
            // OK
        }

    }

    @Test
    public void testSettingBothKeytabAndPassword() throws Exception {
        KerberosRealmSettings realm;

        // unset values
        try {
            realm = new KerberosRealmSettings();
            realm.setServicePrincipal(SERVICE_PRINCIPAL);
            realm.setKeytab(KEYTAB);
            realm.setPassword(PASSWORD);

            realm.initialize();

            Assert.fail("Both keytab and password are set.");
        } catch (Exception ex) {
            // OK
        }
    }

    @Test
    public void testNullOrEmptyValues() throws Exception {
        KerberosRealmSettings realm;

        // unset values
        try {
            realm = new KerberosRealmSettings();

            realm.initialize();

            Assert.fail("Required values are not set.");
        } catch (Exception ex) {
            // OK
        }
        
        // domain is empty string
        try {
            realm = new KerberosRealmSettings();
            realm.setKeytab(KEYTAB);

            realm.initialize();

            Assert.fail("domain must not be set to empty string.");
        } catch (Exception ex) {
            // OK
        }

        // principal == null
        try {
            realm = new KerberosRealmSettings();
            realm.setServicePrincipal((String) nullObj);
            realm.setKeytab(KEYTAB);

            realm.initialize();

            Assert.fail("principal must not be set to null.");
        } catch (Exception ex) {
            // OK
        }

        // principal is empty string
        try {
            realm = new KerberosRealmSettings();
            realm.setServicePrincipal("   ");
            realm.setKeytab(KEYTAB);

            realm.initialize();

            Assert.fail("principal must not be set to empty string.");
        } catch (Exception ex) {
            // OK
        }
    }
}
