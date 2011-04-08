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

import static org.jboss.as.osgi.parser.SubsystemState.PROP_JBOSS_OSGI_SYSTEM_MODULES;
import static org.jboss.as.server.Services.JBOSS_SERVICE_MODULE_LOADER;
import static org.jboss.osgi.framework.Constants.JBOSGI_PREFIX;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;

import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.osgi.parser.SubsystemState;
import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.as.osgi.parser.SubsystemState.OSGiModule;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.as.server.services.net.SocketBinding;
import org.jboss.logging.Logger;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.filter.PathFilters;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.AutoInstallProvider;
import org.jboss.osgi.framework.BundleManagement;
import org.jboss.osgi.framework.BundleReferenceClassLoader;
import org.jboss.osgi.framework.FrameworkModuleProvider;
import org.jboss.osgi.framework.ModuleLoaderProvider;
import org.jboss.osgi.framework.ServiceNames;
import org.jboss.osgi.framework.internal.FrameworkBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * Service responsible for creating and managing the life-cycle of the OSGi Framework.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
public class FrameworkBootstrapService implements Service<Void> {

    public static final ServiceName SERVICE_BASE_NAME = ServiceName.JBOSS.append("osgi", "as");
    public static final ServiceName FRAMEWORK_BASE_NAME = SERVICE_BASE_NAME.append("framework");
    public static final ServiceName FRAMEWORK_BOOTSTRAP = FRAMEWORK_BASE_NAME.append("bootstrap");

    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    private final InjectedValue<ServerEnvironment> injectedEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<SocketBinding> httpServerPortBinding = new InjectedValue<SocketBinding>();
    private final SubsystemState subsystemState;

    public static void addService(final ServiceTarget target, final SubsystemState subsystemState) {
        FrameworkBootstrapService service = new FrameworkBootstrapService(subsystemState);
        ServiceBuilder<?> builder = target.addService(FRAMEWORK_BOOTSTRAP, service);
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedEnvironment);
        builder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append("osgi-http"), SocketBinding.class, service.httpServerPortBinding);
        builder.install();

        AutoInstallIntegration.addService(target, subsystemState);
        FrameworkModuleIntegration.addService(target, subsystemState);
        ModuleLoaderIntegration.addService(target);
        SystemServicesIntegration.addService(target);
    }

    private FrameworkBootstrapService(SubsystemState subsystemState) {
        this.subsystemState = subsystemState;
    }

    public synchronized void start(StartContext context) throws StartException {
        try {
            ServiceContainer serviceContainer = context.getController().getServiceContainer();

            // Setup the OSGi {@link Framework} properties
            Map<String, Object> props = new HashMap<String, Object>(subsystemState.getProperties());
            setupIntegrationProperties(context, props);

            // Start the OSGi {@link Framework}
            FrameworkBuilder builder = new FrameworkBuilder(props);
            builder.setServiceContainer(serviceContainer);
            builder.setServiceTarget(context.getChildTarget());
            builder.addProvidedService(org.jboss.osgi.framework.ServiceNames.AUTOINSTALL_PROVIDER);
            builder.addProvidedService(org.jboss.osgi.framework.ServiceNames.DEPLOYERSERVICE_PROVIDER);
            builder.addProvidedService(ServiceNames.FRAMEWORK_MODULE_PROVIDER);
            builder.addProvidedService(ServiceNames.MODULE_LOADER_PROVIDER);

            Activation activation = subsystemState.getActivationPolicy();
            Mode initialMode = (activation == Activation.EAGER ? Mode.ACTIVE : Mode.ON_DEMAND);
            builder.createFrameworkServices(initialMode, true);
        } catch (Throwable t) {
            throw new StartException("Failed to create Framework services", t);
        }
    }

    public synchronized void stop(StopContext context) {
        log.infof("Stopping OSGi Framework");
    }

    @Override
    public Void getValue() throws IllegalStateException {
        return null;
    }

    private void setupIntegrationProperties(StartContext context, Map<String, Object> props) {

        // Configure the OSGi HttpService port
        // [TODO] This will go away once the HTTP subsystem from AS implements the OSGi HttpService.
        props.put("org.osgi.service.http.port", "" + httpServerPortBinding.getValue().getSocketAddress().getPort());

        // Setup the Framework's storage area. Always clean the framework storage on first init.
        // [TODO] Differentiate beetween user data and persisted bundles. Persist bundle state in the domain model.
        props.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        String storage = (String) props.get(Constants.FRAMEWORK_STORAGE);
        if (storage == null) {
            ServerEnvironment environment = injectedEnvironment.getValue();
            File dataDir = environment.getServerDataDir();
            storage = dataDir.getAbsolutePath() + File.separator + "osgi-store";
            props.put(Constants.FRAMEWORK_STORAGE, storage);
        }
    }

    private static final class SystemServicesIntegration extends AbstractService<Void> {

        static final ServiceName SYSTEM_SERVICES = FRAMEWORK_BASE_NAME.append("services");

        private final InjectedValue<MBeanServer> injectedMBeanServer = new InjectedValue<MBeanServer>();
        private final InjectedValue<BundleContext> injectedSystemContext = new InjectedValue<BundleContext>();
        private ServiceContainer serviceContainer;

        public static void addService(final ServiceTarget target) {
            SystemServicesIntegration service = new SystemServicesIntegration();
            ServiceBuilder<?> builder = target.addService(SYSTEM_SERVICES, service);
            builder.addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, service.injectedMBeanServer);
            builder.addDependency(ServiceNames.SYSTEM_CONTEXT, BundleContext.class, service.injectedSystemContext);
            builder.setInitialMode(Mode.ON_DEMAND);
            builder.install();
        }

        private SystemServicesIntegration() {
        }

        @Override
        public void start(StartContext context) throws StartException {

            // Register the {@link MBeanServer} as OSGi service
            MBeanServer mbeanServer = injectedMBeanServer.getValue();
            BundleContext systemContext = injectedSystemContext.getValue();
            systemContext.registerService(MBeanServer.class.getName(), mbeanServer, null);

            // Register the {@link ServiceContainer} as OSGi service
            serviceContainer = context.getController().getServiceContainer();
            systemContext.registerService(ServiceContainer.class.getName(), serviceContainer, null);
        }
    }

    private static final class ModuleLoaderIntegration extends AbstractService<ModuleLoader> implements ModuleLoaderProvider {

        private final InjectedValue<ServiceModuleLoader> injectedModuleLoader = new InjectedValue<ServiceModuleLoader>();

        private static void addService(final ServiceTarget target) {
            ModuleLoaderIntegration service = new ModuleLoaderIntegration();
            ServiceBuilder<?> builder = target.addService(ServiceNames.MODULE_LOADER_PROVIDER, service);
            builder.addDependency(JBOSS_SERVICE_MODULE_LOADER, ServiceModuleLoader.class, service.injectedModuleLoader);
            builder.setInitialMode(Mode.ON_DEMAND);
            builder.install();
        }

        private ModuleLoaderIntegration() {
        }

        @Override
        public ModuleLoader getValue() throws IllegalStateException {
            return injectedModuleLoader.getValue();
        }
    }

    private static final class FrameworkModuleIntegration implements FrameworkModuleProvider {

        private final InjectedValue<Module> injectedSystemModule = new InjectedValue<Module>();
        private final SubsystemState subsystemState;
        private Module frameworkModule;

        private static void addService(final ServiceTarget target, final SubsystemState subsystemState) {
            FrameworkModuleIntegration service = new FrameworkModuleIntegration(subsystemState);
            ServiceBuilder<?> builder = target.addService(ServiceNames.FRAMEWORK_MODULE_PROVIDER, service);
            builder.addDependency(ServiceNames.SYSTEM_MODULE_PROVIDER, Module.class, service.injectedSystemModule);
            builder.setInitialMode(Mode.ON_DEMAND);
            builder.install();
        }

        private FrameworkModuleIntegration(SubsystemState subsystemState) {
            this.subsystemState = subsystemState;
        }

        @Override
        public void start(StartContext context) throws StartException {
        }

        @Override
        public void stop(StopContext context) {
            frameworkModule = null;
        }

        @Override
        public FrameworkModuleProvider getValue() throws IllegalStateException {
            return this;
        }

        @Override
        public Module getFrameworkModule(Bundle systemBundle) {
            if (frameworkModule == null) {
                frameworkModule = createFrameworkModule(systemBundle);
            }
            return frameworkModule;
        }

        private Module createFrameworkModule(final Bundle systemBundle) {
            // Setup the extended framework module spec
            Module systemModule = injectedSystemModule.getValue();
            ModuleIdentifier systemIdentifier = systemModule.getIdentifier();
            ModuleLoader systemLoader = systemModule.getModuleLoader();
            ModuleSpec.Builder specBuilder = ModuleSpec.build(ModuleIdentifier.create(JBOSGI_PREFIX + ".framework"));
            specBuilder
                    .addDependency(DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(), PathFilters.acceptAll(), systemLoader, systemIdentifier, false));

            // Add a dependency on the default framework module
            ModuleLoader bootLoader = Module.getBootModuleLoader();
            ModuleIdentifier frameworkIdentifier = ModuleIdentifier.create("org.jboss.osgi.framework");
            DependencySpec moduleDep = DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(), PathFilters.acceptAll(), bootLoader, frameworkIdentifier,
                    false);
            specBuilder.addDependency(moduleDep);

            // Add the user defined module dependencies
            String modulesProps = (String) subsystemState.getProperties().get(PROP_JBOSS_OSGI_SYSTEM_MODULES);
            if (modulesProps != null) {
                for (String moduleProp : modulesProps.split(",")) {
                    ModuleIdentifier moduleId = ModuleIdentifier.create(moduleProp.trim());
                    moduleDep = DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(), PathFilters.acceptAll(), bootLoader, moduleId, false);
                    specBuilder.addDependency(moduleDep);
                }
            }

            specBuilder.setModuleClassLoaderFactory(new BundleReferenceClassLoader.Factory(systemBundle));

            try {
                final ModuleSpec moduleSpec = specBuilder.create();
                ModuleLoader moduleLoader = new ModuleLoader() {

                    @Override
                    protected ModuleSpec findModule(ModuleIdentifier identifier) throws ModuleLoadException {
                        return (moduleSpec.getModuleIdentifier().equals(identifier) ? moduleSpec : null);
                    }

                    @Override
                    public String toString() {
                        return getClass().getSimpleName();
                    }
                };
                return moduleLoader.loadModule(specBuilder.getIdentifier());
            } catch (ModuleLoadException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private static final class AutoInstallIntegration extends AbstractService<AutoInstallProvider> implements AutoInstallProvider {

        private InjectedValue<BundleManagement> injectedBundleManager = new InjectedValue<BundleManagement>();
        private InjectedValue<Bundle> injectedSystemBundle = new InjectedValue<Bundle>();
        private List<URL> autoInstall = new ArrayList<URL>();
        private List<URL> autoStart = new ArrayList<URL>();
        private SubsystemState subsystemState;

        private static void addService(final ServiceTarget target, final SubsystemState subsystemState) {
            AutoInstallIntegration service = new AutoInstallIntegration(subsystemState);
            ServiceBuilder<?> builder = target.addService(org.jboss.osgi.framework.ServiceNames.AUTOINSTALL_PROVIDER, service);
            builder.addDependency(ServiceNames.BUNDLE_MANAGER, BundleManagement.class, service.injectedBundleManager);
            builder.addDependency(ServiceNames.SYSTEM_BUNDLE, Bundle.class, service.injectedSystemBundle);
            builder.setInitialMode(Mode.ON_DEMAND);
            builder.install();
        }

        private AutoInstallIntegration(SubsystemState subsystemState) {
            this.subsystemState = subsystemState;
        }

        @Override
        public void start(StartContext context) throws StartException {
            try {
                // Create the list of {@link Deployment}s for the configured modules
                for (OSGiModule moduleMetaData : subsystemState.getModules()) {
                    ModuleIdentifier identifier = moduleMetaData.getIdentifier();
                    URL fileURL = getModuleLocation(identifier);
                    if (moduleMetaData.isStart())
                        autoStart.add(fileURL);
                    else
                        autoInstall.add(fileURL);
                }
            } catch (IOException ex) {
                throw new StartException("Failed to create auto install list", ex);
            }
        }

        @Override
        public AutoInstallProvider getValue() throws IllegalStateException {
            return this;
        }

        @Override
        public List<URL> getAutoInstallList(BundleContext context) {
            return Collections.unmodifiableList(autoInstall);
        }

        @Override
        public List<URL> getAutoStartList(BundleContext context) {
            return Collections.unmodifiableList(autoStart);
        }

        private URL getModuleLocation(final ModuleIdentifier identifier) throws IOException {

            String location = "module:" + identifier.getName();
            if ("main".equals(identifier.getSlot()) == false)
                location += ":" + identifier.getSlot();

            // Check if we have a single root file
            File repoFile = getModuleRepositoryEntry(identifier);
            if (repoFile == null)
                throw new IllegalArgumentException("Cannot obtain repository entry for: " + identifier);

            return repoFile.toURI().toURL();
        }

        /**
         * Get file for the singe jar that corresponds to the given identifier
         */
        private File getModuleRepositoryEntry(ModuleIdentifier identifier) {
            File rootPath = new File(System.getProperty("module.path"));
            String identifierPath = identifier.getName().replace('.', '/') + "/" + identifier.getSlot();
            File moduleDir = new File(rootPath + "/" + identifierPath);
            if (moduleDir.isDirectory() == false) {
                log.warnf("Cannot obtain module directory: %s", moduleDir);
                return null;
            }

            String[] files = moduleDir.list(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            });
            if (files.length == 0) {
                log.warnf("Cannot find module jar in: %s", moduleDir);
                return null;
            }
            if (files.length > 1) {
                log.warnf("Multiple module jars in: %s", moduleDir);
                return null;
            }

            File moduleFile = new File(moduleDir + "/" + files[0]);
            if (moduleFile.exists() == false) {
                log.warnf("Module file does not exist: %s", moduleFile);
                return null;
            }

            return moduleFile;
        }
    }
}
