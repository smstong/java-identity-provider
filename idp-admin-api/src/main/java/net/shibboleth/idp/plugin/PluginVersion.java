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

package net.shibboleth.idp.plugin;

import javax.annotation.Nonnull;

/**
 * @deprecated class.
 */
@Deprecated(forRemoval = true, since = "5.0.0") public class PluginVersion extends InstallableComponentVersion {

    /**
     * Constructor.
     *
     * @param plugin what to get the version of.
     * @throws NumberFormatException if the values are out of range
     */
    public PluginVersion(@Nonnull IdPPlugin plugin) throws NumberFormatException {
        super(plugin);
    }

    /**
     * Constructor.
     *
     * @param version what to build from 
     * @throws NumberFormatException if it doesn't fit a 1.2.3 format or if the values are
     * out of range
     */
    public PluginVersion(final String version) throws NumberFormatException {
        super(version);
    }

    /**
     * Constructor.
     *
     * @param maj Major Version
     * @param min Minor Version
     * @param pat Patch Version
     * @throws NumberFormatException if the values are out of range
     */
    public PluginVersion(final int maj, final int min, final int pat) {
        super(maj, min, pat);
    }
}
