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
    void stopServers(int gracefulTimeout);
    String getServerProcessName(String serverName);
    Map<String, ProcessInfo> determineRunningProcesses();
    ServerStatus determineServerStatus(final String serverName);
    ServerStatus startServer(final String serverName, final ModelNode domainModel);
    void reconnectServer(final String serverName, final ModelNode domainModel, final boolean running);
    ServerStatus restartServer(String serverName, final int gracefulTimeout, final ModelNode domainModel);
    ServerStatus stopServer(final String serverName, final int gracefulTimeout);
    CallbackHandler getServerCallbackHandler();

}
