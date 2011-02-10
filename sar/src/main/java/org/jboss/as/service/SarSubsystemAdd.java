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

package org.jboss.as.service;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public class SarSubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    static final SarSubsystemAdd INSTANCE = new SarSubsystemAdd();

    private SarSubsystemAdd() {
    }

    /** {@inheritDoc} */
    @Override
    public Cancellable execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {

        if(context instanceof BootOperationContext) {
            final BootOperationContext updateContext = (BootOperationContext) context;
            updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_SAR_SUB_DEPLOY_CHECK, new SarSubDeploymentProcessor());
            updateContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_SAR_MODULE, new SarModuleDependencyProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_SERVICE_DEPLOYMENT, new ServiceDeploymentParsingProcessor());
            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_SERVICE_DEPLOYMENT, new ParsedServiceDeploymentProcessor());
        }

        context.getSubModel().setEmptyObject();

        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(operation.require(OP_ADDR));

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

}
