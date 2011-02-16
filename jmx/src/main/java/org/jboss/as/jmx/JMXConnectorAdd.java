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

package org.jboss.as.jmx;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.RuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.RuntimeTask;
import org.jboss.as.server.RuntimeTaskContext;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Emanuel Muckenhuber
 */
class JMXConnectorAdd implements ModelUpdateOperationHandler, RuntimeOperationHandler {

    static final JMXConnectorAdd INSTANCE = new JMXConnectorAdd();

    static final String OPERATION_NAME = "add-connector";

    private JMXConnectorAdd() {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {

        final String serverBinding = operation.require(CommonAttributes.SERVER_BINDING).asString();
        final String registryBinding = operation.require(CommonAttributes.REGISTRY_BINDING).asString();

        context.getSubModel().get(CommonAttributes.SERVER_BINDING).set(serverBinding);
        context.getSubModel().get(CommonAttributes.REGISTRY_BINDING).set(registryBinding);

        final ModelNode compensatingOperation = Util.getEmptyOperation(JMXConnectorRemove.OPERATION_NAME, operation.require(OP_ADDR));

        if (context instanceof RuntimeOperationContext) {
            RuntimeOperationContext.class.cast(context).executeRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context, ResultHandler resultHandler) throws OperationFailedException {
                    final ServiceTarget target = context.getServiceTarget();
                    JMXConnectorService.addService(target, serverBinding, registryBinding);
                    resultHandler.handleResultComplete();
                }
            }, resultHandler);
        } else {
            resultHandler.handleResultComplete();
        }

        return new BasicOperationResult(compensatingOperation);
    }

}
