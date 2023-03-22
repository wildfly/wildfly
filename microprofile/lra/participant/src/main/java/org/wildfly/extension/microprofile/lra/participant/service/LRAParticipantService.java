/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.lra.participant.service;

import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import jakarta.servlet.ServletException;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;
import org.wildfly.extension.microprofile.lra.participant._private.MicroProfileLRAParticipantLogger;
import org.wildfly.extension.microprofile.lra.participant.jaxrs.LRAParticipantApplication;
import org.wildfly.extension.undertow.Host;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class LRAParticipantService implements Service {

    public static final String CONTEXT_PATH = "/lra-participant-narayana-proxy";
    private static final String DEPLOYMENT_NAME = "LRA Participant Proxy";

    private final Supplier<Host> undertow;

    private volatile Deployment deployment = null;

    public LRAParticipantService(Supplier<Host> undertow) {
        this.undertow = undertow;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        deployParticipantProxy();
    }

    @Override
    public synchronized void stop(final StopContext context) {
        undeployServlet();
    }

    private void deployParticipantProxy() {
        undeployServlet();

        final Map<String, String> initialParameters = new HashMap<>();
        initialParameters.put("jakarta.ws.rs.Application", LRAParticipantApplication.class.getName());

        MicroProfileLRAParticipantLogger.LOGGER.startingParticipantProxy(CONTEXT_PATH);
        final DeploymentInfo participantProxyDeploymentInfo = getDeploymentInfo(DEPLOYMENT_NAME, CONTEXT_PATH, initialParameters);
        deployServlet(participantProxyDeploymentInfo);
    }

    private DeploymentInfo getDeploymentInfo(final String name, final String contextPath, final Map<String, String> initialParameters) {
        final DeploymentInfo deploymentInfo = new DeploymentInfo();
        deploymentInfo.setClassLoader(LRAParticipantApplication.class.getClassLoader());
        deploymentInfo.setContextPath(contextPath);
        deploymentInfo.setDeploymentName(name);
        // JAX-RS setup
        ServletInfo restEasyServlet = new ServletInfo("RESTEasy", HttpServletDispatcher.class).addMapping("/*");
        deploymentInfo.addServlets(restEasyServlet);
        ListenerInfo restEasyListener = new ListenerInfo(ResteasyBootstrap.class);
        deploymentInfo.addListener(restEasyListener);

        for (Map.Entry<String, String> entry : initialParameters.entrySet()) {
            deploymentInfo.addInitParameter(entry.getKey(), entry.getValue());
        }

        return deploymentInfo;
    }

    private void deployServlet(final DeploymentInfo deploymentInfo) {
        DeploymentManager manager = ServletContainer.Factory.newInstance().addDeployment(deploymentInfo);
        manager.deploy();
        deployment = manager.getDeployment();

        try {
            undertow.get()
                .registerDeployment(deployment, manager.start());
        } catch (ServletException e) {
            deployment = null;
        }
    }

    private void undeployServlet() {
        if (deployment != null) {
            undertow.get()
                .unregisterDeployment(deployment);
            deployment = null;
        }
    }
}