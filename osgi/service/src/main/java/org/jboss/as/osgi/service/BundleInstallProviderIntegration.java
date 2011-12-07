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

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.as.osgi.deployment.DeploymentHolderService;
import org.jboss.as.server.deployment.client.ModelControllerServerDeploymentManager;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.BundleInstallProvider;
import org.jboss.osgi.framework.BundleManagerService;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.BundleException;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;

import static org.jboss.as.osgi.OSGiMessages.MESSAGES;
import static org.jboss.as.osgi.OSGiLogger.ROOT_LOGGER;
import static org.jboss.as.server.Services.JBOSS_SERVER_CONTROLLER;

/**
 * A {@link BundleInstallProvider} that delegates to the {@link ServerDeploymentManager}.
 *
 * @author thomas.diesler@jboss.com
 * @since 24-Nov-2010
 */
public class BundleInstallProviderIntegration implements BundleInstallProvider {

    private final InjectedValue<ModelController> injectedController = new InjectedValue<ModelController>();
    private final InjectedValue<BundleManagerService> injectedBundleManager = new InjectedValue<BundleManagerService>();
    private volatile ServerDeploymentManager deploymentManager;

    public static ServiceController<?> addService(final ServiceTarget target) {
        BundleInstallProviderIntegration service = new BundleInstallProviderIntegration();
        ServiceBuilder<BundleInstallProvider> builder = target.addService(Services.BUNDLE_INSTALL_PROVIDER, service);
        builder.addDependency(JBOSS_SERVER_CONTROLLER, ModelController.class, service.injectedController);
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManagerService.class, service.injectedBundleManager);
        builder.addDependency(Services.FRAMEWORK_CREATE);
        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
    }

    private BundleInstallProviderIntegration() {
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        ROOT_LOGGER.debugf("Starting: %s in mode %s", controller.getName(), controller.getMode());
        deploymentManager = new ModelControllerServerDeploymentManager(injectedController.getValue());
    }

    @Override
    public void stop(StopContext context) {
        ServiceController<?> controller = context.getController();
        ROOT_LOGGER.debugf("Stopping: %s in mode %s", controller.getName(), controller.getMode());
    }

    @Override
    public BundleInstallProvider getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void installBundle(ServiceTarget serviceTarget, Deployment dep) throws BundleException {
        ROOT_LOGGER.tracef("Install deployment: %s", dep);
        try {

            // Install the {@link Deployment} holder service
            String contextName = DeploymentHolderService.getContextName(dep);
            DeploymentHolderService.addService(serviceTarget, contextName, dep);

            // Build and execute the deployment plan
            InputStream inputStream = dep.getRoot().openStream();
            try {
                DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
                builder = builder.add(contextName, inputStream).andDeploy();
                DeploymentPlan plan = builder.build();
                DeploymentAction deployAction = builder.getLastAction();
                executeDeploymentPlan(plan, deployAction);
            } finally {
                if(inputStream != null) try {
                    inputStream.close();
                } catch (IOException e) {
                    ROOT_LOGGER.debugf(e, "Failed to close resource %s", inputStream);
                }
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (BundleException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BundleException(MESSAGES.cannotDeployBundle(dep), ex);
        }
    }

    @Override
    public void uninstallBundle(Deployment dep) {
        ROOT_LOGGER.tracef("Uninstall deployment: %s", dep);

        try {
            // Undeploy through the deployment manager
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            String contextName = DeploymentHolderService.getContextName(dep);
            builder = builder.undeploy(contextName).remove(contextName);
            DeploymentPlan plan = builder.build();
            DeploymentAction removeAction = builder.getLastAction();
            executeDeploymentPlan(plan, removeAction);
        } catch (Exception ex) {
            ROOT_LOGGER.cannotUndeployBundle(ex, dep);
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
