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

package net.shibboleth.idp.admin.impl;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;

import com.google.common.base.StandardSystemProperty;

import net.shibboleth.idp.Version;
import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.idp.spring.IdPPropertiesApplicationContextInitializer;
import net.shibboleth.profile.module.ModuleContext;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * A bean that logs IdP internals when instantiated, and outputs a number of warning conditions.
 * 
 * @since 4.3.0
 */
public final class LogImplementationDetails extends ApplicationObjectSupport {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LogImplementationDetails.class);

    /** {@inheritDoc} */
    @Override
    protected boolean isContextRequired() {
        return true;
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected void initApplicationContext(@Nonnull final ApplicationContext context) throws BeansException {
        log.info("Shibboleth IdP Version {}", Version.getVersion());
        log.info("Java version='{}' vendor='{}'", StandardSystemProperty.JAVA_VERSION.value(),
                StandardSystemProperty.JAVA_VENDOR.value());
        if (log.isDebugEnabled()) {
            for (final StandardSystemProperty standardSystemProperty : StandardSystemProperty.values()) {
                log.debug("{}", standardSystemProperty);
            }
        }
        final List<IdPPlugin> plugins = ServiceLoader.
                load(IdPPlugin.class).
                stream().
                map(e->e.get()).
                collect(Collectors.toList());
        if (plugins.isEmpty()) {
            log.info("No Plugins Loaded");
        } else {
            log.info("Plugins:");
            for (final IdPPlugin idpPlugin : plugins) {
                log.info("\t\t{} : v{}.{}.{}",  idpPlugin.getPluginId(), idpPlugin.getMajorVersion(),
                        idpPlugin.getMinorVersion(), idpPlugin.getPatchVersion());
            }
        }
        
        final String idpHomeLocation =
                context.getEnvironment().getProperty(IdPPropertiesApplicationContextInitializer.IDP_HOME_PROPERTY);
        if (idpHomeLocation != null) {
            final ModuleContext moduleContext = new ModuleContext(idpHomeLocation);
            final List<IdPModule> modules = ServiceLoader.
                    load(IdPModule.class).
                    stream().
                    map(e->e.get()).
                    filter(f->f.isEnabled(moduleContext)).
                    collect(Collectors.toList());
            if (modules.isEmpty()) {
                log.info("No Modules Enabled");
            } else {
                log.info("Enabled Modules:");
                for (final IdPModule module : modules) {
                    log.info("\t\t{}",  module.getName(moduleContext));
                }
            }
        } else {
            log.warn("Could not enumerate Modules");
        }
        
        final String duplicateProperties =
                context.getEnvironment().getProperty(IdPPropertiesApplicationContextInitializer.IDP_DUPLICATE_PROPERTY);
        if (duplicateProperties != null && !duplicateProperties.isBlank()) {
            log.warn("Duplicate properties were detected: {}", duplicateProperties);
        }
        
        final String autoSearch =
                context.getEnvironment().getProperty(
                        IdPPropertiesApplicationContextInitializer.IDP_AUTOSEARCH_PROPERTY);
        if (!Boolean.valueOf(autoSearch)) {
            log.warn("{} is false or unset, plugin use may require additional changes to add new property sources",
                    IdPPropertiesApplicationContextInitializer.IDP_AUTOSEARCH_PROPERTY);
        }
    }
// Checkstyle: CyclomaticComplexity ON
    
}