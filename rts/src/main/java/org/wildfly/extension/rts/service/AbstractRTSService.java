/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import javax.servlet.ServletException;

import org.jboss.as.network.SocketBinding;
import org.jboss.jbossts.star.service.ContextListener;
import org.jboss.msc.value.InjectedValue;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;
import org.wildfly.extension.rts.logging.RTSLogger;
import org.wildfly.extension.undertow.Host;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
public class AbstractRTSService {

    private InjectedValue<Host> injectedHost = new InjectedValue<>();

    private InjectedValue<SocketBinding> injectedSocketBinding = new InjectedValue<>();

    private volatile Deployment deployment = null;

    public InjectedValue<Host> getInjectedHost() {
        return injectedHost;
    }

    public InjectedValue<SocketBinding> getInjectedSocketBinding() {
        return injectedSocketBinding;
    }

    protected DeploymentInfo getDeploymentInfo(final String name, final String contextPath, final Map<String, String> initialParameters) {
        final DeploymentInfo deploymentInfo = new DeploymentInfo();
        deploymentInfo.setClassLoader(ParticipantService.class.getClassLoader());
        deploymentInfo.setContextPath(contextPath);
        deploymentInfo.setDeploymentName(name);
        deploymentInfo.addServlets(getResteasyServlet());
        deploymentInfo.addListener(getResteasyListener());
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
            injectedHost.getValue().registerDeployment(deployment, manager.start());
        } catch (ServletException e) {
            RTSLogger.ROOT_LOGGER.warn(e.getMessage(), e);
            deployment = null;
        }
    }

    protected void undeployServlet() {
        if (deployment != null) {
            injectedHost.getValue().unregisterDeployment(deployment);
            deployment = null;
        }
    }

    protected String getBaseUrl() {
        final String address = injectedSocketBinding.getValue().getAddress().getHostAddress();
        final int port = injectedSocketBinding.getValue().getAbsolutePort();

        if (injectedSocketBinding.getValue().getAddress() instanceof Inet4Address) {
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

    private ListenerInfo getResteasyListener() {
        final ListenerInfo listenerInfo = new ListenerInfo(ResteasyBootstrap.class);

        return listenerInfo;
    }

    private ListenerInfo getRestATListener() {
        final ListenerInfo listenerInfo = new ListenerInfo(ContextListener.class);

        return listenerInfo;
    }

}
