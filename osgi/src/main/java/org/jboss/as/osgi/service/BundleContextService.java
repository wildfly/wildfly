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

import org.jboss.as.server.deployment.DeploymentService;
import org.jboss.as.osgi.deployment.InstallBundleInitiatorService;
import org.jboss.as.osgi.deployment.ModuleRegistrationService;
import org.jboss.as.osgi.deployment.OSGiDeploymentService;
import org.jboss.as.osgi.parser.OSGiSubsystemState.Activation;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.bundle.AbstractUserBundle;
import org.jboss.osgi.framework.bundle.BundleManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.launch.Framework;

/**
 * Service responsible for creating and managing the life-cycle of the OSGi system {@link BundleContext}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 29-Oct-2010
 */
public class BundleContextService implements Service<BundleContext> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("osgi", "context");

    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    private final InjectedValue<BundleManager> injectedBundleManager = new InjectedValue<BundleManager>();
    private final InjectedValue<Framework> injectedFramework = new InjectedValue<Framework>();
    private BundleContext sysContext;

    public static void addService(final ServiceTarget target, Activation policy) {
        BundleContextService service = new BundleContextService();
        ServiceBuilder<?> serviceBuilder = target.addService(BundleContextService.SERVICE_NAME, service);
        serviceBuilder.addDependency(BundleManagerService.SERVICE_NAME, BundleManager.class, service.injectedBundleManager);
        serviceBuilder.addDependency(FrameworkService.SERVICE_NAME, Framework.class, service.injectedFramework);
        serviceBuilder.setInitialMode(policy == Activation.LAZY ? Mode.ON_DEMAND : Mode.ACTIVE);
        serviceBuilder.install();
    }

    public static BundleContext getServiceValue(ServiceContainer container) {
        try {
            ServiceController<?> controller = container.getRequiredService(SERVICE_NAME);
            return (BundleContext) controller.getValue();
        } catch (ServiceNotFoundException ex) {
            throw new IllegalStateException("Cannot obtain required service: " + SERVICE_NAME);
        }
    }

    public synchronized void start(final StartContext context) throws StartException {
        sysContext = injectedFramework.getValue().getBundleContext();

        // Register a {@link BundleListener} that installs a {@link ServiceListener}
        // with every Non-OSGi {@link DeploymentService}
        BundleListener uninstallListener = new BundleListener() {

            @Override
            public void bundleChanged(BundleEvent event) {
                if (event.getType() == BundleEvent.INSTALLED) {

                    AbstractUserBundle userBundle;
                    try {
                        userBundle = AbstractUserBundle.assertBundleState(event.getBundle());
                    } catch (RuntimeException ex) {
                        // ignore
                        return;
                    }

                    Deployment dep = userBundle.getDeployment();
                    String contextName = InstallBundleInitiatorService.getContextName(dep);

                    // Check if we have an {@link OSGiDeploymentService}
                    ServiceContainer container = context.getController().getServiceContainer();
                    ServiceName osgiDeploymentService = OSGiDeploymentService.getServiceName(contextName);
                    ServiceName deploymentService = DeploymentService.getServiceName(contextName);
                    if (container.getService(deploymentService) != null && container.getService(osgiDeploymentService) == null) {
                        ServiceName serviceName = ModuleRegistrationService.getServiceName(contextName);
                        try {
                            log.tracef("Register service: %s", serviceName);
                            BatchBuilder batchBuilder = container.batchBuilder();
                            ModuleRegistrationService.addService(batchBuilder, dep, contextName);
                            batchBuilder.install();
                        } catch (ServiceRegistryException ex) {
                            throw new IllegalStateException("Cannot register service: " + serviceName, ex);
                        }
                    }
                }
            }
        };
        sysContext.addBundleListener(uninstallListener);
    }

    public synchronized void stop(StopContext context) {
        sysContext = null;
    }

    @Override
    public BundleContext getValue() throws IllegalStateException {
        return sysContext;
    }
}
