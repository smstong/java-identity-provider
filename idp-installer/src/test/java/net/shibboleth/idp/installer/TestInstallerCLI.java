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

import org.testng.annotations.Test;

import net.shibboleth.idp.installer.impl.IdPInstallerCLI;
import net.shibboleth.idp.installer.impl.UpdateIdPCLI;
//import net.shibboleth.idp.installer.impl.UpdateIdPCLI;
/**
 *
 */
public class TestInstallerCLI {

    @Test(enabled = false)
    public void install() {
        System.setProperty(InstallerProperties.HOST_NAME, "machine.org.uk");
        IdPInstallerCLI.runMain(new String[] {
                "-t", "h:\\downloads\\idp",
                "-s",
                "h:\\Perforce\\Juno\\V5\\java-identity-provider\\idp-distribution\\target\\shibboleth-identity-provider-5.0.0-SNAPSHOT",
               });
    }

    @Test(enabled = false)
    public void silentInstall() {

        IdPInstallerCLI.runMain(new String[] {
                "-e", "https://test.example.org/id",
                "--hostName", "machine.org",
                "--scope", "machine.org",
                "--noPrompt",
                "-t", "h:\\downloads\\idp2",
                "-s", "h:\\Perforce\\Juno\\V5\\java-identity-provider\\idp-distribution\\target\\shibboleth-identity-provider-5.0.0-SNAPSHOT",
                "-hc", "shibboleth.InternalHttpClient"
               });
    }

    @Test(enabled = false)
    public void forceDownload() {

        UpdateIdPCLI.runMain(new String[] {
                "--pretendVersion","5.0.0",
                "-d", "h:\\downloads",
                "--home", "H:\\Downloads\\idp3",
                "-fd", "4.3.0"});

    }

    @Test(enabled = false)
    public void updateList430() {
        UpdateIdPCLI.runMain(new String[] {
                "-l",
                "--pretendVersion","4.3.0",
                "--updateURL", "file:H:\\Perforce\\juno\\Plugins\\java-idp-plugin-mgmt\\idp-versions.properties",
                "--home", "H:\\Downloads\\idp"});
    }

    @Test(enabled = false)
    public void updateList431() {
        UpdateIdPCLI.runMain(new String[] {
                "-l",
                "--pretendVersion","4.3.1",
                "--updateURL", "file:H:\\Perforce\\juno\\Plugins\\java-idp-plugin-mgmt\\idp-versions.properties",
                "--home", "H:\\Downloads\\idp"});
    }

    @Test(enabled = false)
    public void check430() {
        UpdateIdPCLI.runMain(new String[] {
                "--pretendVersion","4.3.0",
                "--updateURL", "file:H:\\Perforce\\juno\\Plugins\\java-idp-plugin-mgmt\\idp-versions.properties",
                "--home", "H:\\Downloads\\idp"});
    }

    @Test(enabled = false)
    public void check431() {
        UpdateIdPCLI.runMain(new String[] {
                "--pretendVersion","4.3.1",
                "--updateURL", "file:H:\\Perforce\\juno\\Plugins\\java-idp-plugin-mgmt\\idp-versions.properties",
                "--home", "H:\\Downloads\\idp"});
    }

    @Test(enabled = false)
    public void download430() {
        UpdateIdPCLI.runMain(new String[] {
                "-d", "H:\\downloads\\idp", 
                "--pretendVersion","4.3.0",
                "--updateURL", "file:H:\\Perforce\\juno\\Plugins\\java-idp-plugin-mgmt\\idp-versions.properties",
                "--home", "H:\\Downloads\\idp"});
    }
}
