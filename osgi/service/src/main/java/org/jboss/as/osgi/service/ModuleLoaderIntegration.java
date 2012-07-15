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
package org.jboss.as.osgi.service;

import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.server.Services.JBOSS_SERVICE_MODULE_LOADER;
import static org.jboss.as.server.moduleservice.ServiceModuleLoader.MODULE_PREFIX;
import static org.jboss.as.server.moduleservice.ServiceModuleLoader.MODULE_SERVICE_PREFIX;
import static org.jboss.as.server.moduleservice.ServiceModuleLoader.MODULE_SPEC_SERVICE_PREFIX;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.moduleservice.ModuleLoadService;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.BundleManager;
import org.jboss.osgi.framework.IntegrationServices;
import org.jboss.osgi.framework.ModuleLoaderPlugin;
import org.jboss.osgi.framework.TypeAdaptor;
import org.jboss.osgi.resolver.XResource;
import org.osgi.framework.Bundle;

/**
 * This is the single {@link ModuleLoader} that the OSGi layer uses for the modules that are associated with the bundles that
 * are registered with the {@link BundleManager}.
 * <p/>
 * Plain AS7 modules can create dependencies on OSGi deployments, because OSGi modules can also be loaded from the
 * {@link ServiceModuleLoader}
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Apr-2011
 */
final class ModuleLoaderIntegration extends ModuleLoader implements ModuleLoaderPlugin {

    private final InjectedValue<ServiceModuleLoader> injectedModuleLoader = new InjectedValue<ServiceModuleLoader>();
    private ServiceContainer serviceContainer;
    private ServiceTarget serviceTarget;

    static ServiceController<?> addService(final ServiceTarget target) {
        ModuleLoaderIntegration service = new ModuleLoaderIntegration();
        ServiceBuilder<?> builder = target.addService(IntegrationServices.MODULE_LOADER_PLUGIN, service);
        builder.addDependency(JBOSS_SERVICE_MODULE_LOADER, ServiceModuleLoader.class, service.injectedModuleLoader);
        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
    }

    private ModuleLoaderIntegration() {
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        LOGGER.tracef("Starting: %s in mode %s", controller.getName(), controller.getMode());
        serviceContainer = context.getController().getServiceContainer();
        serviceTarget = context.getChildTarget();
    }

    @Override
    public void stop(StopContext context) {
        ServiceController<?> controller = context.getController();
        LOGGER.tracef("Stopping: %s in mode %s", controller.getName(), controller.getMode());
    }

    @Override
    public ModuleLoaderPlugin getValue() throws IllegalStateException {
        return this;
    }

    @Override
    public ModuleLoader getModuleLoader() {
        return this;
    }

    /**
     * Get the module identifier for the given {@link XBundleRevision}. The returned identifier must be such that it can be used
     * by the {@link ServiceModuleLoader}
     */
    @Override
    public ModuleIdentifier getModuleIdentifier(XResource resource, int rev) {
        Bundle bundle = resource.getAttachment(Bundle.class);
        Deployment deployment = ((TypeAdaptor) bundle).adapt(Deployment.class);
        ModuleIdentifier identifier = deployment.getAttachment(ModuleIdentifier.class);
        if (identifier == null) {
            String name = bundle.getSymbolicName();
            if (rev > 0) {
                name += "-rev" + rev;
            }
            String version = bundle.getVersion().toString();
            identifier = ModuleIdentifier.create(MODULE_PREFIX + name, version);
        }
        return identifier;
    }

    @Override
    public void addIntegrationDependencies(ModuleSpecBuilderContext context) {
    }

    /**
     * Add a {@link ModuleSpec} for and OSGi module as a service that can later be looked up by the {@link ServiceModuleLoader}
     */
    @Override
    public void addModuleSpec(XResource resource, ModuleSpec moduleSpec) {
        ModuleIdentifier identifier = moduleSpec.getModuleIdentifier();
        LOGGER.tracef("Add module spec to loader: %s", identifier);
        ServiceName moduleSpecName = getModuleSpecServiceName(identifier);
        serviceTarget.addService(moduleSpecName, new ValueService<ModuleSpec>(new ImmediateValue<ModuleSpec>(moduleSpec))).install();

        // Install the alias [symbolic-name:version]
        ModuleIdentifier aliasIdentifier = getModuleAliasIdentifier(resource);
        if (aliasIdentifier != null && !aliasIdentifier.equals(identifier)) {
            ServiceName aliasSpecName = getModuleSpecServiceName(aliasIdentifier);
            ModuleSpec aliasSpec = ModuleSpec.buildAlias(aliasIdentifier, identifier).create();
            serviceTarget.addService(aliasSpecName, new ValueService<ModuleSpec>(new ImmediateValue<ModuleSpec>(aliasSpec))).install();
        }
    }

    /**
     * Add an already loaded {@link Module} to the OSGi {@link ModuleLoader}. This happens when AS registers an existing
     * {@link Module} with the {@link BundleManager}.
     * <p/>
     * The {@link Module} may not necessarily result from a user deployment. We use the same {@link ServiceName} convention as
     * in {@link ServiceModuleLoader#moduleServiceName(ModuleIdentifier)}
     * <p/>
     * The {@link ServiceModuleLoader} cannot load these modules.
     */
    @Override
    public void addModule(XResource resource, Module module) {
        ServiceName moduleServiceName = getModuleServiceName(module.getIdentifier());
        if (serviceContainer.getService(moduleServiceName) == null) {
            LOGGER.debugf("Add module to loader: %s", module.getIdentifier());
            serviceTarget.addService(moduleServiceName, new ValueService<Module>(new ImmediateValue<Module>(module))).install();
        }
    }

    @Override
    public ServiceName createModuleService(XResource resource, ModuleIdentifier identifier) {
        List<ModuleDependency> dependencies = Collections.emptyList();
        return ModuleLoadService.install(serviceTarget, identifier, dependencies);
    }

    /**
     * Remove the {@link Module} and {@link ModuleSpec} services associated with the given identifier.
     */
    @Override
    public void removeModule(XResource resource, ModuleIdentifier identifier) {
        Set<ServiceName> serviceNames = new HashSet<ServiceName>();
        serviceNames.add(getModuleSpecServiceName(identifier));
        serviceNames.add(getModuleServiceName(identifier));

        ModuleIdentifier aliasIdentifier = getModuleAliasIdentifier(resource);
        if (aliasIdentifier != null) {
            serviceNames.add(getModuleSpecServiceName(aliasIdentifier));
            serviceNames.add(getModuleServiceName(aliasIdentifier));
        }

        for (ServiceName serviceName : serviceNames) {
            ServiceController<?> controller = serviceContainer.getService(serviceName);
            if (controller != null) {
                LOGGER.debugf("Remove from loader: %s", serviceName);
                controller.setMode(Mode.REMOVE);
            }
        }
    }

    @Override
    protected ModuleSpec findModule(ModuleIdentifier identifier) throws ModuleLoadException {
        ModuleSpec moduleSpec = injectedModuleLoader.getValue().findModule(identifier);
        if (moduleSpec == null)
            LOGGER.debugf("Cannot obtain module spec for: %s", identifier);
        return moduleSpec;
    }

    @Override
    protected Module preloadModule(ModuleIdentifier identifier) throws ModuleLoadException {
        Module module = ModuleLoader.preloadModule(identifier, injectedModuleLoader.getValue());
        if (module == null)
            LOGGER.debugf("Cannot obtain module for: %s", identifier);
        return module;
    }

    @Override
    public void setAndRelinkDependencies(Module module, List<DependencySpec> dependencies) throws ModuleLoadException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceName getModuleServiceName(ModuleIdentifier identifier) {
        return MODULE_SERVICE_PREFIX.append(identifier.getName()).append(identifier.getSlot());
    }

    private ServiceName getModuleSpecServiceName(ModuleIdentifier identifier) {
        return MODULE_SPEC_SERVICE_PREFIX.append(identifier.getName()).append(identifier.getSlot());
    }

    private ModuleIdentifier getModuleAliasIdentifier(XResource resource) {
        Bundle bundle = resource.getAttachment(Bundle.class);
        String name = bundle.getSymbolicName();
        String version = bundle.getVersion().toString();
        return (name != null ? ModuleIdentifier.create(MODULE_PREFIX + name, version) : null);
    }

    @Override
    public String toString() {
        return ModuleLoaderIntegration.class.getSimpleName();
    }
}
