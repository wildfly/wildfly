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

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationResult;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Handler for adding the datasource subsystem.
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 * @author John Bailey
 */
class ResourceAdaptersSubSystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    static final ResourceAdaptersSubSystemAdd INSTANCE = new ResourceAdaptersSubSystemAdd();

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
        // Populate subModel
        final ModelNode subModel = context.getSubModel();
        subModel.setEmptyObject();

        // Initialize the DS and Driver lists
        subModel.get(RESOURCEADAPTER);

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        resultHandler.handleResultComplete();
        return new BasicOperationResult(compensatingOperation);
    }
}
