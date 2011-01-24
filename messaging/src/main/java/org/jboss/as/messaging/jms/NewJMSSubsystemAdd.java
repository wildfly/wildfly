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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class NewJMSSubsystemAdd implements ModelAddOperationHandler, RuntimeOperationHandler{

    static final NewJMSSubsystemAdd INSTANCE = new NewJMSSubsystemAdd();

    /** {@inheritDoc} */
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        if(context instanceof NewRuntimeOperationContext) {
            // FIXME the JMSServer is started as part of the messaging subsystem for now
            // final NewRuntimeOperationContext runtimeContext = (NewRuntimeOperationContext) context;
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

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

}
