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
package org.jboss.as.domain.controller.plan;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;

import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * {@link ServerUpdateTask} that performs the updates by applying them
 * to a running server.
 */
class RunningServerUpdateTask extends ServerUpdateTask {

    private final ModelNode serverUpdate;

    /**
     * Constructor.
     *
     * @param serverId the id of the server being updated. Cannot be <code>null</code>
     * @param serverUpdate the actual rollback updates to apply to this server. Cannot be <code>null</code>
     * @param updatePolicy the policy that controls whether the updates should be applied. Cannot be <code>null</code>
     * @param resultHandler handler for the result of the update. Cannot be <code>null</code>
     */
    RunningServerUpdateTask(final ServerIdentity serverId,
                            final ModelNode serverUpdate,
                            final ServerUpdatePolicy updatePolicy,
                            final ServerUpdateResultHandler resultHandler) {
        super(serverId, updatePolicy, resultHandler);
        this.serverUpdate = serverUpdate;
    }

    @Override
    public ModelNode getOperation() {
        return getServerOp();
    }

    private ModelNode getServerOp() {
        ModelNode op = serverUpdate.clone();
        ModelNode address = new ModelNode();
        address.add(HOST, serverId.getHostName());
        address.add(RUNNING_SERVER, serverId.getServerName());
        if (serverUpdate.hasDefined(OP_ADDR)) {
            for (Property prop : serverUpdate.get(OP_ADDR).asPropertyList()) {
                address.add(prop.getName(), prop.getValue().asString());
            }
        }
        op.get(OP_ADDR).set(address);
        return op;
    }
}
