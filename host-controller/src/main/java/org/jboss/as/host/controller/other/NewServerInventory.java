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

package org.jboss.as.host.controller.other;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.domain.client.api.ServerStatus;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.NewManagedServer;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Inventory of the managed servers.
 *
 * @author Emanuel Muckenhuber
 */
class NewServerInventory {

    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller");
    private final Map<String, NewManagedServer> servers = new HashMap<String, NewManagedServer>();

    private final HostControllerEnvironment environment;
    private final InetSocketAddress managementAddress;

    NewServerInventory(final HostControllerEnvironment environment, final InetSocketAddress managementAddress) {
        this.environment = environment;
        this.managementAddress = managementAddress;
    }

    ServerStatus determineServerStatus(final String serverName) {
        return ServerStatus.UNKNOWN;
    }

    ServerStatus startServer(final String serverName, final ModelNode hostModel, final ModelNode domainModel) {
        log.info("starting server " + serverName);

        ModelCombiner combiner = new ModelCombiner(serverName, domainModel, hostModel);

        System.out.println(combiner.createUpdates());

        return ServerStatus.UNKNOWN;
    }

    ServerStatus restartServer(String serverName) {
        // TODO Auto-generated method stub
        return ServerStatus.UNKNOWN;
    }

    ServerStatus stopServer(String serverName) {
        log.info("stopping server " + serverName);
        return ServerStatus.UNKNOWN;
    }

}
