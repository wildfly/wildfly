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
package org.jboss.as.osgi.deployment;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.as.server.deployment.DeploymentService;
import org.jboss.as.standalone.client.api.deployment.DeploymentAction;
import org.jboss.as.standalone.client.api.deployment.DeploymentPlan;
import org.jboss.as.standalone.client.api.deployment.DeploymentPlanBuilder;
import org.jboss.as.standalone.client.api.deployment.ServerDeploymentActionResult;
import org.jboss.as.standalone.client.api.deployment.ServerDeploymentManager;
import org.jboss.as.standalone.client.api.deployment.ServerDeploymentPlanResult;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;
import org.jboss.osgi.deployment.deployer.DeployerService;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.SystemDeployerService;
import org.jboss.osgi.framework.bundle.BundleManager;
import org.jboss.osgi.framework.plugin.AbstractDeployerServicePlugin;
import org.jboss.osgi.framework.plugin.DeployerServicePlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * The {@link DeployerServicePlugin} that delegates to the {@link ServerDeploymentManager}.
 *
 * @author thomas.diesler@jboss.com
 * @since 24-Nov-2010
 */
public class ServerDeployerServicePlugin extends AbstractDeployerServicePlugin {

    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    private ServerDeploymentManager deploymentManager;

    public ServerDeployerServicePlugin(BundleManager bundleManager, ServerDeploymentManager deploymentManager) {
        super(bundleManager);
        this.deploymentManager = deploymentManager;
    }

    @Override
    protected DeployerService getDeployerService(final BundleContext context) {

        return new SystemDeployerService(context) {

            @Override
            protected Bundle installBundle(Deployment dep) throws BundleException {

                log.tracef("Install deployment: %s", dep);

                Bundle bundle = null;
                String contextName = InstallBundleInitiatorService.getContextName(dep);
                DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
                try {

                    // Install the initiator service
                    ServiceContainer serviceContainer = getBundleManager().getServiceContainer();
                    BatchBuilder batchBuilder = serviceContainer.batchBuilder();
                    InstallBundleInitiatorService.addService(batchBuilder, contextName, dep);
                    batchBuilder.install();

                    // Build and execute the deployment plan
                    InputStream inputStream = dep.getRoot().openStream();
                    builder = builder.add(contextName, inputStream).andDeploy();
                    DeploymentPlan plan = builder.build();
                    DeploymentAction deployAction = builder.getLastAction();
                    executeDeploymentPlan(plan, deployAction);

                    // Pickup the installed bundle
                    final CountDownLatch latch = new CountDownLatch(1);
                    ServiceName serviceName = OSGiDeploymentService.getServiceName(contextName);
                    ServiceController<?> controller = serviceContainer.getService(serviceName);
                    if (controller != null) {
                        controller.addListener(new AbstractServiceListener<Object>() {

                            @Override
                            public void listenerAdded(ServiceController<? extends Object> controller) {
                                if (controller.getState() == State.UP)
                                    serviceStarted(controller);
                                else if (controller.getState() == State.START_FAILED)
                                    serviceFailed(controller, controller.getStartException());
                            }

                            @Override
                            public void serviceStarted(ServiceController<? extends Object> controller) {
                                log.tracef("Service started: %s", controller.getName());
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
                        latch.await(10, TimeUnit.SECONDS);
                        if (controller.getState() == State.START_FAILED)
                            throw controller.getStartException();
                        if (controller.getState() != State.UP)
                            throw new BundleException("OSGiDeploymentService not available: " + serviceName);

                        Deployment bundleDep = (Deployment) controller.getValue();
                        bundle = bundleDep.getAttachment(Bundle.class);
                    }
                } catch (RuntimeException rte) {
                    throw rte;
                } catch (BundleException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new BundleException("Cannot deploy bundle: " + dep, ex);
                }

                if (bundle == null)
                    throw new IllegalStateException("Cannot find bundle: " + contextName);

                return bundle;
            }

            @Override
            protected void uninstallBundle(Deployment dep, Bundle bundle) throws BundleException {
                try {
                    String contextName = InstallBundleInitiatorService.getContextName(dep);

                    // If there is no {@link OSGiDeploymentService} for the given bundle
                    // we unregister the deployment explicitly from the {@link BundleManager}
                    ServiceName serviceName = OSGiDeploymentService.getServiceName(contextName);
                    if (getBundleManager().getServiceContainer().getService(serviceName) == null)
                        getBundleManager().uninstallBundle(dep);

                    // Sanity check that the {@link DeploymentService} is there
                    serviceName = DeploymentService.getServiceName(contextName);
                    if (getBundleManager().getServiceContainer().getService(serviceName) == null)
                        log.warnf("Cannot find deployment service: %s", serviceName);

                    // Undeploy through the deployment manager
                    DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
                    DeploymentPlan plan = builder.undeploy(contextName).remove(contextName).build();
                    Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
                    future.get();

                } catch (RuntimeException rte) {
                    throw rte;
                } catch (Exception ex) {
                    throw new BundleException("Cannot undeploy bundle: " + dep, ex);
                }
            }
        };
    }

    private String executeDeploymentPlan(DeploymentPlan plan, DeploymentAction deployAction) throws Exception {

        Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
        ServerDeploymentPlanResult planResult = future.get();

        ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(deployAction.getId());
        if (actionResult != null) {
            Exception deploymentException = (Exception) actionResult.getDeploymentException();
            if (deploymentException != null)
                throw deploymentException;
        }

        return deployAction.getDeploymentUnitUniqueName();
    }
}
