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

package net.shibboleth.idp.module;

import javax.annotation.Nonnull;

import net.shibboleth.profile.module.Module;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * This interface is exported (via the service API) by every IdP module.
 * 
 * @since 4.1.0
 */
public interface IdPModule extends Module {

    /** Extension for preserving user files. */
    @Nonnull @NotEmpty static final String IDPSAVE_EXT = ".idpsave";

    /** Base extension for adding new default files. */
    @Nonnull @NotEmpty static final String IDPNEW_EXT_BASE = ".idpnew";

    /** {@inheritDoc} */
    @Override
    @Nonnull default String getSaveExtension() {
        return IDPSAVE_EXT;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull default String getNewExtension() {
        return IDPNEW_EXT_BASE;
    }

}