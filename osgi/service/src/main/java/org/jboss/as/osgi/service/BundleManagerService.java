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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.StandardMBean;

import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.osgi.deployment.DeployerServicePluginIntegration;
import org.jboss.as.osgi.parser.SubsystemState;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.server.client.api.deployment.ServerDeploymentManager;
import org.jboss.as.server.services.net.SocketBinding;
import org.jboss.logging.Logger;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.PathFilter;
import org.jboss.modules.PathFilters;
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
import org.jboss.osgi.framework.Constants;
import org.jboss.osgi.framework.bundle.BundleManager;
import org.jboss.osgi.framework.bundle.BundleManager.IntegrationMode;
import org.jboss.osgi.framework.plugin.DeployerServicePlugin;
import org.jboss.osgi.framework.plugin.SystemPackagesPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

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

    private final InjectedValue<MBeanServer> injectedMBeanServer = new InjectedValue<MBeanServer>();
    private final InjectedValue<ServerEnvironment> injectedEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<ServerDeploymentManager> injectedDeploymentManager = new InjectedValue<ServerDeploymentManager>();
    private final InjectedValue<SocketBinding> osgiHttpServerPortBinding = new InjectedValue<SocketBinding>();
    private final SubsystemState subsystemState;

    private BundleManager bundleManager;

    private BundleManagerService(SubsystemState subsystemState) {
        this.subsystemState = subsystemState;
    }

    public static void addService(final ServiceTarget target, final SubsystemState subsystemState) {
        BundleManagerService service = new BundleManagerService(subsystemState);
        ServiceBuilder<?> serviceBuilder = target.addService(BundleManagerService.SERVICE_NAME, service);
        serviceBuilder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedEnvironment);
        serviceBuilder.addDependency(ServerDeploymentManager.SERVICE_NAME_LOCAL, ServerDeploymentManager.class, service.injectedDeploymentManager);
        serviceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append("osgi-http"), SocketBinding.class, service.osgiHttpServerPortBinding);
        serviceBuilder.addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, service.injectedMBeanServer);
        serviceBuilder.setInitialMode(Mode.ON_DEMAND);
        serviceBuilder.install();
    }

    public synchronized void start(StartContext context) throws StartException {
        log.debugf("Starting OSGi BundleManager");
        try {
            // [JBVFS-164] Add a URLStreamHandlerFactory service
            // TODO - use an actually secure system prop get action
            String handlerModules = System.getProperty("jboss.protocol.handler.modules");
            if (handlerModules == null)
                System.setProperty("jboss.protocol.handler.modules", "org.jboss.osgi.framework");

            // Setup the OSGi {@link Framework} properties
            Map<String, Object> props = new HashMap<String, Object>(subsystemState.getProperties());
            setupIntegrationProperties(context, props);

            // Get {@link ModuleLoader} for the OSGi layer
            bundleManager = new BundleManager(props);

            // Setup the Framework {@link Module}
            Module frameworkModule = new FrameworkModuleLoader(bundleManager).getFrameworkModule();
            bundleManager.setProperty(Module.class.getName(), frameworkModule);

            // Setup the {@link DeployerServicePlugin}
            ServerDeploymentManager deploymentManager = injectedDeploymentManager.getValue();
            bundleManager.addPlugin(DeployerServicePlugin.class, new DeployerServicePluginIntegration(bundleManager, deploymentManager));

            // Register the {@link BundleManagerMBean}
            BundleManagerMBean bundleManagerMBean = new BundleManagerMBean() {
                @Override
                public long installBundle(ModuleIdentifier identifier) throws BundleException {
                    Bundle bundle = bundleManager.installBundle(identifier);
                    return bundle.getBundleId();
                }
                @Override
                public long installBundle(String location, ModuleIdentifier identifier) throws BundleException {
                    Bundle bundle = bundleManager.installBundle(location, identifier);
                    return bundle.getBundleId();
                }
            };
            StandardMBean mbean = new StandardMBean(bundleManagerMBean, BundleManagerMBean.class);
            injectedMBeanServer.getValue().registerMBean(mbean, BundleManagerMBean.OBJECTNAME);
        } catch (Throwable t) {
            throw new StartException("Failed to create BundleManager", t);
        }
    }

    private void setupIntegrationProperties(StartContext context, Map<String, Object> props) {

        // Set the Framework's {@link IntegrationMode}
        props.put(IntegrationMode.class.getName(), IntegrationMode.CONTAINER);

        // Setup the default {@link ModuleLoader}
        //ModuleLoader moduleLoader = injectedModuleLoader.getValue().getModuleLoader();
        //props.put(ModuleLoader.class.getName(), moduleLoader);

        // Setup the {@link ServiceContainer}
        ServiceContainer container = context.getController().getServiceContainer();
        props.put(ServiceContainer.class.getName(), container);

        // Configure the OSGi HttpService port
        // [TODO] This will go away once the HTTP subsystem from AS implements the OSGi HttpService.
        props.put("org.osgi.service.http.port", "" + osgiHttpServerPortBinding.getValue().getSocketAddress().getPort());

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

    public synchronized void stop(StopContext context) {
        log.debugf("Stopping OSGi BundleManager");
        try {
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
     * User defined dependencies can be added by property 'org.jboss.osgi.system.modules' in the configuration
     *
     * In case there are no user defined system modules, this loader simply returns the default 'org.jboss.osgi.framework'
     * module
     */
    static class FrameworkModuleLoader extends ModuleLoader {

        private final ModuleSpec moduleSpec;

        FrameworkModuleLoader(BundleManager bundleManager) throws ModuleLoadException {

            ModuleLoader moduleLoader = Module.getSystemModuleLoader();
            Module frameworkModule = moduleLoader.loadModule(ModuleIdentifier.create("org.jboss.osgi.framework"));

            // Setup the extended framework module spec
            ModuleIdentifier frameworkIdentifier = ModuleIdentifier.create("org.jboss.osgi.framework.extended");
            ModuleSpec.Builder builder = ModuleSpec.build(frameworkIdentifier);
            PathFilter all = PathFilters.acceptAll();

            // Add a dependency on the default framework module
            ModuleIdentifier moduleId = frameworkModule.getIdentifier();
            DependencySpec moduleDep = DependencySpec.createModuleDependencySpec(all, all, moduleLoader, moduleId, false);
            builder.addDependency(moduleDep);

            // Add the user defined module dependencies
            String modulesProps = (String) bundleManager.getProperty(SubsystemState.PROP_JBOSS_OSGI_SYSTEM_MODULES);
            if (modulesProps != null) {
                for (String moduleProp : modulesProps.split(",")) {
                    moduleId = ModuleIdentifier.create(moduleProp.trim());
                    moduleDep = DependencySpec.createModuleDependencySpec(all, all, moduleLoader, moduleId, false);
                    builder.addDependency(moduleDep);
                }
            }

            // Add a dependency on the system module
            PathFilter exp = PathFilters.in(bundleManager.getPlugin(SystemPackagesPlugin.class).getExportedPaths());
            moduleDep = DependencySpec.createModuleDependencySpec(all, exp, moduleLoader, ModuleIdentifier.SYSTEM, false);
            builder.addDependency(moduleDep);

            moduleSpec = builder.create();
        }

        Module getFrameworkModule() throws ModuleLoadException {
            return loadModule(moduleSpec.getModuleIdentifier());
        }

        @Override
        protected ModuleSpec findModule(ModuleIdentifier identifier) throws ModuleLoadException {
            return moduleSpec;
        }

        @Override
        public String toString() {
            return "ExtendedFrameworkModuleLoader";
        }
    }
}
