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

import static org.jboss.as.server.Services.JBOSS_SERVER_CONTROLLER;

import java.io.InputStream;
import java.util.concurrent.Future;

import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.as.osgi.deployment.DeploymentHolderService;
import org.jboss.as.server.ServerController;
import org.jboss.as.server.deployment.client.ModelControllerServerDeploymentManager;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.BundleManagement;
import org.jboss.osgi.framework.BundleInstallHandler;
import org.jboss.osgi.framework.ServiceNames;
import org.osgi.framework.BundleException;

/**
 * A {@link BundleInstallHandler} that delegates to the {@link ServerDeploymentManager}.
 *
 * @author thomas.diesler@jboss.com
 * @since 24-Nov-2010
 */
public class InstallHandlerIntegration implements BundleInstallHandler {

    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    private final InjectedValue<ServerController> injectedServerController = new InjectedValue<ServerController>();
    private final InjectedValue<BundleManagement> injectedBundleManager = new InjectedValue<BundleManagement>();
    private ServerDeploymentManager deploymentManager;

    public static void addService(final ServiceTarget target) {
        InstallHandlerIntegration service = new InstallHandlerIntegration();
        ServiceBuilder<BundleInstallHandler> builder = target.addService(ServiceNames.BUNDLE_INSTALL_HANDLER, service);
        builder.addDependency(JBOSS_SERVER_CONTROLLER, ServerController.class, service.injectedServerController);
        builder.addDependency(ServiceNames.BUNDLE_MANAGER, BundleManagement.class, service.injectedBundleManager);
        builder.addDependency(ServiceNames.FRAMEWORK_CREATE);
        builder.setInitialMode(Mode.ON_DEMAND);
        builder.install();
    }

    private InstallHandlerIntegration() {
    }

    @Override
    public void start(StartContext context) throws StartException {
        deploymentManager = new ModelControllerServerDeploymentManager(injectedServerController.getValue());
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public BundleInstallHandler getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void installBundle(ServiceTarget serviceTarget, Deployment dep) throws BundleException {
        log.tracef("Install deployment: %s", dep);
        try {

            // Install the {@link Deployment} holder service
            String contextName = DeploymentHolderService.getContextName(dep);
            DeploymentHolderService.addService(serviceTarget, contextName, dep);

            // Build and execute the deployment plan
            InputStream inputStream = dep.getRoot().openStream();
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            builder = builder.add(contextName, inputStream).andDeploy();
            DeploymentPlan plan = builder.build();
            DeploymentAction deployAction = builder.getLastAction();
            executeDeploymentPlan(plan, deployAction);

        } catch (RuntimeException rte) {
            throw rte;
        } catch (BundleException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BundleException("Cannot deploy bundle: " + dep, ex);
        }
    }

    @Override
    public void uninstallBundle(Deployment dep) {
        log.tracef("Uninstall deployment: %s", dep);

        // Always uninstall from the bundle manager
        BundleManagement bundleManager = injectedBundleManager.getValue();
        bundleManager.uninstallBundle(dep);

        try {
            // Undeploy through the deployment manager
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            String contextName = DeploymentHolderService.getContextName(dep);
            builder = builder.undeploy(contextName).remove(contextName);
            DeploymentPlan plan = builder.build();
            DeploymentAction removeAction = builder.getLastAction();
            executeDeploymentPlan(plan, removeAction);
        } catch (Exception ex) {
            log.warn("Cannot undeploy bundle: " + dep, ex);
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
