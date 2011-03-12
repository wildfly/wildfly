/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.server.moduleservice;

import org.jboss.modules.DependencySpec;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.ResourceLoaders;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.VFSUtils;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

/**
 * Service that manages the module spec for external modules (i.e. modules that reside outside of the application server).
 *
 * @author Stuart Douglas
 *
 */
public class ExternalModuleSpecService implements Service<ModuleSpec> {

    private final ModuleIdentifier moduleIdentifier;

    private final File file;

    private volatile ModuleSpec moduleSpec;

    private volatile JarFile jarFile;

    public ExternalModuleSpecService(ModuleIdentifier moduleIdentifier, File file) {
        this.moduleIdentifier = moduleIdentifier;
        this.file = file;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        try {
            this.jarFile = new JarFile(file);
        } catch (IOException e) {
            throw new StartException(e);
        }
        final ModuleSpec.Builder specBuilder = ModuleSpec.build(moduleIdentifier);
        addResourceRoot(specBuilder, jarFile);
        //TODO: We need some way of configuring module dependencies for external archives
        specBuilder.addDependency(DependencySpec.createModuleDependencySpec(ModuleIdentifier.create("javaee.api")));
        specBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        // TODO: external resource need some kind of dependency mechanism
        moduleSpec = specBuilder.create();
    }

    @Override
    public synchronized void stop(StopContext context) {
        VFSUtils.safeClose(jarFile);
        jarFile = null;
        moduleSpec = null;
    }

    @Override
    public ModuleSpec getValue() throws IllegalStateException, IllegalArgumentException {
        return moduleSpec;
    }

    private static void addResourceRoot(final ModuleSpec.Builder specBuilder, final JarFile file) {
        specBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(ResourceLoaders.createJarResourceLoader(
                    file.getName(), file)));
    }

}
