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
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTERS_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
public class RaRemove extends RaOperationUtil implements OperationStepHandler {
    static final RaRemove INSTANCE = new RaRemove();

    public void execute(OperationContext context, ModelNode operation) {

        final ModelNode opAddr = operation.require(OP_ADDR);
        final String archive = PathAddress.pathAddress(opAddr).getLastElement().getValue();

        operation.get(ARCHIVE.getName()).set(archive);

        // Compensating is add
        final ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);
        final ModelNode compensating = Util.getEmptyOperation(ADD, opAddr);

        if (model.hasDefined(RESOURCEADAPTERS_NAME)) {
            for (ModelNode raNode : model.get(RESOURCEADAPTERS_NAME).asList()) {
                ModelNode raCompensatingNode = raNode.clone();
                compensating.get(RESOURCEADAPTERS_NAME).add(raCompensatingNode);
            }
        }
        context.removeResource(PathAddress.EMPTY_ADDRESS);

        context.addStep(new OperationStepHandler() {
            public void execute(OperationContext context, ModelNode operation) {
                ServiceName raServiceName = ServiceName.of(ConnectorServices.RA_SERVICE, archive);
                context.removeService(raServiceName);

                if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                    // TODO:  RE-ADD SERVICES
                }
            }
        }, OperationContext.Stage.RUNTIME);
        context.completeStep();
    }
}
