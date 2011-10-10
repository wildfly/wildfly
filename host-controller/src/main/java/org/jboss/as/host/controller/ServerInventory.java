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

import javax.security.auth.callback.CallbackHandler;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.process.ProcessInfo;
import org.jboss.dmr.ModelNode;

/**
 * Inventory of the managed servers.
 *
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
   */
public interface ServerInventory extends ManagedServerLifecycleCallback {

    /**
     * Get the process name for a server {@see ManagedServer#getServerProcessName(String)}.
     *
     * @param serverName the server name
     * @return the server process name
     */
    String getServerProcessName(String serverName);

    /**
     * Determine the current status of a server.
     *
     * @param serverName the server name
     * @return the server status
     */
    ServerStatus determineServerStatus(final String serverName);

    /**
     * Start a server.
     *
     * @param serverName the server name
     * @param domainModel the current domain model
     * @return the server status
     */
    ServerStatus startServer(final String serverName, final ModelNode domainModel);

    /**
     * Restart a server.
     *
     * @param serverName the server name
     * @param gracefulTimeout the graceful timeout in ms
     * @param domainModel the domain model
     * @return the server status
     */
    ServerStatus restartServer(String serverName, final int gracefulTimeout, final ModelNode domainModel);

    /**
     * Stop a server.
     *
     * @param serverName the server nam
     * @param gracefulTimeout the graceful timeout in ms
     * @return the server status
     */
    ServerStatus stopServer(final String serverName, final int gracefulTimeout);

    /**
     * Reconnect to a running managed server.
     *
     * @param serverName the server name
     * @param domainModel the domain model
     * @param running whether the server is running or not
     */
    void reconnectServer(final String serverName, final ModelNode domainModel, final boolean running);

    /**
     * Stop all running servers.
     *
     * @param gracefulTimeout the graceful timeout in ms
     */
    void stopServers(int gracefulTimeout);

    /**
     * Get the server callback handler.
     *
     * @return the callback handler
     */
    CallbackHandler getServerCallbackHandler();

    // TODO remove
    Map<String, ProcessInfo> determineRunningProcesses();

}
