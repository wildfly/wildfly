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

package org.jboss.as.server.deployment.scanner;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * Handles the addition of the deployment scanning subsystem.
 *
 * @author Emanuel Muckenhuber
 */
public class DeploymentScannerSubsystemRemove implements ModelRemoveOperationHandler, DescriptionProvider {

    static final String OPERATION_NAME = ModelDescriptionConstants.REMOVE;

    static final DeploymentScannerSubsystemRemove INSTANCE = new DeploymentScannerSubsystemRemove();

    private DeploymentScannerSubsystemRemove() {
        //
    }

    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) throws OperationFailedException {

        ModelNode subsystem = context.getSubModel();
        if (subsystem.hasDefined(CommonAttributes.SCANNER)
                && subsystem.get(CommonAttributes.SCANNER).asInt() > 0) {
            throw new OperationFailedException(new ModelNode().set("Cannot remove subsystem while it still has scanners configured. Remove all scanners first."));
        }

        final ModelNode compensatingOperation = Util.getEmptyOperation(DeploymentScannerSubsystemAdd.OPERATION_NAME,
                                                                       operation.get(OP_ADDR));

        resultHandler.handleResultComplete();

        return new BasicOperationResult(compensatingOperation);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentSubsystemDescriptions.getSubsystemRemove(locale);
    }

}
