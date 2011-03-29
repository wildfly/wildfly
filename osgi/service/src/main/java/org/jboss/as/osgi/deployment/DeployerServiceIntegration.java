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
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.as.server.ServerController;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.client.ModelControllerServerDeploymentManager;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractService;
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
import org.jboss.osgi.framework.FrameworkExt;
import org.jboss.osgi.framework.FrameworkModuleProvider;
import org.jboss.osgi.framework.SystemBundleProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * The {@link DeployerService} that delegates to the {@link ServerDeploymentManager}.
 *
 * @author thomas.diesler@jboss.com
 * @since 24-Nov-2010
 */
public class DeployerServiceIntegration implements DeployerServiceProvider {

    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    private final InjectedValue<ServerController> injectedServerController = new InjectedValue<ServerController>();
    private final InjectedValue<FrameworkExt> injectedBundleManager = new InjectedValue<FrameworkExt>();
    private final InjectedValue<Bundle> injectedSystemBundle = new InjectedValue<Bundle>();
    private ServerDeploymentManager deploymentManager;
    private DeployerService delegate;

    public static void addService(final ServiceTarget target) {
        DeployerServiceIntegration service = new DeployerServiceIntegration();
        ServiceBuilder<?> serviceBuilder = target.addService(SERVICE_NAME, service);
        serviceBuilder.addDependency(Services.JBOSS_SERVER_CONTROLLER, ServerController.class, service.injectedServerController);
        serviceBuilder.addDependency(SystemBundleProvider.SERVICE_NAME, Bundle.class, service.injectedSystemBundle);
        serviceBuilder.addDependency(FrameworkExt.SERVICE_NAME, FrameworkExt.class, service.injectedBundleManager);
        serviceBuilder.addDependency(FrameworkModuleProvider.SERVICE_NAME);
        serviceBuilder.setInitialMode(Mode.PASSIVE);
        serviceBuilder.install();
    }

    private DeployerServiceIntegration() {
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final ServiceContainer serviceContainer = context.getController().getServiceContainer();
        final BundleContext systemContext = injectedSystemBundle.getValue().getBundleContext();
        deploymentManager = new ModelControllerServerDeploymentManager(injectedServerController.getValue());
        delegate = new SystemDeployerService(systemContext) {

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
                    OSGiDeploymentLatchService.addService(serviceTarget, contextName);

                    // Build and execute the deployment plan
                    InputStream inputStream = dep.getRoot().openStream();
                    builder = builder.add(contextName, inputStream).andDeploy();
                    DeploymentPlan plan = builder.build();
                    DeploymentAction deployAction = builder.getLastAction();
                    executeDeploymentPlan(plan, deployAction);

                    // Pickup the installed bundle
                    final CountDownLatch latch = new CountDownLatch(1);
                    ServiceName serviceName = OSGiDeploymentLatchService.getServiceName(contextName);
                    ServiceController<?> controller = serviceContainer.getService(serviceName);
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
                    try {
                        latch.await(10, TimeUnit.SECONDS);
                        if (controller.getState() == State.START_FAILED)
                            throw controller.getStartException();
                        if (controller.getState() != State.UP)
                            throw new BundleException("OSGiDeploymentService not available: " + serviceName);

                        Deployment bundleDep = (Deployment) controller.getValue();
                        bundle = bundleDep.getAttachment(Bundle.class);
                    } finally {
                        controller.setMode(Mode.REMOVE);
                    }
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
                    // If there is no {@link OSGiDeploymentService} for the given bundle
                    // we unregister the deployment explicitly from the {@link BundleManager}
                    String contextName = DeploymentHolderService.getContextName(dep);
                    ServiceName serviceName = OSGiDeploymentService.getServiceName(contextName);
                    ServiceController<?> controller = serviceContainer.getService(serviceName);
                    if (controller == null) {
                        FrameworkExt bundleManager = injectedBundleManager.getValue();
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
                        throw new BundleException("OSGiDeploymentService not removed: " + serviceName);

                } catch (RuntimeException rte) {
                    throw rte;
                } catch (BundleException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new BundleException("Cannot undeploy bundle: " + dep, ex);
                }
            }
        };

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
        systemContext.registerService(DeployerService.class.getName(), delegate, props);
    }

    @Override
    public void stop(StopContext arg0) {
        delegate = null;
    }

    @Override
    public DeployerService getValue() throws IllegalStateException, IllegalArgumentException {
        return delegate;
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

    static class OSGiDeploymentLatchService extends AbstractService<Deployment> {

        private InjectedValue<Deployment> injectedDeployment = new InjectedValue<Deployment>();

        static void addService(ServiceTarget serviceTarget, String contextName) {
            OSGiDeploymentLatchService service = new OSGiDeploymentLatchService();
            ServiceBuilder<Deployment> serviceBuilder = serviceTarget.addService(getServiceName(contextName), service);
            ServiceName serviceName = OSGiDeploymentService.getServiceName(contextName);
            serviceBuilder.addDependency(serviceName, Deployment.class, service.injectedDeployment);
            serviceBuilder.setInitialMode(Mode.ACTIVE);
            serviceBuilder.install();
        }

        static ServiceName getServiceName(String contextName) {
            return OSGiDeploymentService.SERVICE_NAME_BASE.append("latch", contextName);
        }

        @Override
        public Deployment getValue() throws IllegalStateException {
            return injectedDeployment.getValue();
        }
    }
}
