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

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.deployment.module.ClassifyingModuleLoaderInjector;
import org.jboss.as.deployment.module.ClassifyingModuleLoaderService;
import org.jboss.as.services.net.SocketBinding;
import org.jboss.as.util.SystemPropertyActions;
import org.jboss.logging.Logger;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.PathFilter;
import org.jboss.modules.PathFilters;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jboss.osgi.framework.Constants;
import org.jboss.osgi.framework.bundle.BundleManager;
import org.jboss.osgi.framework.bundle.BundleManager.IntegrationMode;
import org.jboss.osgi.framework.plugin.ModuleManagerPlugin;

/**
 * Service responsible for creating and managing the life-cycle of the OSGi {@link BundleManager}.
 *
 * @author Thomas.Diesler@jboss.com
 * @author <a href="david@redhat.com">David Bosschaert</a>
 * @since 11-Sep-2010
 */
public class BundleManagerService implements Service<BundleManager> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("osgi", "bundlemanager");
    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    private InjectedValue<Configuration> injectedConfig = new InjectedValue<Configuration>();
    private InjectedValue<ClassifyingModuleLoaderService> injectedModuleLoader = new InjectedValue<ClassifyingModuleLoaderService>();
    private InjectedValue<SocketBinding> osgiHttpServerPortBinding = new InjectedValue<SocketBinding>();
    private Injector<ClassifyingModuleLoaderService> osgiModuleLoaderInjector;
    private BundleManager bundleManager;

    public static void addService(final BatchBuilder batchBuilder, Mode initialMode) {
        BundleManagerService service = new BundleManagerService();
        BatchServiceBuilder<?> serviceBuilder = batchBuilder.addService(BundleManagerService.SERVICE_NAME, service);
        serviceBuilder.addDependency(ClassifyingModuleLoaderService.SERVICE_NAME, ClassifyingModuleLoaderService.class,
                service.injectedModuleLoader);
        serviceBuilder.addDependency(Configuration.SERVICE_NAME, Configuration.class, service.injectedConfig);
        serviceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append("osgi-http"), SocketBinding.class,
                service.osgiHttpServerPortBinding);
        serviceBuilder.setInitialMode(initialMode);
    }

    public synchronized void start(StartContext context) throws StartException {
        log.debugf("Starting OSGi BundleManager");
        try {
            // [JBVFS-164] Add a URLStreamHandlerFactory service
            String handlerModules = SystemPropertyActions.getProperty("jboss.protocol.handler.modules");
            if (handlerModules == null)
                System.setProperty("jboss.protocol.handler.modules", "org.jboss.osgi.framework");

            // Setup the OSGi {@link Framework} properties
            Configuration config = injectedConfig.getValue();
            Map<String, Object> props = new HashMap<String, Object>(config.getProperties());
            ServiceContainer container = context.getController().getServiceContainer();
            ModuleLoader moduleLoader = injectedModuleLoader.getValue().getModuleLoader();
            Module frameworkModule = new FrameworkModuleLoader(moduleLoader, props).getFrameworkModule();
            props.put(IntegrationMode.class.getName(), IntegrationMode.CONTAINER);
            props.put(ModuleLoader.class.getName(), moduleLoader);
            props.put(ServiceContainer.class.getName(), container);
            props.put(Module.class.getName(), frameworkModule);

            // [TODO] This is a temporary workaround to configure the HTTP subsystem in OSGi
            // this will go away once the HTTP subsystem from AS implements the OSGi HTTP Service.
            props.put("org.osgi.service.http.port", "" + osgiHttpServerPortBinding.getValue().getSocketAddress().getPort());

            // Always clean the framework storage
            // [TODO] Differentiate beetween user data and persisted bundles
            props.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);

            // Get {@link ModuleLoader} for the OSGi layer
            bundleManager = new BundleManager(props);
            ModuleManagerPlugin plugin = bundleManager.getPlugin(ModuleManagerPlugin.class);
            ModuleLoader osgiModuleLoader = plugin.getModuleLoader();

            // Register the {@link ModuleLoader} with the {@link ClassifyingModuleLoaderService}
            ServiceController<?> controller = container.getRequiredService(ClassifyingModuleLoaderService.SERVICE_NAME);
            ClassifyingModuleLoaderService moduleLoaderService = (ClassifyingModuleLoaderService) controller.getValue();
            Value<ModuleLoader> value = new ImmediateValue<ModuleLoader>(osgiModuleLoader);
            osgiModuleLoaderInjector = new ClassifyingModuleLoaderInjector(Constants.JBOSGI_PREFIX, value);
            osgiModuleLoaderInjector.inject(moduleLoaderService);
        } catch (Throwable t) {
            throw new StartException("Failed to create BundleManager", t);
        }
    }

    public synchronized void stop(StopContext context) {
        log.debugf("Stopping OSGi BundleManager");
        try {
            if (osgiModuleLoaderInjector != null)
                osgiModuleLoaderInjector.uninject();

            bundleManager = null;

        } catch (Exception ex) {
            log.errorf(ex, "Cannot stop OSGi BundleManager");
        }
    }

    @Override
    public BundleManager getValue() throws IllegalStateException {
        return bundleManager;
    }

    /**
     * Provides the Framework module with its dependencies
     *
     * User defined dependencies can be added by property
     * 'org.jboss.osgi.system.modules' in the configuration
     *
     * In case there are no user defined system modules, this loader
     * simply returns the default 'org.jboss.osgi.framework' module
     */
    static class FrameworkModuleLoader extends ModuleLoader {

        private final ModuleSpec moduleSpec;
        private final ModuleIdentifier frameworkIdentifier;
        private Module defaultFrameworkModule;

        FrameworkModuleLoader(ModuleLoader moduleLoader, Map<String, Object> props) throws ModuleLoadException {

            defaultFrameworkModule = moduleLoader.loadModule(ModuleIdentifier.create("org.jboss.osgi.framework"));
            String modulesProps = (String) props.get(Configuration.PROP_JBOSS_OSGI_SYSTEM_MODULES);
            if (modulesProps == null) {
                frameworkIdentifier = defaultFrameworkModule.getIdentifier();
                moduleSpec = null;
                return;
            }

            // Setup the extended framework moduel spec
            frameworkIdentifier = ModuleIdentifier.create("org.jboss.osgi.framework.extended");
            ModuleSpec.Builder builder = ModuleSpec.build(frameworkIdentifier);
            PathFilter all = PathFilters.acceptAll();

            // Add a dependency on the default framework module
            ModuleIdentifier moduleId = defaultFrameworkModule.getIdentifier();
            DependencySpec moduleDep = DependencySpec.createModuleDependencySpec(all, all, moduleLoader, moduleId, false);
            builder.addDependency(moduleDep);

            // Add the user defined module dependencies
            for (String moduleProp : modulesProps.split(",")) {
                moduleId = ModuleIdentifier.create(moduleProp.trim());
                moduleDep = DependencySpec.createModuleDependencySpec(all, all, moduleLoader, moduleId, false);
                builder.addDependency(moduleDep);
            }

            moduleSpec = builder.create();
        }

        Module getFrameworkModule() throws ModuleLoadException {
            return (moduleSpec != null ? loadModule(moduleSpec.getModuleIdentifier()) : defaultFrameworkModule);
        }

        @Override
        protected ModuleSpec findModule(ModuleIdentifier identifier) throws ModuleLoadException {
            if (moduleSpec == null || identifier.equals(frameworkIdentifier) == false)
                throw new IllegalStateException("Cannot provide ModuleSpec for: " + identifier);
            return moduleSpec;
        }

        @Override
        public String toString() {
            return "ExtendedFrameworkModuleLoader";
        }
    }
}
