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

package org.jboss.as.domain.controller.plan;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import org.jboss.as.controller.remote.TransactionalProtocolClient;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.dmr.ModelNode;

import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
abstract class AbstractServerGroupRolloutTask implements Runnable {

    protected final List<ServerTask> tasks;
    protected final ServerUpdatePolicy updatePolicy;
    protected final ServerRolloutTaskHandler rolloutHandler;
    protected final ServerTaskExecutor executor;
    protected final AbstractServerUpdateTask.ServerUpdateResultHandler resultHandler;

    public AbstractServerGroupRolloutTask(List<ServerTask> tasks, ServerUpdatePolicy updatePolicy, ServerRolloutTaskHandler rolloutHandler, ServerTaskExecutor executor, final AbstractServerUpdateTask.ServerUpdateResultHandler resultHandler) {
        this.tasks = tasks;
        this.updatePolicy = updatePolicy;
        this.rolloutHandler = rolloutHandler;
        this.executor = executor;
        this.resultHandler = resultHandler;
    }

    @Override
    public void run() {
        execute();
    }

    /**
     * Execute the the rollout task.
     */
    protected abstract void execute();

    /**
     * Record a prepared operation.
     *
     * @param identity the server identity
     * @param prepared the prepared operation
     */
    protected void recordPreparedOperation(final ServerIdentity identity, final TransactionalProtocolClient.PreparedOperation<ServerTaskExecutor.ServerOperation> prepared) {
        updatePolicy.recordServerResult(identity, prepared.getPreparedResult());
        rolloutHandler.recordPreparedOperation(prepared);
        resultHandler.handleServerUpdateResult(identity, prepared.getPreparedResult());
    }

    protected void sendCancelledResponse(ServerIdentity serverId) {
        final ModelNode response = new ModelNode();
        response.get(OUTCOME).set(CANCELLED);
        resultHandler.handleServerUpdateResult(serverId, response);
    }

}
