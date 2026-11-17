/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoader;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;

/**
 * @author <a href="jasondlee@redhat.com">Jason Lee</a>
 */
public class OpenTelemetrySubsystemExtension extends SubsystemExtension<OpenTelemetrySubsystemSchema> {
    public OpenTelemetrySubsystemExtension() {
        super(SubsystemConfiguration.of(OpenTelemetryConfigurationConstants.SUBSYSTEM_NAME,
                OpenTelemetrySubsystemModel.CURRENT,
                OpenTelemetrySubsystemRegistrar::new),
                SubsystemPersistence.of(OpenTelemetrySubsystemSchema.CURRENT));
        // Initialize the Vert.x logger factory
        initializeVertxLogger();
    }

    private static void initializeVertxLogger() {
        final ClassLoader classLoader = WildFlySecurityManager.getClassLoaderPrivileged(OpenTelemetrySubsystemExtension.class);
        if (classLoader instanceof ModuleClassLoader cl) {
            final ModuleLoader loader = cl.getModule().getModuleLoader();
            try {
                // Attempt to load the LoggerFactory for Vert.x 4
                final Module module = loader.loadModule("io.vertx.core");
                Class.forName("io.vertx.core.logging.LoggerFactory", true, module.getClassLoader());
            } catch (Throwable ignore) {
                try {
                    // Attempt to load the LoggerFactory for Vert.x 5
                    final Module module = loader.loadModule("io.vertx.core.logging");
                    Class.forName("io.vertx.core.internal.logging.LoggerFactory", true, module.getClassLoader());
                } catch (Throwable ignore2) {
                }
            }
        }
    }
}
