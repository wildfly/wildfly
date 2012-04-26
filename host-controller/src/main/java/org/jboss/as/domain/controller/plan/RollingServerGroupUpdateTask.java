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

import org.jboss.as.controller.remote.TransactionalProtocolClient;
import org.jboss.as.domain.controller.ServerIdentity;

import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
class RollingServerGroupUpdateTask extends AbstractServerGroupRolloutTask implements Runnable {

    public RollingServerGroupUpdateTask(List<ServerUpdateTask> tasks, ServerUpdatePolicy updatePolicy,
                                        ServerTaskExecutor executor, ServerUpdateTask.ServerUpdateResultHandler resultHandler) {
        super(tasks, updatePolicy, executor, resultHandler);
    }

    @Override
    public void execute() {
        boolean interrupted = false;
        final ServerTaskExecutor.ServerOperationListener listener = new ServerTaskExecutor.ServerOperationListener();
        for(final ServerUpdateTask task : tasks) {
            final ServerIdentity identity = task.getServerIdentity();
            if(interrupted || ! updatePolicy.canUpdateServer(identity)) {
                sendCancelledResponse(identity);
                continue;
            }
            // Execute the task
            if(executor.executeTask(listener, task)) {
                try {
                    // Wait for the prepared result
                    final TransactionalProtocolClient.PreparedOperation<ServerTaskExecutor.ServerOperation> prepared = listener.retrievePreparedOperation();
                    recordPreparedOperation(identity, prepared);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        }
        if(interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
