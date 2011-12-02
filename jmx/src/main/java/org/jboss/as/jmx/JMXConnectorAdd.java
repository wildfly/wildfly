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

import java.util.List;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Emanuel Muckenhuber
 */
class JMXConnectorAdd implements OperationStepHandler {

    static final JMXConnectorAdd INSTANCE = new JMXConnectorAdd();
    static final String OPERATION_NAME = "add-connector";

    private JMXConnectorAdd() {
        //
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final Resource subsystem = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode model = subsystem.getModel();

        final String serverBinding = operation.require(CommonAttributes.SERVER_BINDING).asString();
        final String registryBinding = operation.require(CommonAttributes.REGISTRY_BINDING).asString();
        final String passwordFile = (operation.hasDefined(CommonAttributes.PASSWORD_FILE) ? operation.get(CommonAttributes.PASSWORD_FILE).asString() : null);
        final String accessFile = (operation.hasDefined(CommonAttributes.ACCESS_FILE) ? operation.get(CommonAttributes.ACCESS_FILE).asString() : null);

        model.get(CommonAttributes.SERVER_BINDING).set(serverBinding);
        model.get(CommonAttributes.REGISTRY_BINDING).set(registryBinding);
        if(passwordFile != null) {
            model.get(CommonAttributes.PASSWORD_FILE).set(passwordFile);
        }
        if(accessFile != null) {
            model.get(CommonAttributes.ACCESS_FILE).set(accessFile);
        }
        if(context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    final ServiceTarget target = context.getServiceTarget();
                    final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                    JMXConnectorService.addService(target, serverBinding, registryBinding, passwordFile, accessFile, verificationHandler);
                    context.addStep(verificationHandler, OperationContext.Stage.VERIFY);
                    if(context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        context.removeService(JMXConnectorService.SERVICE_NAME);
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }
}
