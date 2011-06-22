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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;

import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.dmr.ModelNode;

/**
 * Base class for tasks that can perform an update on a server.
 *
 * Thread-Safety: This class is immutable, but is intended to only have its
 * {@link #run()} method executed once.
 *
 * @author Brian Stansberry
 */
abstract class AbstractServerUpdateTask implements Runnable {

    /**
     * Callback interface to allow the creator of this task to
     * handle the results of the updates.
     */
    interface ServerUpdateResultHandler {

        /**
         * Handle the result of an individual update on an individual server.
         *
         * @param serverId the server that was updated
         * @param response the result of the update
         */
        void handleServerUpdateResult(ServerIdentity serverId, ModelNode response);
    }

    protected final ServerUpdatePolicy updatePolicy;
    protected final ServerIdentity serverId;
    protected final ServerUpdateResultHandler resultHandler;

    /**
     * Create a new update task.
     *
     * @param serverId the id of the server being updated. Cannot be <code>null</code>
     * @param updatePolicy the policy that controls whether the updates should be applied. Cannot be <code>null</code>
     * @param resultHandler handler for the result of the update. Cannot be <code>null</code>
     */
    AbstractServerUpdateTask(final ServerIdentity serverId,
            final ServerUpdatePolicy updatePolicy,
            final ServerUpdateResultHandler resultHandler) {
        assert serverId != null : "serverId is null";
        assert updatePolicy != null : "updatePolicy is null";
        assert resultHandler != null : "resultHandler is null";
        this.serverId = serverId;
        this.updatePolicy = updatePolicy;
        this.resultHandler = resultHandler;
    }

    /**
     * Checks if the {@link ServerUpdatePolicy} allows the update to proceed; if
     * sp {@link #processUpdates() executes them}, else notifies the
     * {@link ServerUpdateResultHandler} that they were cancelled.
     */
    @Override
    public void run() {
        if (updatePolicy.canUpdateServer(serverId)) {
            processUpdates();
        }
        else {
            sendCancelledResponse();
        }
    }

    /**
     * Actually perform the updates.
     */
    protected abstract void processUpdates();

    private void sendCancelledResponse() {
        ModelNode response = new ModelNode();
        response.get(OUTCOME).set(CANCELLED);
        resultHandler.handleServerUpdateResult(serverId, response);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{server=");
        sb.append(serverId.getServerName());
        sb.append("}");
        return sb.toString();
    }
}
