/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.server.deploymentoverlay;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.deployment.ModelContentReference;
import org.jboss.dmr.ModelNode;

/**
 * Handles removal of a deployment overlay content from the model.
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class DeploymentOverlayContentRemove implements OperationStepHandler {

    private final ContentRepository contentRepository;

    public DeploymentOverlayContentRemove(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        ModelNode model = context.readResourceFromRoot(address, false).getModel();
        final byte[] hash;
        if (model.has(CONTENT)) {
            hash = model.get(CONTENT).asBytes();
        } else {
            hash = null;
        }
        context.removeResource(PathAddress.EMPTY_ADDRESS);
        context.completeStep(new OperationContext.ResultHandler() {
            @Override
            public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                if (resultAction != OperationContext.ResultAction.ROLLBACK) {
                    //check that if this is a server group level op the referenced deployment overlay exists
                    if (hash != null) {
                        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
                        contentRepository.removeContent(ModelContentReference.fromModelAddress(address, hash));
                    }
                }
            }
        });
    }
}
