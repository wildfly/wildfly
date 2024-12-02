/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.coordinator.service;

import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.wildfly.extension.microprofile.lra.coordinator._private.MicroProfileLRACoordinatorLogger;
import org.wildfly.extension.microprofile.lra.coordinator.jaxrs.LRACoordinatorApp;
import org.wildfly.extension.undertow.Host;

import jakarta.servlet.ServletException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class LRACoordinatorService implements Service {

    public static final String CONTEXT_PATH = "";
    private static final String DEPLOYMENT_NAME = "LRA Coordinator";

    private final Supplier<Host> undertow;

    private volatile DeploymentManager deploymentManager = null;
    private volatile Deployment deployment = null;

    public LRACoordinatorService(Supplier<Host> undertow) {
        this.undertow = undertow;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        deployCoordinator();
    }

    @Override
    public synchronized void stop(final StopContext context) {
        undeployServlet();
    }

    private void deployCoordinator() {
        undeployServlet();

        final Map<String, String> initialParameters = new HashMap<>();
        initialParameters.put("jakarta.ws.rs.Application", LRACoordinatorApp.class.getName());

        MicroProfileLRACoordinatorLogger.LOGGER.startingCoordinator(CONTEXT_PATH);
        final DeploymentInfo coordinatorDeploymentInfo = getDeploymentInfo(DEPLOYMENT_NAME, CONTEXT_PATH, initialParameters);
        deployServlet(coordinatorDeploymentInfo);
    }

    private DeploymentInfo getDeploymentInfo(final String name, final String contextPath, final Map<String, String> initialParameters) {
        final DeploymentInfo deploymentInfo = new DeploymentInfo();
        deploymentInfo.setClassLoader(LRACoordinatorApp.class.getClassLoader());
        deploymentInfo.setContextPath(contextPath);
        deploymentInfo.setDeploymentName(name);
        // JAX-RS setup
        ServletInfo restEasyServlet = new ServletInfo("RESTEasy", HttpServletDispatcher.class).addMapping("/lra-coordinator/*");
        deploymentInfo.addServlets(restEasyServlet);

        for (Map.Entry<String, String> entry : initialParameters.entrySet()) {
            deploymentInfo.addInitParameter(entry.getKey(), entry.getValue());
        }

        return deploymentInfo;
    }

    private void deployServlet(final DeploymentInfo deploymentInfo) {
        deploymentManager = ServletContainer.Factory.newInstance().addDeployment(deploymentInfo);
        deploymentManager.deploy();
        deployment = deploymentManager.getDeployment();

        try {
            undertow.get()
                .registerDeployment(deployment, deploymentManager.start());
        } catch (ServletException e) {
            deployment = null;
        }
    }

    private void undeployServlet() {
        if (deploymentManager != null) {
            if (deployment != null) {
                undertow.get()
                        .unregisterDeployment(deployment);
                deployment = null;
            }
            try {
                deploymentManager.stop();
            } catch (ServletException e) {
                MicroProfileLRACoordinatorLogger.LOGGER.failedStoppingCoordinator(CONTEXT_PATH, e);
            } finally {
                deploymentManager.undeploy();
            }
            deploymentManager = null;
        }
    }
}