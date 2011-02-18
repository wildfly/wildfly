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

package org.jboss.as.messaging.jms;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * The JMS subsystem add handler. Creating and installing the core subsystem services.
 *
 * @author Emanuel Muckenhuber
 */
class JMSSubsystemAdd implements ModelAddOperationHandler {

    static final JMSSubsystemAdd INSTANCE = new JMSSubsystemAdd();

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(operation.require(OP_ADDR));

        if(context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {

                }
            });
            // FIXME the JMSServer is started as part of the messaging subsystem for now
            // final RuntimeOperationContext runtimeContext = (RuntimeOperationContext) context;
            // final BatchBuilder builder = runtimeContext.getBatchBuilder();
            // final JMSService service = new JMSService();
            // final ServiceBuilder<?> serviceBuilder = builder.addService(JMSSubsystemElement.JMS_MANAGER, service)
            //    .addDependency(MessagingSubsystemElement.JBOSS_MESSAGING, HornetQServer.class, service.getHornetQServer())
            //    .addOptionalDependency(JNDI_SERVICE_NAME);
            // serviceBuilder.addListener(new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param));
        }

        final ModelNode node = context.getSubModel();
        node.get(CommonAttributes.CONNECTION_FACTORY).setEmptyObject();
        node.get(CommonAttributes.QUEUE).setEmptyObject();
        node.get(CommonAttributes.TOPIC).setEmptyObject();
        resultHandler.handleResultComplete();
        return new BasicOperationResult(compensatingOperation);
    }

}
