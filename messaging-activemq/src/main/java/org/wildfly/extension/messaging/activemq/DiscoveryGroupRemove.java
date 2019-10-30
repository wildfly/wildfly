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

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CLUSTER;
import static org.wildfly.extension.messaging.activemq.DiscoveryGroupDefinition.CAPABILITY;
import static org.wildfly.extension.messaging.activemq.DiscoveryGroupDefinition.JGROUPS_CHANNEL;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.spi.ClusteringDefaultRequirement;

/**
 * Removes a discovery group.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DiscoveryGroupRemove extends AbstractRemoveStepHandler {

    public static final DiscoveryGroupRemove INSTANCE = new DiscoveryGroupRemove();

    private DiscoveryGroupRemove() {
        super();
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();

        ModelNode model = resource.getModel();
        if (JGROUPS_CLUSTER.resolveModelAttribute(context, model).isDefined() && !JGROUPS_CHANNEL.resolveModelAttribute(context, model).isDefined()) {
            context.deregisterCapabilityRequirement(ClusteringDefaultRequirement.COMMAND_DISPATCHER_FACTORY.getName(), CAPABILITY.getDynamicName(address));
        }

        context.deregisterCapability(CAPABILITY.getDynamicName(address));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        context.reloadRequired();
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        context.revertReloadRequired();
    }
}