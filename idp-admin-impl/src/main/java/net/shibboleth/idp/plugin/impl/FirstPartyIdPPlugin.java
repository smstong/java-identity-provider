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

package net.shibboleth.idp.plugin.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.annotation.Nonnull;

import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.idp.plugin.PropertyDrivenIdPPlugin;
import net.shibboleth.profile.plugin.PluginException;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;

/**
 * Implementation class for plugins from the project itself to centralize
 * update handling.
 */
public class FirstPartyIdPPlugin extends PropertyDrivenIdPPlugin {

    /**
     * Constructor.
     *
     * @param claz type of plugin
     * 
     * @throws IOException if properties can't be loaded
     * @throws PluginException if another error occurs
     */
    public FirstPartyIdPPlugin(@Nonnull final Class<? extends IdPPlugin> claz) throws IOException, PluginException {
        super(claz);
    }

    /** {@inheritDoc} 
     * @throws PluginException if the URLs cannot be resolved */
    @Override
    @Nonnull @Unmodifiable @NotLive public List<URL> getDefaultUpdateURLs() throws PluginException {
        try {
            // The second location is a backup CNAME pointing into AWS S3 at present.
            return CollectionSupport.listOf(
                    new URL("https://shibboleth.net/downloads/identity-provider/plugins/plugins.properties"),
                    new URL("http://plugins.shibboleth.net/plugins.properties"));
        } catch (final MalformedURLException e) {
            throw new PluginException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @Unmodifiable @NotLive public List<URL> getDefaultModuleInfoSources() throws PluginException {
        try {
            // The second location is a backup CNAME pointing into AWS S3 at present.
            return CollectionSupport.listOf(
                    new URL("https://shibboleth.net/downloads/identity-provider/plugins/modules.properties"),
                    new URL("http://plugins.shibboleth.net/modules.properties"));
        } catch (final MalformedURLException e) {
            throw new PluginException(e);
        }
    }
}