/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.deployment.item;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.io.IOException;

/**
 * Service that deploys a module into a module loader.
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author John E. Bailey
 */
public final class ModuleDeploymentService implements Service<Module> {

    private final ModuleDeploymentItem moduleDeploymentItem;
    private final ModuleLoader moduleLoader;

    public static final ServiceName MODULE_DEPLOYMENT_SERVICE = ServiceName.JBOSS.append("module", "service");

    public ModuleDeploymentService(final ModuleDeploymentItem moduleDeploymentItem, final ModuleLoader moduleLoader) {
        this.moduleDeploymentItem = moduleDeploymentItem;
        this.moduleLoader = moduleLoader;
    }

    public void start(final StartContext context) throws StartException {
        final ModuleSpec.Builder specBuilder = ModuleSpec.build(moduleDeploymentItem.getIdentifier());
        for(ModuleDeploymentItem.ResourceRoot resource : moduleDeploymentItem.getResources()) {
            try {
                specBuilder.addRoot(resource.getRootName(), new VFSResourceLoader(specBuilder.getIdentifier(), resource.getRoot()));
            } catch (IOException e) {
                throw new StartException("Failed to create module roots", e);
            }
        }
        final ModuleDeploymentItem.Dependency[] dependencies = moduleDeploymentItem.getDependencies();
        for(ModuleDeploymentItem.Dependency dependency : dependencies) {
            specBuilder.addDependency(dependency.getIdentifier())
                .setExport(dependency.isExport())
                .setOptional(dependency.isOptional());
        }
        final ModuleSpec moduleSpec = specBuilder.create();
        // Somehow jam the spec into the provided loaded//
        //((DynamicModuleLoader)moduleLoader).addSpec(moduleSpec);
    }

    public void stop(final StopContext context) {
    }

    public Module getValue() throws IllegalStateException {
        try {
            return moduleLoader.loadModule(moduleDeploymentItem.getIdentifier());
        } catch (ModuleLoadException e) {
            throw new IllegalStateException("Unable to load module value", e);
        }
    }
}
