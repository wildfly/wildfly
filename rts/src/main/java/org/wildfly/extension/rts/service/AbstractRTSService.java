/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.rts.service;

import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;

import java.net.Inet4Address;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import jakarta.servlet.ServletException;

import org.jboss.as.network.SocketBinding;
import org.jboss.jbossts.star.service.ContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.wildfly.extension.rts.logging.RTSLogger;
import org.wildfly.extension.undertow.Host;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
public class AbstractRTSService {

    private final Supplier<Host> hostSupplier;
    private final Supplier<SocketBinding> socketBindingSupplier;

    private volatile Deployment deployment = null;

    public AbstractRTSService(final Supplier<Host> hostSupplier, final Supplier<SocketBinding> socketBindingSupplier) {
        this.hostSupplier = hostSupplier;
        this.socketBindingSupplier = socketBindingSupplier;
    }

    protected DeploymentInfo getDeploymentInfo(final String name, final String contextPath, final Map<String, String> initialParameters) {
        final DeploymentInfo deploymentInfo = new DeploymentInfo();
        deploymentInfo.setClassLoader(ParticipantService.class.getClassLoader());
        deploymentInfo.setContextPath(contextPath);
        deploymentInfo.setDeploymentName(name);
        deploymentInfo.addServlets(getResteasyServlet());
        deploymentInfo.addListener(getRestATListener());

        for (Entry<String, String> entry : initialParameters.entrySet()) {
            deploymentInfo.addInitParameter(entry.getKey(), entry.getValue());
        }

        return deploymentInfo;
    }

    protected void deployServlet(final DeploymentInfo deploymentInfo) {
        DeploymentManager manager = ServletContainer.Factory.newInstance().addDeployment(deploymentInfo);

        manager.deploy();
        deployment = manager.getDeployment();

        try {
            hostSupplier.get().registerDeployment(deployment, manager.start());
        } catch (ServletException e) {
            RTSLogger.ROOT_LOGGER.warn(e.getMessage(), e);
            deployment = null;
        }
    }

    protected void undeployServlet() {
        if (deployment != null) {
            hostSupplier.get().unregisterDeployment(deployment);
            deployment = null;
        }
    }

    protected String getBaseUrl() {
        final String address = socketBindingSupplier.get().getAddress().getHostAddress();
        final int port = socketBindingSupplier.get().getAbsolutePort();

        if (socketBindingSupplier.get().getAddress() instanceof Inet4Address) {
            return "http://" + address + ":" + port;
        } else {
            return "http://[" + address + "]:" + port;
        }
    }

    private ServletInfo getResteasyServlet() {
        final ServletInfo servletInfo = new ServletInfo("Resteasy", HttpServletDispatcher.class);
        servletInfo.addMapping("/*");

        return servletInfo;
    }

    private ListenerInfo getRestATListener() {
        final ListenerInfo listenerInfo = new ListenerInfo(ContextListener.class);

        return listenerInfo;
    }

}
