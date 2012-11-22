/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller.resources;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.host.controller.ServerInventory;
import org.jboss.as.host.controller.descriptions.HostResolver;
import org.jboss.as.host.controller.operations.ServerStatusHandler;
import static org.jboss.as.host.controller.resources.ServerConfigResourceDefinition.registerServerLifecycleOperations;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.controller.resources.ServerRootResourceDefinition;
import org.jboss.as.server.operations.LaunchTypeHandler;
import org.jboss.dmr.ModelNode;

/**
 * {@code ResourceDescription} describing a stopped (running) server.
 *
 * @author Emanuel Muckenhuber
 */
public class StoppedServerResource extends SimpleResourceDefinition {

    private static final PathElement SERVER = PathElement.pathElement(ModelDescriptionConstants.RUNNING_SERVER);

    private final ServerInventory serverInventory;
    public StoppedServerResource(final ServerInventory serverInventory) {
        super(SERVER, HostResolver.getResolver(ModelDescriptionConstants.RUNNING_SERVER, false));
        this.serverInventory = serverInventory;
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        // start,stop,restart
        registerServerLifecycleOperations(resourceRegistration, serverInventory);

        resourceRegistration.registerMetric(ServerConfigResourceDefinition.STATUS, new ServerStatusHandler(serverInventory));
        resourceRegistration.registerMetric(ServerRootResourceDefinition.LAUNCH_TYPE, new LaunchTypeHandler(ServerEnvironment.LaunchType.DOMAIN));

    }

}
