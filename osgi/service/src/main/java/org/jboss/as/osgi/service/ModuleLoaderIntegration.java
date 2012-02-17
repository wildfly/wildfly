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

import java.util.List;

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
import org.jboss.osgi.framework.BundleManagerService;
import org.jboss.osgi.framework.ModuleLoaderProvider;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.resolver.XModule;
import org.jboss.osgi.resolver.XModuleIdentity;
import org.jboss.osgi.spi.NotImplementedException;

import static org.jboss.as.osgi.OSGiLogger.ROOT_LOGGER;
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;
import static org.jboss.as.server.Services.JBOSS_SERVICE_MODULE_LOADER;
import static org.jboss.as.server.moduleservice.ServiceModuleLoader.MODULE_SERVICE_PREFIX;
import static org.jboss.as.server.moduleservice.ServiceModuleLoader.MODULE_SPEC_SERVICE_PREFIX;

/**
 * This is the single {@link ModuleLoader} that the OSGi layer uses for the modules that are associated with the bundles that
 * are registered with the {@link BundleManagerService}.
 * <p/>
 * Plain AS7 modules can create dependencies on OSGi deployments, because OSGi modules can also be loaded from the
 * {@link ServiceModuleLoader}
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Apr-2011
 */
final class ModuleLoaderIntegration extends ModuleLoader implements ModuleLoaderProvider {

    private final InjectedValue<ServiceModuleLoader> injectedModuleLoader = new InjectedValue<ServiceModuleLoader>();
    private ServiceContainer serviceContainer;
    private ServiceTarget serviceTarget;

    static ServiceController<?> addService(final ServiceTarget target) {
        ModuleLoaderIntegration service = new ModuleLoaderIntegration();
        ServiceBuilder<?> builder = target.addService(Services.MODULE_LOADER_PROVIDER, service);
        builder.addDependency(JBOSS_SERVICE_MODULE_LOADER, ServiceModuleLoader.class, service.injectedModuleLoader);
        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
    }

    private ModuleLoaderIntegration() {
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        ROOT_LOGGER.debugf("Starting: %s in mode %s", controller.getName(), controller.getMode());
        serviceContainer = context.getController().getServiceContainer();
        serviceTarget = context.getChildTarget();
    }

    @Override
    public void stop(StopContext context) {
        ServiceController<?> controller = context.getController();
        ROOT_LOGGER.debugf("Stopping: %s in mode %s", controller.getName(), controller.getMode());
    }

    @Override
    public ModuleLoaderProvider getValue() throws IllegalStateException {
        return this;
    }

    @Override
    public ModuleLoader getModuleLoader() {
        return this;
    }

    /**
     * Add a {@link ModuleSpec} for and OSGi module as a service that can later be looked up by the {@link ServiceModuleLoader}
     */
    @Override
    public void addModule(final ModuleSpec moduleSpec) {
        ModuleIdentifier identifier = moduleSpec.getModuleIdentifier();
        ROOT_LOGGER.debugf("Add module spec to loader: %s", identifier);

        ServiceName moduleSpecName = ServiceModuleLoader.moduleSpecServiceName(identifier);
        serviceTarget.addService(moduleSpecName, new ValueService<ModuleSpec>(new ImmediateValue<ModuleSpec>(moduleSpec))).install();
    }

    /**
     * Add an already loaded {@link Module} to the OSGi {@link ModuleLoader}. This happens when AS registers an existing
     * {@link Module} with the {@link BundleManagerService}.
     * <p/>
     * The {@link Module} may not necessarily result from a user deployment. We use the same {@link ServiceName} convention as
     * in {@link ServiceModuleLoader#moduleServiceName(ModuleIdentifier)}
     * <p/>
     * The {@link ServiceModuleLoader} cannot load these modules.
     */
    @Override
    public void addModule(final Module module) {
        ServiceName moduleServiceName = getModuleServiceName(module.getIdentifier());
        if (serviceContainer.getService(moduleServiceName) == null) {
            ROOT_LOGGER.debugf("Add module to loader: %s", module.getIdentifier());
            serviceTarget.addService(moduleServiceName, new ValueService<Module>(new ImmediateValue<Module>(module))).install();
        }
    }

    /**
     * Remove the {@link Module} and {@link ModuleSpec} services associated with the given identifier.
     */
    @Override
    public void removeModule(ModuleIdentifier identifier) {
        ServiceName serviceName = getModuleSpecServiceName(identifier);
        ServiceController<?> controller = serviceContainer.getService(serviceName);
        if (controller != null) {
            ROOT_LOGGER.debugf("Remove module spec fom loader: %s", serviceName);
            controller.setMode(Mode.REMOVE);
        }
        serviceName = getModuleServiceName(identifier);
        controller = serviceContainer.getService(serviceName);
        if (controller != null) {
            ROOT_LOGGER.debugf("Remove module fom loader: %s", serviceName);
            controller.setMode(Mode.REMOVE);
        }
    }

    /**
     * Get the module identifier for the given {@link XModule} The returned identifier must be such that it can be used by the
     * {@link ServiceModuleLoader}
     */
    @Override
    public ModuleIdentifier getModuleIdentifier(XModule resModule) {
        if (resModule == null)
            throw MESSAGES.nullVar("resModule");

        XModuleIdentity moduleId = resModule.getModuleId();

        String slot = moduleId.getVersion().toString();
        int revision = moduleId.getRevision();
        if (revision > 0)
            slot += "-rev" + revision;

        String name = ServiceModuleLoader.MODULE_PREFIX + moduleId.getName();
        ModuleIdentifier identifier = ModuleIdentifier.create(name, slot);
        resModule.addAttachment(ModuleIdentifier.class, identifier);

        return identifier;
    }

    @Override
    protected ModuleSpec findModule(ModuleIdentifier identifier) throws ModuleLoadException {
        ModuleSpec moduleSpec = injectedModuleLoader.getValue().findModule(identifier);
        if (moduleSpec == null)
            ROOT_LOGGER.debugf("Cannot obtain module spec for: %s", identifier);
        return moduleSpec;
    }

    @Override
    protected Module preloadModule(ModuleIdentifier identifier) throws ModuleLoadException {
        Module module = ModuleLoader.preloadModule(identifier, injectedModuleLoader.getValue());
        if (module == null)
            ROOT_LOGGER.debugf("Cannot obtain module for: %s", identifier);
        return module;
    }

    @Override
    public void setAndRelinkDependencies(Module module, List<DependencySpec> dependencies) throws ModuleLoadException {
        throw new NotImplementedException();
    }

    private ServiceName getModuleSpecServiceName(ModuleIdentifier identifier) {
        return MODULE_SPEC_SERVICE_PREFIX.append(identifier.getName()).append(identifier.getSlot());
    }

    private ServiceName getModuleServiceName(ModuleIdentifier identifier) {
        return MODULE_SERVICE_PREFIX.append(identifier.getName()).append(identifier.getSlot());
    }

    @Override
    public String toString() {
        return ModuleLoaderIntegration.class.getSimpleName();
    }
}
