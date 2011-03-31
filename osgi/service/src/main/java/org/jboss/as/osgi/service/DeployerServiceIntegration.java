/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.as.osgi.deployment.BundleInstallService;
import org.jboss.as.osgi.deployment.DeploymentHolderService;
import org.jboss.as.server.ServerController;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.client.ModelControllerServerDeploymentManager;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.DeployerService;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.SystemDeployerService;
import org.jboss.osgi.framework.DeployerServiceProvider;
import org.jboss.osgi.framework.FrameworkIntegration;
import org.jboss.osgi.framework.SystemBundleProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * The {@link DeployerService} that delegates to the {@link ServerDeploymentManager}.
 *
 * @author thomas.diesler@jboss.com
 * @since 24-Nov-2010
 */
public class DeployerServiceIntegration implements DeployerServiceProvider {

    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    private final InjectedValue<ServerController> injectedServerController = new InjectedValue<ServerController>();
    private final InjectedValue<FrameworkIntegration> injectedBundleManager = new InjectedValue<FrameworkIntegration>();
    private final InjectedValue<Bundle> injectedSystemBundle = new InjectedValue<Bundle>();
    private ServerDeploymentManager deploymentManager;
    private DeployerService delegate;

    public static void addService(final ServiceTarget target) {
        DeployerServiceIntegration service = new DeployerServiceIntegration();
        ServiceBuilder<?> builder = target.addService(SERVICE_NAME, service);
        builder.addDependency(Services.JBOSS_SERVER_CONTROLLER, ServerController.class, service.injectedServerController);
        builder.addDependency(SystemBundleProvider.SERVICE_NAME, Bundle.class, service.injectedSystemBundle);
        builder.addDependency(FrameworkIntegration.SERVICE_NAME, FrameworkIntegration.class, service.injectedBundleManager);
        builder.setInitialMode(Mode.PASSIVE);
        builder.install();
    }

    private DeployerServiceIntegration() {
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final ServiceContainer serviceContainer = context.getController().getServiceContainer();
        final BundleContext systemContext = injectedSystemBundle.getValue().getBundleContext();
        deploymentManager = new ModelControllerServerDeploymentManager(injectedServerController.getValue());
        delegate = new DeploymentManagerDelegate(systemContext, serviceContainer);
    }

    @Override
    public void stop(StopContext arg0) {
        delegate = null;
    }

    @Override
    public DeployerService getValue() throws IllegalStateException, IllegalArgumentException {
        return delegate;
    }

    private final class DeploymentManagerDelegate extends SystemDeployerService {
        private final ServiceContainer serviceContainer;

        private DeploymentManagerDelegate(BundleContext context, ServiceContainer serviceContainer) {
            super(context);
            this.serviceContainer = serviceContainer;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected Bundle installBundle(Deployment dep) throws BundleException {

            log.tracef("Install deployment: %s", dep);

            Bundle bundle = null;
            String contextName = DeploymentHolderService.getContextName(dep);
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            try {

                // Install the initiator service
                ServiceTarget serviceTarget = serviceContainer.subTarget();
                DeploymentHolderService.addService(serviceTarget, contextName, dep);

                // Build and execute the deployment plan
                InputStream inputStream = dep.getRoot().openStream();
                builder = builder.add(contextName, inputStream).andDeploy();
                DeploymentPlan plan = builder.build();
                DeploymentAction deployAction = builder.getLastAction();
                executeDeploymentPlan(plan, deployAction);

                // Pickup the installed bundle
                // [TODO] Revisit this hack and figure out how to do this with dependencies
                ServiceName serviceNameOne = BundleInstallService.getServiceName(contextName);
                ServiceController<BundleInstallService> controllerOne = (ServiceController<BundleInstallService>) serviceContainer.getService(serviceNameOne);
                FutureServiceValue<BundleInstallService> futureOne = new FutureServiceValue<BundleInstallService>(controllerOne);
                BundleInstallService serviceOne = futureOne.get(5, TimeUnit.SECONDS);
                ServiceName serviceNameTwo = serviceOne.getInstalledBundleName();
                ServiceController<Bundle> controllerTwo = (ServiceController<Bundle>) serviceContainer.getService(serviceNameTwo);
                FutureServiceValue<Bundle> futureTwo = new FutureServiceValue<Bundle>(controllerTwo);
                bundle = futureTwo.get(5, TimeUnit.SECONDS);

            } catch (RuntimeException rte) {
                throw rte;
            } catch (BundleException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BundleException("Cannot deploy bundle: " + dep, ex);
            } finally {
                DeploymentHolderService.removeService(serviceContainer, contextName);
            }

            if (bundle == null)
                throw new IllegalStateException("Cannot find bundle: " + contextName);

            return bundle;
        }

        @Override
        protected void uninstallBundle(Deployment dep, Bundle bundle) throws BundleException {
            try {
                // If there is no {@link BundleInstallService} for the given bundle
                // we unregister the deployment explicitly from the {@link BundleManager}
                String contextName = DeploymentHolderService.getContextName(dep);
                ServiceName serviceName = BundleInstallService.getServiceName(contextName);
                ServiceController<?> controller = serviceContainer.getService(serviceName);
                if (controller == null) {
                    FrameworkIntegration bundleManager = injectedBundleManager.getValue();
                    bundleManager.uninstallBundle(dep);
                    return;
                }

                // Sanity check that the {@link DeploymentService} is there
                final CountDownLatch latch = new CountDownLatch(1);
                controller.addListener(new AbstractServiceListener<Object>() {

                    @Override
                    public void listenerAdded(ServiceController<? extends Object> controller) {
                        if (controller.getState() == State.REMOVED)
                            serviceRemoved(controller);
                        else if (controller.getState() == State.START_FAILED)
                            serviceFailed(controller, controller.getStartException());
                    }

                    @Override
                    public void serviceRemoved(ServiceController<? extends Object> controller) {
                        log.tracef("Service removed: %s", controller.getName());
                        controller.removeListener(this);
                        latch.countDown();
                    }

                    @Override
                    public void serviceFailed(ServiceController<? extends Object> controller, StartException reason) {
                        log.tracef(reason, "Service failed: %s", controller.getName());
                        controller.removeListener(this);
                        latch.countDown();
                    }
                });

                // Undeploy through the deployment manager
                DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
                builder = builder.undeploy(contextName).remove(contextName);
                DeploymentPlan plan = builder.build();
                DeploymentAction removeAction = builder.getLastAction();
                executeDeploymentPlan(plan, removeAction);

                latch.await(10, TimeUnit.SECONDS);
                if (controller.getState() == State.START_FAILED)
                    throw controller.getStartException();
                if (controller.getState() != State.REMOVED)
                    throw new BundleException("BundleInstallService not removed: " + serviceName);

            } catch (RuntimeException rte) {
                throw rte;
            } catch (BundleException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BundleException("Cannot undeploy bundle: " + dep, ex);
            }
        }

        private String executeDeploymentPlan(DeploymentPlan plan, DeploymentAction action) throws Exception {

            Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
            ServerDeploymentPlanResult planResult = future.get();

            ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(action.getId());
            if (actionResult != null) {
                Exception deploymentException = (Exception) actionResult.getDeploymentException();
                if (deploymentException != null)
                    throw deploymentException;
            }

            return action.getDeploymentUnitUniqueName();
        }
    }
}
