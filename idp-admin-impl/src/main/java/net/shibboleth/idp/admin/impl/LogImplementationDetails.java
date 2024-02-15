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
import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.common.base.StandardSystemProperty;

import net.shibboleth.idp.Version;
import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.profile.module.ModuleContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * A bean that logs IdP internals when instantiated.
 * 
 * @since 4.3.0
 */
public final class LogImplementationDetails {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LogImplementationDetails.class);

    /**
     * <p>Log the IdP version and Java version and vendor at INFO level.</p>
     * 
     * <p>Log system properties defined by {@link StandardSystemProperty} at DEBUG level.</p>
     * 
     * <p>Log duplicate properties if found at WARN level.</p>
     * 
     * @param idpHomeLocation idp.home property
     * @param duplicateProperties tracking of duplicated properties
     */
    public LogImplementationDetails(@Nullable @NotEmpty final String idpHomeLocation,
            @Nullable @NotEmpty final String duplicateProperties) {
        
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
        
        if (idpHomeLocation != null) {
            final ModuleContext context = new ModuleContext(idpHomeLocation);
            final List<IdPModule> modules = ServiceLoader.
                    load(IdPModule.class).
                    stream().
                    map(e->e.get()).
                    filter(f->f.isEnabled(context)).
                    collect(Collectors.toList());
            if (modules.isEmpty()) {
                log.info("No Modules Enabled");
            } else {
                log.info("Enabled Modules:");
                for (final IdPModule module : modules) {
                    log.info("\t\t{}",  module.getName(context));
                }
            }
        } else {
            log.warn("Could not enumerate Modules");
        }
        
        if (duplicateProperties != null && !duplicateProperties.isBlank()) {
            log.warn("Duplicate properties were detected: {}", duplicateProperties);
        }
        
    }
    
}