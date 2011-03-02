/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller;

import org.jboss.as.controller.TransactionalModelController;
import org.jboss.as.domain.client.api.ServerStatus;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.protocol.Connection;
import org.jboss.msc.service.ServiceName;

/**
 * @author Emanuel Muckenhuber
 */
public interface HostController extends TransactionalModelController {

    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller");

    /**
     * Get the host name.
     *
     * @return the host name
     */
    String getName();

    /**
     * Start a local server.
     *
     * @param serverName the server name to start
     * @return the server state
     */
    ServerStatus startServer(String serverName);

    /**
     * Restart a local server.
     *
     * @param serverName the server name
     * @return the server state
     */
    ServerStatus restartServer(String serverName);

    /**
     * Restart a local server.
     *
     * @param serverName the server name
     * @param gracefulTimeout the graceful timeout
     * @return the server state
     */
    ServerStatus restartServer(String serverName, int gracefulTimeout);

    /**
     * Stop a local server.
     *
     * @param serverName the server name to stop
     * @return the server state
     */
    ServerStatus stopServer(String serverName);

    /**
     * Stop a local server.
     *
     * @param serverName the server name to stop
     * @param gracefulTimeout the graceful timeout
     * @return the server state
     */
    ServerStatus stopServer(String serverName, int gracefulTimeout);

    /**
     * Registers a running server in the domain model
     *
     * @param serverName the name of the server
     * @param connection the connection to the running server
     */
    void registerRunningServer(String serverName, Connection connection);

    /**
     * Unregisters a running server from the domain model
     *
     * @param serverName the name of the server
     */
    void unregisterRunningServer(String serverName);

    void startServers(DomainController domainController);

    void stopServers();
}
