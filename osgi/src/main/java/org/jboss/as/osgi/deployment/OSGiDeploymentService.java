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

package org.jboss.as.osgi.deployment;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jboss.as.deployment.DeploymentService;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.osgi.service.FrameworkService;
import org.jboss.as.osgi.service.PackageAdminService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.DeployerService;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Service responsible for creating and managing the life-cycle of an OSGi deployment.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Sep-2010
 */
public class OSGiDeploymentService implements Service<Deployment> {

    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");
    private static final OSGiDeploymentListener listener = new OSGiDeploymentListener();

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("osgi", "deployment");
    public static boolean enableListener = true;

    private final Deployment deployment;
    private InjectedValue<BundleContext> injectedContext = new InjectedValue<BundleContext>();

    private OSGiDeploymentService(Deployment deployment) {
        this.deployment = deployment;
    }

    public static void addService(DeploymentUnitContext context) {

        // Attach the {@link DeploymentService} name so we remove that service on Bundle.uninstall()
        ServiceName deploymentServiceName = DeploymentService.getServiceName(context.getName());
        Deployment deployment = DeploymentAttachment.getDeploymentAttachment(context);
        deployment.addAttachment(ServiceName.class, deploymentServiceName);

        BatchBuilder batchBuilder = context.getBatchBuilder();
        OSGiDeploymentService service = new OSGiDeploymentService(deployment);
        ServiceName serviceName = OSGiDeploymentService.SERVICE_NAME.append(deploymentServiceName.getSimpleName());
        ServiceBuilder<Deployment> serviceBuilder = batchBuilder.addService(serviceName, service);
        serviceBuilder.addDependency(FrameworkService.SERVICE_NAME, BundleContext.class, service.injectedContext);
        serviceBuilder.addDependency(PackageAdminService.SERVICE_NAME);
        serviceBuilder.addDependency(deploymentServiceName);
        serviceBuilder.setInitialMode(Mode.ACTIVE);
        if (enableListener)
            serviceBuilder.addListener(listener);
    }

    /**
     * Install the Bundle associated with this deployment.
     *
     * @param context The start context
     */
    public synchronized void start(StartContext context) throws StartException {

        // Get the OSGi system context
        ServiceController<?> controller = context.getController();
        ServiceContainer serviceContainer = controller.getServiceContainer();

        // Make sure the Framework does not shut down when the last bundle gets removed
        ServiceController<?> frameworkController = serviceContainer.getService(FrameworkService.SERVICE_NAME);
        frameworkController.setMode(Mode.ACTIVE);

        log.tracef("Installing deployment: %s", deployment);
        try {
            boolean autoStart = deployment.isAutoStart();
            deployment.setAutoStart(false);
            Bundle bundle = getDeployerService().deploy(deployment);
            deployment.addAttachment(Bundle.class, bundle);
            deployment.setAutoStart(autoStart);
        } catch (Throwable t) {
            throw new StartException("Failed to install deployment: " + deployment, t);
        }
    }

    /**
     * Uninstall the Bundle associated with this deployment.
     *
     * @param context The stop context.
     */
    public synchronized void stop(StopContext context) {
        log.tracef("Uninstalling deployment: %s", deployment);
        try {
            getDeployerService().undeploy(deployment);
        } catch (Throwable t) {
            log.errorf(t, "Failed to uninstall deployment: %s", deployment);
        }
    }

    @Override
    public Deployment getValue() throws IllegalStateException {
        return deployment;
    }

    // Get the OSGi {@link DeployerService}
    private DeployerService getDeployerService() {
        BundleContext sysContext = injectedContext.getValue();
        ServiceReference sref = sysContext.getServiceReference(DeployerService.class.getName());
        return (DeployerService) sysContext.getService(sref);
    }

    static class OSGiDeploymentListener extends AbstractServiceListener<Deployment> {

        private final Set<Deployment> startedDeployments = new CopyOnWriteArraySet<Deployment>();
        private final Set<Deployment> pendingDeployments = new CopyOnWriteArraySet<Deployment>();

        @Override
        public void listenerAdded(ServiceController<? extends Deployment> controller) {
            pendingDeployments.add(controller.getValue());
        }

        @Override
        public void serviceStarted(ServiceController<? extends Deployment> controller) {
            startedDeployments.add(controller.getValue());
            processDeployment(controller);
        }

        @Override
        public void serviceFailed(ServiceController<? extends Deployment> controller, StartException reason) {
            processDeployment(controller);
        }

        private void processDeployment(ServiceController<? extends Deployment> controller) {
            controller.removeListener(this);
            Set<Deployment> bundlesToStart = null;
            synchronized (this) {
                pendingDeployments.remove(controller.getValue());
                if (pendingDeployments.isEmpty()) {
                    bundlesToStart = new HashSet<Deployment>(startedDeployments);
                    startedDeployments.clear();
                }
            }

            if (bundlesToStart != null) {
                ServiceContainer serviceContainer = controller.getServiceContainer();
                PackageAdmin packageAdmin = PackageAdminService.getServiceValue(serviceContainer);
                for (Deployment deployment : bundlesToStart) {
                    Bundle bundle = deployment.getAttachment(Bundle.class);
                    if (packageAdmin.getBundleType(bundle) != PackageAdmin.BUNDLE_TYPE_FRAGMENT) {
                        log.tracef("Starting bundle: %s", bundle);
                        try {
                            bundle.start();
                        } catch (BundleException ex) {
                            log.errorf(ex, "Cannot start bundle: %s", bundle);
                        }
                    }
                }
            }
        }
    }
}
