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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CLUSTER;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.shallow.ShallowResourceAdd;

/**
 * Handler for adding a discovery group.
 * This is now a ShallowResourceAdd.
 *
 * @deprecated please use Jgroups DiscoveryGroupAdd or Socket DiscoveryGroupAdd
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DiscoveryGroupAdd extends ShallowResourceAdd {

    public static final DiscoveryGroupAdd INSTANCE = new DiscoveryGroupAdd(false);
    public static final DiscoveryGroupAdd LEGACY_INSTANCE = new DiscoveryGroupAdd(true);

    private final boolean isLegacyCall;

    private DiscoveryGroupAdd(boolean isLegacyCall) {
        super(DiscoveryGroupDefinition.ATTRIBUTES);
        this.isLegacyCall = isLegacyCall;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        CommonAttributes.renameChannelToCluster(operation);
        if (!isLegacyCall) {
            ModelNode op = operation.clone();
            PathAddress target = context.getCurrentAddress().getParent();
            OperationStepHandler addHandler;
            if (operation.hasDefined(JGROUPS_CLUSTER.getName())) {
                target = target.append(CommonAttributes.JGROUPS_DISCOVERY_GROUP, context.getCurrentAddressValue());
                addHandler = JGroupsDiscoveryGroupAdd.LEGACY_INSTANCE;
            } else {
                target = target.append(CommonAttributes.SOCKET_DISCOVERY_GROUP, context.getCurrentAddressValue());
                addHandler = SocketDiscoveryGroupAdd.LEGACY_INSTANCE;
            }
            op.get(OP_ADDR).set(target.toModelNode());
            context.addStep(op, addHandler, OperationContext.Stage.MODEL, true);
        }
        super.execute(context, operation);
    }
}
