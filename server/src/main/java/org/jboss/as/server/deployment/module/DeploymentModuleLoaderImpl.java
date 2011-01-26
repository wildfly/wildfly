/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.module;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.as.server.Bootstrap;
import org.jboss.as.server.Services;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Default deployment module loader.  Maintains a map of module specs that can be loaded at a later time.
 *
 * @author John E. Bailey
 */
public class DeploymentModuleLoaderImpl extends DeploymentModuleLoader implements Service<DeploymentModuleLoader> {
    private final ConcurrentMap<ModuleIdentifier, ModuleSpec> moduleSpecs = new ConcurrentHashMap<ModuleIdentifier, ModuleSpec>();

    private final ModuleLoader mainModuleLoader;

    private DeploymentModuleLoaderImpl(final ModuleLoader mainModuleLoader) {
        this.mainModuleLoader = mainModuleLoader;
    }

    public static void addService(final ServiceTarget serviceTarget, final Bootstrap.Configuration configuration) {
        addService(serviceTarget, configuration.getModuleLoader());
    }

    public static void addService(final ServiceTarget serviceTarget, final ModuleLoader mainModuleLoader) {
        Service<DeploymentModuleLoader> service = new DeploymentModuleLoaderImpl(mainModuleLoader);
        ServiceBuilder<?> serviceBuilder = serviceTarget.addService(Services.JBOSS_DEPLOYMENT_MODULE_LOADER, service);
        serviceBuilder.install();
    }

    @Override
    public void addModuleSpec(ModuleSpec moduleSpec) {
        if(moduleSpecs.putIfAbsent(moduleSpec.getModuleIdentifier(), moduleSpec) != null) {
            throw new IllegalArgumentException("Module spec has already been added for identifier [" + moduleSpec.getModuleIdentifier() + "]");
        }
    }

    @Override
    public ModuleSpec removeModuleSpec(ModuleIdentifier moduleId) {
       return moduleSpecs.remove(moduleId);
   }

    @Override
    protected Module preloadModule(final ModuleIdentifier identifier) throws ModuleLoadException {
        if (identifier.getName().startsWith("deployment.")) {
            return super.preloadModule(identifier);
        } else {
            return preloadModule(identifier, mainModuleLoader);
        }
    }

    @Override
    protected ModuleSpec findModule(ModuleIdentifier moduleIdentifier) throws ModuleLoadException {
        final ConcurrentMap<ModuleIdentifier, ModuleSpec> moduleSpecs = this.moduleSpecs;
        ModuleSpec moduleSpec = moduleSpecs.get(moduleIdentifier);
        return moduleSpec;
    }

    @Override
    public void removeModule(Module module) {
        if (moduleSpecs.remove(module.getIdentifier()) != null) {
            unloadModuleLocal(module);
        }

        else throw new IllegalStateException("Unknown module " + module);
    }

    @Override
    public void relinkModule(Module module) throws ModuleLoadException {
        relink(module);
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public DeploymentModuleLoader getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public String toString() {
        return "as-deployment";
    }
}
