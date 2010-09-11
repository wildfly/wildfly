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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.deployment.module.ClassifyingModuleLoaderInjector;
import org.jboss.as.deployment.module.ClassifyingModuleLoaderService;
import org.jboss.as.util.SystemPropertyActions;
import org.jboss.logging.Logger;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
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
import org.jboss.osgi.framework.plugin.ModuleManagerPlugin.ModuleSpecCreationHook;

/**
 * Service responsible for creating and managing the life-cycle of the OSGi {@link BundleManager}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
public class BundleManagerService implements Service<BundleManager> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("osgi.bundle.manager");
    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    private InjectedValue<Configuration> injectedConfig = new InjectedValue<Configuration>();
    private InjectedValue<ClassifyingModuleLoaderService> injectedModuleLoader = new InjectedValue<ClassifyingModuleLoaderService>();
    private Injector<ClassifyingModuleLoaderService> osgiModuleLoaderInjector;
    private BundleManager bundleManager;

    public static void addService(final BatchBuilder batchBuilder, final Configuration config) {
        BundleManagerService service = new BundleManagerService();
        BatchServiceBuilder<?> serviceBuilder = batchBuilder.addService(BundleManagerService.SERVICE_NAME, service);
        serviceBuilder.addDependency(ClassifyingModuleLoaderService.SERVICE_NAME, ClassifyingModuleLoaderService.class,
                service.injectedModuleLoader);
        serviceBuilder.addDependency(Configuration.SERVICE_NAME, Configuration.class, service.injectedConfig);
        serviceBuilder.setInitialMode(Mode.ON_DEMAND);
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
            ServiceContainer container = context.getController().getServiceContainer();
            ModuleLoader classifyingModuleLoader = injectedModuleLoader.getValue().getModuleLoader();
            final Map<String, Object> props = new HashMap<String, Object>(config.getProperties());
            props.put(IntegrationMode.class.getName(), IntegrationMode.CONTAINER);
            props.put(ModuleLoader.class.getName(), classifyingModuleLoader);
            props.put(ServiceContainer.class.getName(), container);

            // Get {@link ModuleLoader} for the OSGi layer
            bundleManager = new BundleManager(props);
            ModuleManagerPlugin plugin = bundleManager.getPlugin(ModuleManagerPlugin.class);
            ModuleLoader moduleLoader = plugin.getModuleLoader();

            // Register the {@link ModuleLoader} with the {@link ClassifyingModuleLoaderService}
            ServiceController<?> controller = container.getRequiredService(ClassifyingModuleLoaderService.SERVICE_NAME);
            ClassifyingModuleLoaderService moduleLoaderService = (ClassifyingModuleLoaderService) controller.getValue();
            Value<ModuleLoader> value = new ImmediateValue<ModuleLoader>(moduleLoader);
            osgiModuleLoaderInjector = new ClassifyingModuleLoaderInjector(Constants.JBOSGI_PREFIX, value);
            osgiModuleLoaderInjector.inject(moduleLoaderService);

            // Setup the list of dependencies for the core framework
            ModuleSpecCreationHook creationHook = new ModuleSpecCreationHook() {

                @Override
                public ModuleSpec create(ModuleSpec.Builder specBuilder) {
                    ModuleIdentifier identifier = specBuilder.getIdentifier();
                    if (identifier.getName().equals(Constants.JBOSGI_PREFIX + ".system.bundle")) {
                        List<ModuleIdentifier> systemModules = new ArrayList<ModuleIdentifier>();
                        systemModules.add(ModuleIdentifier.create("org.jboss.logging"));
                        systemModules.add(ModuleIdentifier.create("org.osgi.core"));
                        systemModules.add(ModuleIdentifier.create("org.osgi.compendium"));
                        systemModules.add(ModuleIdentifier.create("org.jboss.osgi.spi"));
                        systemModules.add(ModuleIdentifier.create("org.jboss.osgi.deployment"));
                        // User defined dependencies can be added by 'org.jboss.osgi.system.modules'
                        String modulesProps = (String) props.get(Configuration.PROP_JBOSS_OSGI_SYSTEM_MODULES);
                        if (modulesProps != null) {
                            for (String moduleProp : modulesProps.split(",")) {
                                ModuleIdentifier moduleId = ModuleIdentifier.create(moduleProp.trim());
                                systemModules.add(moduleId);
                            }
                        }
                        PathFilter all = PathFilters.acceptAll();
                        ModuleLoader moduleLoader = Module.getDefaultModuleLoader();
                        for (ModuleIdentifier moduleId : systemModules)
                            specBuilder.addDependency(DependencySpec.createModuleDependencySpec(all, all, moduleLoader,
                                    moduleId, false));
                    }
                    return specBuilder.create();
                }
            };
            plugin.setModuleSpecCreationHook(creationHook);

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
}
