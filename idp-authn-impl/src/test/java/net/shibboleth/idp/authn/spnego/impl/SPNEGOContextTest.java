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

import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link SPNEGOContextTest} unit test. */
@SuppressWarnings("javadoc")
public class SPNEGOContextTest {

    @Test
    public void testGettersAndSetters() throws Exception {
        SPNEGOContext context = new SPNEGOContext();
        KerberosSettings settings = new KerberosSettings();
        KerberosRealmSettings realm1 = new KerberosRealmSettings();
        KerberosRealmSettings realm2 = new KerberosRealmSettings();
        ArrayList<KerberosRealmSettings> realms = new ArrayList<>();
        realms.add(realm1);
        realms.add(realm2);
        settings.setRealms(realms);

        final GSSContextAcceptor acceptor = new GSSContextAcceptor(settings);
        
        context.setKerberosSettings(settings);
        context.setContextAcceptor(acceptor);

        Assert.assertSame(settings, context.getKerberosSettings());
        Assert.assertSame(acceptor, context.getContextAcceptor());
    }

}