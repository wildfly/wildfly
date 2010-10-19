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
import java.util.List;

import javax.management.MBeanServer;

import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.osgi.parser.OSGiSubsystemState.OSGiModule;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.DeployerService;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.bundle.AbstractUserBundle;
import org.jboss.osgi.framework.bundle.BundleManager;
import org.jboss.osgi.framework.plugin.BundleDeploymentPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.launch.Framework;

/**
 * Service responsible for creating and managing the life-cycle of the OSGi Framework.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
public class FrameworkService implements Service<BundleContext> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("osgi.system.context");
    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    private InjectedValue<BundleManager> injectedBundleManager = new InjectedValue<BundleManager>();
    private InjectedValue<MBeanServer> injectedMBeanServer = new InjectedValue<MBeanServer>();
    private InjectedValue<Configuration> injectedConfig = new InjectedValue<Configuration>();
    private Framework framework;

    public static void addService(final BatchBuilder batchBuilder) {
        FrameworkService service = new FrameworkService();
        BatchServiceBuilder<?> serviceBuilder = batchBuilder.addService(FrameworkService.SERVICE_NAME, service);
        serviceBuilder.addDependency(BundleManagerService.SERVICE_NAME, BundleManager.class, service.injectedBundleManager);
        serviceBuilder.addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, service.injectedMBeanServer);
        serviceBuilder.addDependency(Configuration.SERVICE_NAME, Configuration.class, service.injectedConfig);
        serviceBuilder.setInitialMode(Mode.ON_DEMAND);
    }

    public static BundleContext getServiceValue(ServiceContainer container) {
        try {
            ServiceController<?> controller = container.getRequiredService(SERVICE_NAME);
            return (BundleContext) controller.getValue();
        } catch (ServiceNotFoundException ex) {
            throw new IllegalStateException("Cannot obtain required service: " + SERVICE_NAME);
        }
    }

    public synchronized void start(StartContext context) throws StartException {
        log.infof("Starting OSGi Framework");
        try {
            // Start the OSGi {@link Framework}
            final ServiceContainer serviceContainer = context.getController().getServiceContainer();
            BundleManager bundleManager = injectedBundleManager.getValue();
            framework = bundleManager.getFrameworkState();
            framework.start();

            // Register the {@link MBeanServer} as OSGi service
            BundleContext sysContext = framework.getBundleContext();
            MBeanServer mbeanServer = injectedMBeanServer.getValue();
            sysContext.registerService(MBeanServer.class.getName(), mbeanServer, null);

            // Register a {@link SynchronousBundleListener} that removes the {@link DeploymentService}
            BundleListener uninstallListener = new SynchronousBundleListener() {

                @Override
                public void bundleChanged(BundleEvent event) {
                    if (event.getType() == BundleEvent.UNINSTALLED) {
                        AbstractUserBundle userBundle;
                        try {
                            userBundle = AbstractUserBundle.assertBundleState(event.getBundle());
                        } catch (RuntimeException ex) {
                            // ignore
                            return;
                        }
                        Deployment deployment = userBundle.getDeployment();
                        ServiceName serviceName = deployment.getAttachment(ServiceName.class);
                        if (serviceName != null) {
                            ServiceController<?> controller = serviceContainer.getService(serviceName);
                            if (controller != null) {
                                controller.setMode(ServiceController.Mode.REMOVE);
                            }
                        }
                    }
                }
            };
            sysContext.addBundleListener(uninstallListener);

            // Create the list of {@link Deployment}s for the configured modules
            List<Deployment> deployments = new ArrayList<Deployment>();
            BundleDeploymentPlugin depPlugin = bundleManager.getPlugin(BundleDeploymentPlugin.class);
            for (OSGiModule module : injectedConfig.getValue().getModules()) {
                ModuleIdentifier identifier = module.getIdentifier();
                Deployment dep = depPlugin.createDeployment(identifier);
                dep.setAutoStart(module.isStart());
                deployments.add(dep);
            }

            // Deploy the bundles through the {@link DeployerService}
            ServiceReference sref = sysContext.getServiceReference(DeployerService.class.getName());
            DeployerService service = (DeployerService) sysContext.getService(sref);
            service.deploy(deployments.toArray(new Deployment[deployments.size()]));

        } catch (Throwable t) {
            throw new StartException("Failed to start OSGi Framework: " + framework, t);
        }
    }

    public synchronized void stop(StopContext context) {
        log.infof("Stopping OSGi Framework");
        if (framework != null) {
            try {
                framework.stop();
                framework.waitForStop(2000);
            } catch (Exception ex) {
                log.errorf(ex, "Cannot stop OSGi Framework");
            }
        }
    }

    @Override
    public BundleContext getValue() throws IllegalStateException {
        if (framework == null || framework.getState() != Bundle.ACTIVE)
            throw new IllegalStateException("Cannot get BundleContext for: " + framework);
        return framework.getBundleContext();
    }
}
