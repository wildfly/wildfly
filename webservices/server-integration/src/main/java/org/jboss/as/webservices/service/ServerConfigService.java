/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.webservices.service;

import javax.management.MBeanServer;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.webservices.config.ServerConfigImpl;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.wsf.common.management.AbstractServerConfig;
import org.jboss.wsf.spi.management.ServerConfig;

/**
 * WS server config service.
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ServerConfigService implements Service<ServerConfig> {

    private static final Logger log = Logger.getLogger(ServerConfigService.class);
    private static final ServiceName MBEAN_SERVER_NAME = ServiceName.JBOSS.append("mbean", "server");
    private final AbstractServerConfig serverConfig;

    private ServerConfigService(final AbstractServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public ServerConfig getValue() {
        return serverConfig;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        log.tracef("Starting %s", ServerConfigService.class.getName());
        final ClassLoader origClassloader = SecurityActions.getContextClassLoader();
        try {
            SecurityActions.setContextClassLoader(this.getClass().getClassLoader());
            serverConfig.create();
        } catch (final Exception e) {
            log.fatal("Error while creating configuration service", e);
            throw new StartException(e);
        } finally {
            SecurityActions.setContextClassLoader(origClassloader);
        }
    }

    @Override
    public void stop(final StopContext context) {
        log.tracef("Stopping %s", ServerConfigService.class.getName());
        try {
            serverConfig.destroy();
        } catch (final Exception e) {
            log.error("Error while destroying configuration service", e);
        }
    }

    public static void install(final ServiceTarget serviceTarget, final ServerConfigImpl serverConfig) {
        final ServiceBuilder<ServerConfig> builder = serviceTarget.addService(WSServices.CONFIG_SERVICE, new ServerConfigService(serverConfig));
        builder.addDependency(DependencyType.REQUIRED, MBEAN_SERVER_NAME, MBeanServer.class, serverConfig.getMBeanServerInjector());
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, serverConfig.getServerEnvironmentInjector());
        builder.setInitialMode(Mode.ACTIVE);
        builder.install();
    }

}
