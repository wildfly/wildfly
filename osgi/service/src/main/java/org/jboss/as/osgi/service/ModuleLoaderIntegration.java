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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.server.deployment.module.FilterSpecification;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.moduleservice.ModuleLoadService;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.ModuleSpec.Builder;
import org.jboss.modules.filter.MultiplePathFilterBuilder;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
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
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XIdentityCapability;

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
    public ModuleIdentifier getModuleIdentifier(XBundleRevision brev) {
        XBundle bundle = brev.getBundle();
        Deployment deployment = bundle.adapt(Deployment.class);
        ModuleIdentifier identifier = deployment.getAttachment(ModuleIdentifier.class);
        if (identifier == null) {
            XIdentityCapability icap = brev.getIdentityCapability();
            List<XBundleRevision> allrevs = bundle.getAllBundleRevisions();
            String name = icap.getSymbolicName();
            if (allrevs.size() > 1) {
                name += "-rev" + (allrevs.size() - 1);
            }
            identifier = ModuleIdentifier.create(MODULE_PREFIX + name, "" + icap.getVersion());
        }
        return identifier;
    }

    @Override
    public void addIntegrationDependencies(ModuleSpecBuilderContext context) {
        Builder builder = context.getModuleSpecBuilder();
        XBundleRevision brev = context.getBundleRevision();
        Map<ModuleIdentifier, DependencySpec> moduleDependencies = context.getModuleDependencies();
        Deployment deployment = brev.getBundle().adapt(Deployment.class);
        ModuleSpecification moduleSpecification = deployment.getAttachment(ModuleSpecification.class);
        if (moduleSpecification != null) {
            List<ModuleDependency> dependencies = moduleSpecification.getAllDependencies();
            LOGGER.debugf("Adding integration dependencies: %d", dependencies.size());
            for (ModuleDependency moduleDep : dependencies) {
                ModuleIdentifier moduleId = moduleDep.getIdentifier();
                if (moduleDependencies.get(moduleId) != null) {
                    LOGGER.debugf("  -dependency on %s (skipped)", moduleId);
                    continue;
                }
                // Build import filter
                MultiplePathFilterBuilder importBuilder = PathFilters.multiplePathFilterBuilder(true);
                for (FilterSpecification filter : moduleDep.getImportFilters()) {
                    importBuilder.addFilter(filter.getPathFilter(), filter.isInclude());
                }
                PathFilter importFilter = importBuilder.create();
                // Build export filter
                MultiplePathFilterBuilder exportBuilder = PathFilters.multiplePathFilterBuilder(true);
                for (FilterSpecification filter : moduleDep.getExportFilters()) {
                    importBuilder.addFilter(filter.getPathFilter(), filter.isInclude());
                }
                PathFilter exportFilter = exportBuilder.create();
                ModuleLoader moduleLoader = moduleDep.getModuleLoader();
                boolean optional = moduleDep.isOptional();
                DependencySpec depSpec = DependencySpec.createModuleDependencySpec(importFilter, exportFilter, moduleLoader, moduleId, optional);
                LOGGER.debugf("  +%s", depSpec);
                builder.addDependency(depSpec);
            }
        }
    }

    /**
     * Add a {@link ModuleSpec} for and OSGi module as a service that can later be looked up by the {@link ServiceModuleLoader}
     */
    @Override
    public void addModuleSpec(XBundleRevision brev, final ModuleSpec moduleSpec) {
        ModuleIdentifier identifier = moduleSpec.getModuleIdentifier();
        LOGGER.tracef("Add module spec to loader: %s", identifier);
        ServiceName moduleSpecName = ServiceModuleLoader.moduleSpecServiceName(identifier);
        ImmediateValue<ModuleSpec> value = new ImmediateValue<ModuleSpec>(moduleSpec);
        serviceTarget.addService(moduleSpecName, new ValueService<ModuleSpec>(value)).install();
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
    public void addModule(XBundleRevision brev, final Module module) {
        ServiceName moduleServiceName = getModuleServiceName(module.getIdentifier());
        if (serviceContainer.getService(moduleServiceName) == null) {
            LOGGER.debugf("Add module to loader: %s", module.getIdentifier());
            serviceTarget.addService(moduleServiceName, new ValueService<Module>(new ImmediateValue<Module>(module))).install();
        }
    }

    @Override
    public ServiceName createModuleService(XBundleRevision brev, ModuleIdentifier identifier) {
        List<ModuleDependency> dependencies = Collections.emptyList();
        return ModuleLoadService.install(serviceTarget, identifier, dependencies);
    }

    /**
     * Remove the {@link Module} and {@link ModuleSpec} services associated with the given identifier.
     */
    @Override
    public void removeModule(XBundleRevision brev, ModuleIdentifier identifier) {
        Set<ServiceName> serviceNames = new HashSet<ServiceName>();
        serviceNames.add(getModuleSpecServiceName(identifier));
        serviceNames.add(getModuleServiceName(identifier));
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
        return ServiceModuleLoader.moduleServiceName(identifier);
    }

    private ServiceName getModuleSpecServiceName(ModuleIdentifier identifier) {
        return ServiceModuleLoader.moduleSpecServiceName(identifier);
    }

    @Override
    public String toString() {
        return ModuleLoaderIntegration.class.getSimpleName();
    }
}
