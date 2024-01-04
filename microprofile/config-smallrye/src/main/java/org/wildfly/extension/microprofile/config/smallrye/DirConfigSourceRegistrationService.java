/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye;

import java.io.File;
import java.util.function.Supplier;

import io.smallrye.config.source.file.FileSystemConfigSource;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.services.path.AbsolutePathService;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.microprofile.config.smallrye._private.MicroProfileConfigLogger;

/**
 * Service to register a ConfigSource reading its configuration from a directory (where files are property keys and
 * their content is the property values).
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
class DirConfigSourceRegistrationService implements Service {

    private final String name;
    private final String path;
    /**
     * Can be null
     */
    private final String relativeTo;
    private final int ordinal;
    private final Supplier<PathManager> pathManager;
    private final Registry<ConfigSource> sources;

    DirConfigSourceRegistrationService(String name, String path, String relativeTo, int ordinal, Supplier<PathManager> pathManager, Registry<ConfigSource> sources) {
        this.name = name;
        this.path = path;
        this.relativeTo = relativeTo;
        this.ordinal = ordinal;
        this.pathManager = pathManager;
        this.sources = sources;
    }

    static void install(OperationContext context, String name, String path, String relativeTo, int ordinal, Registry registry) {
        ServiceBuilder<?> builder = context.getServiceTarget()
                .addService(ServiceNames.CONFIG_SOURCE.append(name));
        Supplier<PathManager> pathManager = builder.requires(context.getCapabilityServiceName("org.wildfly.management.path-manager", PathManager.class));

        builder.setInstance(new DirConfigSourceRegistrationService(name, path, relativeTo, ordinal, pathManager, registry))
                .install();
    }

    @Override
    public void start(StartContext startContext) {
        String relativeToPath = AbsolutePathService.isAbsoluteUnixOrWindowsPath(path) ? null  : relativeTo;
        String dirPath = pathManager.get().resolveRelativePathEntry(path, relativeToPath);
        File dir = new File(dirPath);
        MicroProfileConfigLogger.ROOT_LOGGER.loadConfigSourceFromDir(dir.getAbsolutePath());
        this.sources.register(this.name, new FileSystemConfigSource(dir, ordinal));
    }

    @Override
    public void stop(StopContext context) {
        this.sources.unregister(this.name);
    }
}
