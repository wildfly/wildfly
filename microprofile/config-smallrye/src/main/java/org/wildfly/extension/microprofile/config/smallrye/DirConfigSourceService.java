/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
import java.util.function.Supplier;

import io.smallrye.config.DirConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.services.path.AbsolutePathService;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.microprofile.config.smallrye._private.MicroProfileConfigLogger;

/**
 * Service to install a ConfigSource reading its configuration from a directory (where files are property keys and
 * their content is the property values).
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
class DirConfigSourceService implements Service<ConfigSource> {

    private final String name;
    private final String path;
    /**
     * Can be null
     */
    private final String relativeTo;
    private final int ordinal;
    private final Supplier<PathManager> pathManager;

    private ConfigSource configSource;

    DirConfigSourceService(String name, String path, String relativeTo, int ordinal, Supplier<PathManager> pathManager) {
        this.name = name;
        this.path = path;
        this.relativeTo = relativeTo;
        this.ordinal = ordinal;
        this.pathManager = pathManager;
    }

    static void install(OperationContext context, String name, String path, String relativeTo, int ordinal) {
        ServiceBuilder<?> builder = context.getServiceTarget()
                .addService(ServiceNames.CONFIG_SOURCE.append(name));
        Supplier<PathManager> pathManager = builder.requires(context.getCapabilityServiceName("org.wildfly.management.path-manager", PathManager.class));

        builder.setInstance(new DirConfigSourceService(name, path, relativeTo, ordinal, pathManager))
                .install();
    }

    @Override
    public void start(StartContext startContext) {
        String relativeToPath = AbsolutePathService.isAbsoluteUnixOrWindowsPath(path) ? null  : relativeTo;
        String dirPath = pathManager.get().resolveRelativePathEntry(path, relativeToPath);
        File dir = new File(dirPath);
        MicroProfileConfigLogger.ROOT_LOGGER.loadConfigSourceFromDir(dir.getAbsolutePath());
        configSource = new DirConfigSource(dir, ordinal);
    }

    @Override
    public void stop(StopContext stopContext) {
        configSource = null;
    }

    @Override
    public ConfigSource getValue() throws IllegalStateException, IllegalArgumentException {
        return configSource;
    }
}
