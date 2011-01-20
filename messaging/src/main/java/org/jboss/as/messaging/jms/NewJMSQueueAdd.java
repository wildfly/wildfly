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
import static org.jboss.as.messaging.jms.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.jms.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.jms.CommonAttributes.SELECTOR;

import java.util.HashSet;
import java.util.Set;

import org.hornetq.jms.server.JMSServerManager;
import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;

/**
 * @author Emanuel Muckenhuber
 */
class NewJMSQueueAdd implements ModelAddOperationHandler, RuntimeOperationHandler {

    static final NewJMSQueueAdd INSTANCE = new NewJMSQueueAdd();
    private static final String[] NO_BINDINGS = new String[0];

    /** {@inheritDoc} */
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, ResultHandler resultHandler) {

        final ModelNode address = operation.get(OP_ADDR);
        final String name = address.get(address.asInt() - 1).asString();

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        if(context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext runtimeContext = (NewRuntimeOperationContext) context;

            final JMSQueueService service = new JMSQueueService(name, operation.get(SELECTOR).asString(),
                    operation.get(DURABLE).asBoolean(true), jndiBindings(operation));
            final ServiceName serviceName = JMSServices.JMS_QUEUE_BASE.append(name);
            runtimeContext.getServiceTarget().addService(serviceName, service)
                    .addDependency(JMSServices.JMS_MANAGER, JMSServerManager.class, service.getJmsServer())
                    .setInitialMode(Mode.ACTIVE)
                    .install();
        }

        final ModelNode subModel = context.getSubModel();
        subModel.get(SELECTOR).set(operation.get(SELECTOR));
        subModel.get(DURABLE).set(operation.get(DURABLE));
        subModel.get(ENTRIES).set(operation.get(ENTRIES));

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

    static String[] jndiBindings(final ModelNode node) {
        if(node.has(ENTRIES)) {
            final Set<String> bindings = new HashSet<String>();
            for(final ModelNode entry : node.get(ENTRIES).asList()) {
                bindings.add(entry.asString());
            }
            return bindings.toArray(new String[bindings.size()]);
        }
        return NO_BINDINGS;
    }

}
