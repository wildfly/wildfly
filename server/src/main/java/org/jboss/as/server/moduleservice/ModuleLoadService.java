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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleNotFoundException;
import org.jboss.modules.ModuleSpec;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import static org.jboss.as.server.ServerLogger.PRIVATE_DEP_LOGGER;
import static org.jboss.as.server.ServerLogger.UNSUPPORTED_DEP_LOGGER;

/**
 * Service that loads and re-links a module once all the modules dependencies are available.
 *
 * @author Stuart Douglas
 */
public class ModuleLoadService implements Service<Module> {

    private final InjectedValue<ServiceModuleLoader> serviceModuleLoader = new InjectedValue<ServiceModuleLoader>();
    private final InjectedValue<ModuleSpec> moduleSpec = new InjectedValue<ModuleSpec>();
    private final List<ModuleIdentifier> dependencies;

    private volatile Module module;

    private ModuleLoadService(final List<ModuleIdentifier> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        try {
            final ServiceModuleLoader moduleLoader = serviceModuleLoader.getValue();
            final Module module = moduleLoader.loadModule(moduleSpec.getValue().getModuleIdentifier());
            moduleLoader.relinkModule(module);
            for (ModuleIdentifier id : dependencies) {
                try {
                    String val = moduleLoader.loadModule(id).getProperty("jboss.api");
                    if (val != null) {
                        if (val.equals("private")) {
                            PRIVATE_DEP_LOGGER.privateApiUsed(moduleSpec.getValue().getModuleIdentifier().getName(), id);
                        } else if (val.equals("unsupported")) {
                            UNSUPPORTED_DEP_LOGGER.unsupportedApiUsed(moduleSpec.getValue().getModuleIdentifier().getName(), id);
                        }
                    }
                } catch (ModuleNotFoundException ignore) {
                    //can happen with optional dependencies
                }
            }
            this.module = module;
        } catch (ModuleLoadException e) {
            throw new StartException("Failed to load module: " + moduleSpec.getValue().getModuleIdentifier(), e);
        }
    }

    @Override
    public synchronized void stop(StopContext context) {
        // we don't actually unload the module, that is taken care of by the service module loader
        module = null;
    }

    @Override
    public Module getValue() throws IllegalStateException, IllegalArgumentException {
        return module;
    }

    public static ServiceName install(final ServiceTarget target, final ModuleIdentifier identifier, final List<ModuleDependency> dependencies) {
        final Map<ModuleIdentifier, ServiceBuilder.DependencyType> deps = new HashMap<ModuleIdentifier, ServiceBuilder.DependencyType>();
        for (final ModuleDependency i : dependencies) {
            deps.put(i.getIdentifier(), i.isOptional() ? ServiceBuilder.DependencyType.OPTIONAL : ServiceBuilder.DependencyType.REQUIRED);
        }
        return installService(target, identifier, deps);
    }

    public static ServiceName installService(final ServiceTarget target, final ModuleIdentifier identifier, final List<ModuleIdentifier> dependencies) {
        final Map<ModuleIdentifier, ServiceBuilder.DependencyType> deps = new HashMap<ModuleIdentifier, ServiceBuilder.DependencyType>();
        for (final ModuleIdentifier i : dependencies) {
            deps.put(i, ServiceBuilder.DependencyType.REQUIRED);
        }
        return installService(target, identifier, deps);
    }

    private static ServiceName installService(final ServiceTarget target, final ModuleIdentifier identifier, final Map<ModuleIdentifier, ServiceBuilder.DependencyType> dependencies) {
        final ModuleLoadService service = new ModuleLoadService(new ArrayList<ModuleIdentifier>(dependencies.keySet()));
        final ServiceName serviceName = ServiceModuleLoader.moduleServiceName(identifier);
        final ServiceBuilder<Module> builder = target.addService(serviceName, service);
        builder.addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ServiceModuleLoader.class, service.getServiceModuleLoader());
        builder.addDependency(ServiceModuleLoader.moduleSpecServiceName(identifier), ModuleSpec.class, service.getModuleSpec());
        for (final Map.Entry<ModuleIdentifier, ServiceBuilder.DependencyType> entry : dependencies.entrySet()) {
            if (entry.getKey().getName().startsWith(ServiceModuleLoader.MODULE_PREFIX)) {
                builder.addDependency(entry.getValue(), ServiceModuleLoader.moduleSpecServiceName(entry.getKey()));
            }
        }
        builder.setInitialMode(Mode.ON_DEMAND);
        builder.install();
        return serviceName;
    }

    public InjectedValue<ServiceModuleLoader> getServiceModuleLoader() {
        return serviceModuleLoader;
    }

    public InjectedValue<ModuleSpec> getModuleSpec() {
        return moduleSpec;
    }
}
