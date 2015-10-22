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

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.operations.common.Util.getWriteAttributeOperation;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * Operation handler responsible for the legacy 'enable' and 'disable' ops used to enable or
 * disable an existing data-source.
 *
 * @author John Bailey
 */
public class DataSourceEnableDisable implements OperationStepHandler {

    static final DataSourceEnableDisable ENABLE = new DataSourceEnableDisable(true);
    static final DataSourceEnableDisable DISABLE = new DataSourceEnableDisable(false);

    private final boolean enabled;

    private DataSourceEnableDisable(final boolean enabled) {
        this.enabled = enabled;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        // Log that this is deprecated
        ConnectorLogger.ROOT_LOGGER.legacyDisableEnableOperation(operation.get(OP).asString());

        // Just delegate to write-attribute.
        ModelNode writeAttributeOp = getWriteAttributeOperation(context.getCurrentAddress(), Constants.ENABLED.getName(), enabled);
        OperationStepHandler writeHandler = context.getResourceRegistration().getOperationHandler(PathAddress.EMPTY_ADDRESS, WRITE_ATTRIBUTE_OPERATION);
        // set the addFirst param to 'true' so the write-attribute runs before any other steps already registered;
        // i.e. in the logically equivalent spot in the sequence to this step
        context.addStep(writeAttributeOp, writeHandler, OperationContext.Stage.MODEL, true);
    }
}
