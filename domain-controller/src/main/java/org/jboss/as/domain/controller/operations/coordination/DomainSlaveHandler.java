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

package org.jboss.as.domain.controller.operations.coordination;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Executes the first phase of a two phase operation on one or more remote, slave host controllers.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DomainSlaveHandler implements NewStepHandler {

    private final ExecutorService executorService;
    private final DomainOperationContext domainOperationContext;
    private final Map<String, NewProxyController> hostProxies;

    public DomainSlaveHandler(final Map<String, NewProxyController> hostProxies, final DomainOperationContext domainOperationContext,
                              final ExecutorService executorService) {
        this.hostProxies = hostProxies;
        this.domainOperationContext = domainOperationContext;
        this.executorService = executorService;
    }

    @Override
    public void execute(final NewOperationContext context, final ModelNode operation) throws OperationFailedException {
        final Map<String, ProxyTask> tasks = new HashMap<String, ProxyTask>();
        final Map<String, Future<ModelNode>> futures = new HashMap<String, Future<ModelNode>>();

        for (Map.Entry<String, NewProxyController> entry : hostProxies.entrySet()) {
            String host = entry.getKey();
            ProxyTask task = new ProxyTask(host, operation, context, entry.getValue());
            tasks.put(host, task);
            futures.put(host, executorService.submit(task));
        }

        boolean interrupted = false;
        try {
            for (Map.Entry<String, Future<ModelNode>> entry : futures.entrySet()) {
                ModelNode result = null;
                try {
                    result = entry.getValue().get();
                } catch (InterruptedException e) {
                    result = new ModelNode();
                    result.get(OUTCOME).set(FAILED);
                    result.get(FAILURE_DESCRIPTION).set(String.format("Interrupted waiting for result from host %s", entry.getKey()));
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    result = new ModelNode();
                    result.get(OUTCOME).set(FAILED);
                    result.get(FAILURE_DESCRIPTION).set(String.format("Caught exception executing operation on host %s -- %s",
                            entry.getKey(), cause == null ? e.toString() : cause.toString()));
                }

                domainOperationContext.addHostControllerResult(entry.getKey(), result);
            }

            context.completeStep();

        } finally {
            try {
                // Inform the remote hosts whether to commit or roll back their updates
                // Do this in parallel
                // TODO consider blocking until all return?
                boolean rollback = domainOperationContext.isCompleteRollback();
                for (ProxyTask task : tasks.values()) {
                    NewModelController.OperationTransaction tx = task.getRemoteTransaction();
                    if (tx != null) {
                        executorService.submit(new ProxyCommitRollbackTask(tx, rollback));
                    }
                }
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
