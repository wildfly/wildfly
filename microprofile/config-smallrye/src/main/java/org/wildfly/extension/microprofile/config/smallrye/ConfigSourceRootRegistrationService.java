/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.microprofile.config.smallrye;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.services.path.AbsolutePathService;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.microprofile.config.smallrye._private.MicroProfileConfigLogger;

import io.smallrye.config.source.file.FileSystemConfigSource;

/**
 * Service to register a ConfigSourceProvider for a root directory, creating a FileSystemConfigSource for each directory
 * under the given location.
 *
 * @author Kabir Khan
 */
class ConfigSourceRootRegistrationService implements Service {

    private final String name;
    private final String path;
    /**
     * Can be null
     */
    private final String relativeTo;
    private final int ordinal;
    private final Supplier<PathManager> pathManager;
    private final Registry<ConfigSourceProvider> providerRegistry;

    private ConfigSourceRootRegistrationService(String name, String path, String relativeTo, int ordinal, Supplier<PathManager> pathManager, Registry<ConfigSourceProvider> sources) {
        this.name = name;
        this.path = path;
        this.relativeTo = relativeTo;
        this.ordinal = ordinal;
        this.pathManager = pathManager;
        this.providerRegistry = sources;
    }

    static void install(OperationContext context, String name, String path, String relativeTo, int ordinal, Registry<ConfigSourceProvider> registry) {
        ServiceBuilder<?> builder = context.getServiceTarget()
                .addService(ServiceNames.CONFIG_SOURCE_ROOT.append(name));
        Supplier<PathManager> pathManager = builder.requires(context.getCapabilityServiceName("org.wildfly.management.path-manager", PathManager.class));

        builder.setInstance(new ConfigSourceRootRegistrationService(name, path, relativeTo, ordinal, pathManager, registry))
                .install();
    }

    @Override
    public void start(StartContext startContext) {
        String relativeToPath = AbsolutePathService.isAbsoluteUnixOrWindowsPath(path) ? null  : relativeTo;
        String dirPath = pathManager.get().resolveRelativePathEntry(path, relativeToPath);
        File dir = new File(dirPath);
        MicroProfileConfigLogger.ROOT_LOGGER.loadConfigSourceRootFromDir(dir.getAbsolutePath());
        this.providerRegistry.register(this.name, new RootDirectoryConfigSourceProvider(name, dir, ordinal));
    }

    @Override
    public void stop(StopContext context) {
        this.providerRegistry.unregister(this.name);
    }

    private static class RootDirectoryConfigSourceProvider implements ConfigSourceProvider {
        private final String name;
        private final File rootDirectory;
        private int ordinal;

        public RootDirectoryConfigSourceProvider(String name, File rootDirectory, int ordinal) {
            this.name = name;
            this.rootDirectory = rootDirectory;
            this.ordinal = ordinal;
        }

        @Override
        public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
            TreeMap<String, ConfigSource> map = new TreeMap<>();
            File[] files = rootDirectory.listFiles();
            List<String> configSourceDirs = new ArrayList<>();
            if (files != null) {
                // If the directory exist, this will be an empty list
                for (File file : rootDirectory.listFiles()) {
                    if (file.isDirectory()) {
                        map.put(file.getName(), new FileSystemConfigSource(file, ordinal));
                        configSourceDirs.add(file.getAbsolutePath());
                    }
                }
            }
            MicroProfileConfigLogger.ROOT_LOGGER.logDirectoriesUnderConfigSourceRoot(name, configSourceDirs);
            return map.values();
        }
    }

}
