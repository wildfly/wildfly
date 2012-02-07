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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Operation handler responsible for disabling an existing data-source.
 *
 * @author tefano Maestri
 */
public class RaActivate implements OperationStepHandler {
    static final RaActivate INSTANCE = new RaActivate();

    public void execute(OperationContext context, ModelNode operation)  throws OperationFailedException {

        final ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        final ModelNode address = operation.require(OP_ADDR);
        final String raName = PathAddress.pathAddress(address).getLastElement().getValue();
        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(final OperationContext context, ModelNode operation) throws OperationFailedException {

                    final String archiveName = model.get(ARCHIVE.getName()).asString();

                    RaOperationUtil.deactivateIfActive(context, archiveName);
                    RaOperationUtil.activate(context, raName, archiveName);

                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }

}
