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

package org.jboss.as.messaging;

import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.SERVLET_CONNECTOR;
import static org.jboss.as.messaging.MessagingLogger.MESSAGING_LOGGER;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;
import static org.jboss.as.messaging.MessagingServices.getHornetQServiceName;

import java.io.File;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.web.host.ServletBuilder;
import org.jboss.as.web.host.WebDeploymentBuilder;
import org.jboss.as.web.host.WebDeploymentController;
import org.jboss.as.web.host.WebHost;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.netty.channel.socket.http.HttpTunnelingServlet;

/**
 * A service that deploys a servlet to tunnel HornetQ protocol inside HTTP.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
class ServletConnectorService implements Service<ServletConnectorService> {

    private final String hornetqServerName;
    private final String connectorName;

    private InjectedValue<ServerEnvironment> injectedServerEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<WebHost> injectedVirtualHost = new InjectedValue<WebHost>();

    private WebDeploymentController controller;

    static ServiceName getServiceName(final String hornetqServerName, final String connectorName) {
        return getHornetQServiceName(hornetqServerName).append(SERVLET_CONNECTOR, connectorName);
    }

    private static String getContextRoot() {
        return "/" + HORNETQ_SERVER;
    }

    private static String getUrlMapping(String hornetqServerName, String connectorName) {
        return "/" + hornetqServerName + "/" + connectorName;
    }

    static String getServletPath(String hornetqServerName, String connectorName) {
        return getContextRoot() + getUrlMapping(hornetqServerName, connectorName);
    }

    static String getServletEndpoint(final String hornetqServerName, final String connectorName) {
        return "org.hornetq." + hornetqServerName + "." + connectorName;
    }

    static ServiceController<ServletConnectorService> addService(ServiceTarget serviceTarget,
                                              ServiceVerificationHandler verificationHandler,
                                              String host, String hornetqServerName,
                                              String connectorName) {
        ServiceName hornetQServiceName = MessagingServices.getHornetQServiceName(hornetqServerName);

        final ServiceName serviceName = ServletConnectorService.getServiceName(hornetqServerName, connectorName);
        final ServletConnectorService service = new ServletConnectorService(hornetqServerName, connectorName);

        return serviceTarget.addService(serviceName, service)
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedServerEnvironment)
                .addDependency(WebHost.SERVICE_NAME.append(host), WebHost.class, service.injectedVirtualHost)
                .addDependency(HornetQActivationService.getHornetQActivationServiceName(hornetQServiceName))
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install();
    }

    ServletConnectorService(final String hornetqServerName, final String connectorName) {
        this.hornetqServerName = hornetqServerName;
        this.connectorName = connectorName;
        if(hornetqServerName == null) {
            throw MESSAGES.nullVar("hornetqServerName");
        }
        if(connectorName == null) {
            throw MESSAGES.nullVar("connectorName");
        }
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        try {
            WebDeploymentBuilder deployment = createWebDeployment(hornetqServerName, connectorName);
            controller = injectedVirtualHost.getValue().addWebDeployment(deployment);
            controller.create();
            controller.start();
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public synchronized void stop(StopContext context) {
        try {
            controller.stop();
            controller.destroy();
        } catch (Exception e) {
            MESSAGING_LOGGER.failedToDestroy(e, "web deployment", controller.toString());
        }
    }

    @Override
    public ServletConnectorService getValue() throws IllegalStateException {
        return this;
    }

    private WebDeploymentBuilder createWebDeployment(String hornetqServerName, String connectorName) throws Exception {
        WebDeploymentBuilder builder = new WebDeploymentBuilder();
        builder.setContextRoot(getContextRoot());
        File documentRoot = new File(injectedServerEnvironment.getValue().getServerTempDir() + File.separator + hornetqServerName + File.separator + connectorName + "-root");
        // JBoss Web requires a document root. Undertow doesn't.
        builder.setDocumentRoot(documentRoot);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        builder.setClassLoader(classLoader);

        ServletBuilder servlet = new ServletBuilder();
        servlet.setServletName(hornetqServerName + "-" + connectorName);
        servlet.setServletClass(classLoader.loadClass(HttpTunnelingServlet.class.getName()));
        servlet.addUrlMapping(getUrlMapping(hornetqServerName, connectorName));
        servlet.addInitParam("endpoint", "local:" + getServletEndpoint(hornetqServerName, connectorName));
        servlet.setForceInit(true);
        builder.addServlet(servlet);

        return builder;
    }

}