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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.server.deployment.DeploymentUnit;
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
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.spi.BundleManager;
import org.jboss.osgi.framework.spi.FrameworkModuleLoader;
import org.jboss.osgi.framework.spi.FrameworkModuleLoaderPlugin;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.osgi.framework.wiring.BundleWire;

/**
 * This is the single {@link ModuleLoader} that the OSGi layer uses for the modules that are associated with the bundles that
 * are registered with the {@link BundleManager}.
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Apr-2011
 */
final class ModuleLoaderIntegration extends FrameworkModuleLoaderPlugin {

    private final InjectedValue<ServiceModuleLoader> injectedModuleLoader = new InjectedValue<ServiceModuleLoader>();
    private ServiceContainer serviceContainer;
    private ServiceTarget serviceTarget;

    @Override
    protected void addServiceDependencies(ServiceBuilder<FrameworkModuleLoader> builder) {
        super.addServiceDependencies(builder);
        builder.addDependency(JBOSS_SERVICE_MODULE_LOADER, ServiceModuleLoader.class, injectedModuleLoader);
    }

    @Override
    public void start(StartContext context) throws StartException {
        serviceContainer = context.getController().getServiceContainer();
        serviceTarget = context.getChildTarget();
        super.start(context);
    }

    @Override
    protected FrameworkModuleLoader createServiceValue(StartContext startContext) {
        return new FrameworkModuleLoaderImpl();
    }

    class FrameworkModuleLoaderImpl implements FrameworkModuleLoader {

        @Override
        public ModuleLoader getModuleLoader() {
            class DelegatingModuleLoader extends ModuleLoader {

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
                public String toString() {
                    return ModuleLoaderIntegration.class.getSimpleName() + "." + getClass().getSimpleName();
                }
            }
            return new DelegatingModuleLoader();
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
                identifier = ModuleIdentifier.create(MODULE_PREFIX + name, brev.getVersion().toString());
            }
            return identifier;
        }

        @Override
        public void addIntegrationDependencies(ModuleSpecBuilderContext context) {
            // no nothing
        }

        /**
         * Add a {@link ModuleSpec} for and OSGi module as a service that can later be looked up by the {@link ServiceModuleLoader}
         */
        @Override
        public void addModuleSpec(XBundleRevision brev, final ModuleSpec moduleSpec) {
            ModuleIdentifier moduleId = moduleSpec.getModuleIdentifier();
            LOGGER.tracef("Add module spec to loader: %s", moduleId);
            ServiceName serviceName = getModuleSpecServiceName(moduleId);
            Value<ModuleSpec> value = new ImmediateValue<ModuleSpec>(moduleSpec);
            serviceTarget.addService(serviceName, new ValueService<ModuleSpec>(value)).install();
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
            ModuleIdentifier moduleId = module.getIdentifier();
            ServiceName moduleServiceName = getModuleServiceName(moduleId);
            if (serviceContainer.getService(moduleServiceName) == null) {
                LOGGER.debugf("Add module to loader: %s", moduleId);
                ValueService<Module> service = new ValueService<Module>(new ImmediateValue<Module>(module));
                ServiceBuilder<Module> builder = serviceTarget.addService(moduleServiceName, service);
                builder.install();
            }
        }

        @Override
        public ServiceName createModuleService(XBundleRevision brev, List<BundleWire> wires) {
            Deployment deployment = brev.getBundle().adapt(Deployment.class);
            DeploymentUnit depUnit = deployment.getAttachment(DeploymentUnit.class);

            // Add a dependency on the parent module if we have one
            List<ModuleDependency> dependencies = new ArrayList<ModuleDependency>();
            if (depUnit != null && depUnit.getParent() != null) {
                String parentName = depUnit.getParent().getName();
                ModuleIdentifier depId = ModuleIdentifier.create(MODULE_PREFIX + parentName);
                dependencies.add(new ModuleDependency(null, depId, false, false, false, false));
            }

            // Add dependencies on all modules this brev has a wire to
            for (BundleWire wire : wires) {
                XBundleRevision provider = (XBundleRevision) wire.getProvider();
                ModuleIdentifier providerid = provider.getModuleIdentifier();
                dependencies.add(new ModuleDependency(null, providerid, false, false, false, false));
            }

            ModuleIdentifier identifier = brev.getModuleIdentifier();
            return ModuleLoadService.install(serviceTarget, identifier, dependencies);
        }

        /**
         * Remove the {@link Module} and {@link ModuleSpec} services associated with the given identifier.
         */
        @Override
        public void removeModule(XBundleRevision brev) {
            Set<ServiceName> serviceNames = new HashSet<ServiceName>();
            ModuleIdentifier identifier = brev.getModuleIdentifier();
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
 }
