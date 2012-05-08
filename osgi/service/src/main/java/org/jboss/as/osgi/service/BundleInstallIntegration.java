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

import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;
import static org.jboss.as.server.Services.JBOSS_SERVER_CONTROLLER;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
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
import org.jboss.osgi.framework.BundleInstallHandler;
import org.jboss.osgi.framework.BundleManager;
import org.jboss.osgi.framework.IntegrationServices;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.BundleException;

/**
 * A {@link BundleInstallProvider} that delegates to the {@link ServerDeploymentManager}.
 *
 * @author thomas.diesler@jboss.com
 * @since 24-Nov-2010
 */
public class BundleInstallIntegration implements BundleInstallHandler {

    private static Map<String, Deployment> deploymentMap = new HashMap<String, Deployment>();

    private final InjectedValue<ModelController> injectedController = new InjectedValue<ModelController>();
    private final InjectedValue<BundleManager> injectedBundleManager = new InjectedValue<BundleManager>();
    private ServerDeploymentManager deploymentManager;

    public static ServiceController<?> addService(final ServiceTarget target) {
        BundleInstallIntegration service = new BundleInstallIntegration();
        ServiceBuilder<BundleInstallHandler> builder = target.addService(IntegrationServices.BUNDLE_INSTALL_HANDLER, service);
        builder.addDependency(JBOSS_SERVER_CONTROLLER, ModelController.class, service.injectedController);
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManager.class, service.injectedBundleManager);
        builder.addDependency(Services.FRAMEWORK_CREATE);
        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
    }

    private BundleInstallIntegration() {
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        LOGGER.tracef("Starting: %s in mode %s", controller.getName(), controller.getMode());
        deploymentManager = new ModelControllerServerDeploymentManager(injectedController.getValue());
    }

    @Override
    public void stop(StopContext context) {
        ServiceController<?> controller = context.getController();
        LOGGER.tracef("Stopping: %s in mode %s", controller.getName(), controller.getMode());
    }

    @Override
    public BundleInstallHandler getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public static Deployment getDeployment(String contextName) {
        return deploymentMap.get(contextName);
    }

    public static Deployment removeDeployment(String contextName) {
        return deploymentMap.remove(contextName);
    }

    @Override
    public void installBundle(Deployment dep) throws BundleException {
        LOGGER.tracef("Install deployment: %s", dep);
        try {

            // Install the {@link Deployment} holder service
            String contextName = getContextName(dep);
            deploymentMap.put(contextName, dep);

            // Build and execute the deployment plan
            InputStream input = dep.getRoot().openStream();
            try {
                ServerDeploymentHelper server = new ServerDeploymentHelper(deploymentManager);
                server.deploy(contextName, input);
            } finally {
                if(input != null) try {
                    input.close();
                } catch (IOException e) {
                    LOGGER.debugf(e, "Failed to close resource %s", input);
                }
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw MESSAGES.cannotDeployBundle(ex, dep);
        }
    }

    @Override
    public void uninstallBundle(Deployment dep) {
        LOGGER.tracef("Uninstall deployment: %s", dep);
        try {
            ServerDeploymentHelper server = new ServerDeploymentHelper(deploymentManager);
            server.undeploy(getContextName(dep));
        } catch (Exception ex) {
            LOGGER.warnCannotUndeployBundle(ex, dep);
        }
    }

    private String getContextName(Deployment dep) {
        String name = dep.getLocation();
        if (name.endsWith("/"))
            name = name.substring(0, name.length() - 1);
        int idx = name.lastIndexOf("/");
        if (idx > 0)
            name = name.substring(idx + 1);
        return name;
    }
}
