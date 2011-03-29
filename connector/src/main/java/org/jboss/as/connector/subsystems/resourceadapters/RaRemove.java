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

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersService.ModifiableResourceAdapeters;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
public class RaRemove extends AbstractRaOperation implements ModelRemoveOperationHandler {
    static final RaRemove INSTANCE = new RaRemove();

    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler)
            throws OperationFailedException {
        final ModelNode opAddr = operation.require(OP_ADDR);
        final String archive = PathAddress.pathAddress(opAddr).getLastElement().getValue();

        operation.get(ARCHIVE).set(archive);

        // Compensating is add
        final ModelNode model = context.getSubModel();
        final ModelNode compensating = Util.getEmptyOperation(ADD, opAddr);

        if (model.hasDefined(RESOURCEADAPTERS)) {
            for (ModelNode raNode : model.get(RESOURCEADAPTERS).asList()) {
                ModelNode raCompensatingNode = raNode.clone();
                compensating.get(RESOURCEADAPTERS).add(raCompensatingNode);
            }
        }

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    ModifiableResourceAdapeters resourceAdapters = buildResourceAdaptersObject(operation);

                    final ServiceController<?> raService = context.getServiceRegistry().getService(
                            ConnectorServices.RESOURCEADAPTERS_SERVICE);
                    if (raService != null) {
                        ((ModifiableResourceAdapeters) raService.getValue()).removeAllResourceAdapters(resourceAdapters
                                .getResourceAdapters());
                    }

                    resultHandler.handleResultComplete();

                }
            });
        } else {
            resultHandler.handleResultComplete();
        }

        return new BasicOperationResult(compensating);
    }
}
