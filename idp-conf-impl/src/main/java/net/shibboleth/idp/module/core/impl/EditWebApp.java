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

package net.shibboleth.idp.module.core.impl;

import java.io.IOException;

import javax.annotation.Nonnull;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.module.ModuleContext;
import net.shibboleth.idp.module.ModuleException;
import net.shibboleth.idp.module.impl.CoreIdPModule;

/**
 * {@link IdPModule} implementation.
 */
public final class EditWebApp extends CoreIdPModule {

    /**
     * Constructor.
     *  
     * @throws ModuleException on error
     * @throws IOException on error
     */
    public EditWebApp() throws IOException, ModuleException {
        super(EditWebApp.class);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEnabled(@Nonnull final ModuleContext moduleContext) {
        return true;
    }

}