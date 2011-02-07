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
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.jmx.ObjectNameFactory;
import org.jboss.as.osgi.deployment.DeployerServicePluginIntegration;
import org.jboss.as.osgi.parser.SubsystemState;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.server.Services;
import org.jboss.as.server.client.api.deployment.ServerDeploymentManager;
import org.jboss.as.server.deployment.module.DeploymentModuleLoader;
import org.jboss.as.server.services.net.SocketBinding;
import org.jboss.logging.Logger;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.filter.PathFilters;
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
import org.jboss.osgi.framework.BundleManagerMBean;
import org.jboss.osgi.framework.Constants;
import org.jboss.osgi.framework.bundle.BundleManager;
import org.jboss.osgi.framework.bundle.BundleManager.IntegrationMode;
import org.jboss.osgi.framework.bundle.OSGiModuleLoader;
import org.jboss.osgi.framework.bundle.SystemBundle;
import org.jboss.osgi.framework.plugin.DeployerServicePlugin;
import org.jboss.osgi.framework.plugin.SystemModuleProviderPlugin;
import org.jboss.osgi.framework.plugin.internal.AbstractSystemModuleProviderPlugin;
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
    private final InjectedValue<DeploymentModuleLoader> injectedDeploymentModuleLoader = new InjectedValue<DeploymentModuleLoader>();
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
        serviceBuilder.addDependency(Services.JBOSS_DEPLOYMENT_MODULE_LOADER, DeploymentModuleLoader.class, service.injectedDeploymentModuleLoader);
        serviceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append("osgi-http"), SocketBinding.class, service.osgiHttpServerPortBinding);
        serviceBuilder.addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, service.injectedMBeanServer);
        serviceBuilder.setInitialMode(Mode.ON_DEMAND);
        serviceBuilder.install();
    }

    public synchronized void start(StartContext context) throws StartException {
        log.debugf("Starting OSGi BundleManager");
        try {
            // [JBVFS-164] Add a URLStreamHandlerFactory service
            String handlerModules = SecurityActions.getSystemProperty("jboss.protocol.handler.modules");
            if (handlerModules == null)
                SecurityActions.setSystemProperty("jboss.protocol.handler.modules", "org.jboss.osgi.framework");

            // Setup the OSGi {@link Framework} properties
            Map<String, Object> props = new HashMap<String, Object>(subsystemState.getProperties());
            setupIntegrationProperties(context, props);

            // Get {@link ModuleLoader} for the OSGi layer
            bundleManager = new BundleManager(props);

            // Setup the Framework module provider
            bundleManager.addPlugin(SystemModuleProviderPlugin.class, new FrameworkModuleProvider(bundleManager));

            // Setup the {@link DeployerServicePlugin}
            ServerDeploymentManager deploymentManager = injectedDeploymentManager.getValue();
            bundleManager.addPlugin(DeployerServicePlugin.class, new DeployerServicePluginIntegration(bundleManager, deploymentManager));

            // Register the {@link BundleManagerMBean}
            BundleManagerMBean bundleManagerMBean = new BundleManagerMBean() {
                @Override
                public long installBundle(ModuleIdentifier identifier) throws BundleException, ModuleLoadException {
                    Bundle bundle;
                    if (identifier.getName().startsWith("deployment.")) {
                        ModuleLoader moduleLoader = injectedDeploymentModuleLoader.getValue();
                        Module module = moduleLoader.loadModule(identifier);
                        bundle = bundleManager.installBundle(module);
                    } else {
                        bundle = bundleManager.installBundle(identifier);
                    }
                    return bundle.getBundleId();
                }
            };
            StandardMBean mbean = new StandardMBean(bundleManagerMBean, BundleManagerMBean.class);
            ObjectName oname = ObjectNameFactory.create(BundleManagerMBean.OBJECT_NAME);
            injectedMBeanServer.getValue().registerMBean(mbean, oname);
        } catch (Throwable t) {
            throw new StartException("Failed to create BundleManager", t);
        }
    }

    private void setupIntegrationProperties(StartContext context, Map<String, Object> props) {

        // Set the Framework's {@link IntegrationMode}
        props.put(IntegrationMode.class.getName(), IntegrationMode.CONTAINER);

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

    final class FrameworkModuleProvider extends AbstractSystemModuleProviderPlugin {

        private Module frameworkModule;

        public FrameworkModuleProvider(BundleManager bundleManager) {
            super(bundleManager);
        }

        @Override
        public void destroyPlugin() {
            super.destroyPlugin();
            frameworkModule = null;
        }

        @Override
        public Module getFrameworkModule() {
            return frameworkModule;
        }

        @Override
        public Module createFrameworkModule(OSGiModuleLoader osgiLoader, SystemBundle systemBundle) throws ModuleLoadException {

            // Setup the extended framework module spec
            ModuleLoader systemLoader = Module.getSystemModuleLoader();
            ModuleIdentifier systemIdentifier = getSystemModule().getIdentifier();
            ModuleSpec.Builder specBuilder = ModuleSpec.build(ModuleIdentifier.create(Constants.JBOSGI_PREFIX + ".framework"));
            specBuilder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(), PathFilters.acceptAll(), osgiLoader, systemIdentifier, false));

            // Add a dependency on the default framework module
            ModuleIdentifier frameworkIdentifier = ModuleIdentifier.create("org.jboss.osgi.framework");
            DependencySpec moduleDep = DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(), PathFilters.acceptAll(), systemLoader, frameworkIdentifier, false);
            specBuilder.addDependency(moduleDep);

            // Add the user defined module dependencies
            String modulesProps = (String) bundleManager.getProperty(SubsystemState.PROP_JBOSS_OSGI_SYSTEM_MODULES);
            if (modulesProps != null) {
                for (String moduleProp : modulesProps.split(",")) {
                    ModuleIdentifier moduleId = ModuleIdentifier.create(moduleProp.trim());
                    moduleDep = DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(), PathFilters.acceptAll(), systemLoader, moduleId, false);
                    specBuilder.addDependency(moduleDep);
                }
            }

            ModuleSpec moduleSpec = specBuilder.create();
            osgiLoader.addModule(systemBundle.getCurrentRevision(), moduleSpec);
            try {
                frameworkModule = osgiLoader.loadModule(specBuilder.getIdentifier());
                return frameworkModule;
            } catch (ModuleLoadException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
