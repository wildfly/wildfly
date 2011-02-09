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

package org.jboss.as.server.mgmt;

import java.util.List;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.ServerModel;
import org.jboss.as.server.legacy.ServerController;
import org.jboss.msc.service.ServiceName;

/**
 * An object capable of persisting the configuration for a server.
 *
 * @author Brian Stansberry
 */
public interface ServerConfigurationPersister {
    /**
     * ServiceName under which an implementation of this interface should be registered.
     */
    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("server-configuration-persister");

    /**
     * Notification that the server configuration has been modified
     * and thus needs to be persisted.
     *
     * @param serverController the server controller
     * @param serverModel the server model
     */
    void persist(ServerController serverController, ServerModel serverModel);

    /**
     * Load the initial configuration.
     *
     * @param serverController the server controller
     * @return the list of updates to execute in order to create the initial model
     * @throws Exception if the initial configuration cannot be read
     */
    List<AbstractServerModelUpdate<?>> load(ServerController serverController) throws Exception;
}
