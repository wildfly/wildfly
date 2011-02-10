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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.jms.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.jms.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.jms.CommonAttributes.SELECTOR;

import java.util.HashSet;
import java.util.Set;

import org.hornetq.jms.server.JMSServerManager;
import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.RuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;

/**
 * Update handler adding a queue to the JMS subsystem. The
 * runtime action will create the {@link JMSQueueService}.
 *
 * @author Emanuel Muckenhuber
 */
class JMSQueueAdd implements ModelAddOperationHandler, RuntimeOperationHandler {

    public static final String OPERATION_NAME = ADD;

    /** Create an "add" operation using the existing model */
    static ModelNode getOperation(ModelNode address, ModelNode existing) {
        ModelNode op = Util.getEmptyOperation(OPERATION_NAME, address);
        if (existing.hasDefined(SELECTOR)) {
            op.get(SELECTOR).set(existing.get(SELECTOR));
        }
        if (existing.hasDefined(DURABLE)) {
            op.get(DURABLE).set(existing.get(DURABLE));
        }
        if (existing.hasDefined(ENTRIES)) {
            op.get(ENTRIES).set(existing.get(ENTRIES));
        }
        return op;
    }

    static final JMSQueueAdd INSTANCE = new JMSQueueAdd();
    private static final String[] NO_BINDINGS = new String[0];

    /** {@inheritDoc} */
    @Override
    public Cancellable execute(final OperationContext context, final ModelNode operation, ResultHandler resultHandler) {

        ModelNode opAddr = operation.require(OP_ADDR);
        final PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();

        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(opAddr);

        String selector = null;
        final ModelNode subModel = context.getSubModel();
        if (operation.hasDefined(SELECTOR)) {
            selector = operation.get(SELECTOR).asString();
            subModel.get(SELECTOR).set(selector);
        }
        if (operation.hasDefined(DURABLE)) {
            subModel.get(DURABLE).set(operation.get(DURABLE));
        }
        if (operation.hasDefined(ENTRIES)) {
            subModel.get(ENTRIES).set(operation.get(ENTRIES));
        }

        if(context instanceof RuntimeOperationContext) {
            final RuntimeOperationContext runtimeContext = (RuntimeOperationContext) context;

            final JMSQueueService service = new JMSQueueService(name, selector,
                    operation.get(DURABLE).asBoolean(true), jndiBindings(operation));
            final ServiceName serviceName = JMSServices.JMS_QUEUE_BASE.append(name);
            runtimeContext.getServiceTarget().addService(serviceName, service)
                    .addDependency(JMSServices.JMS_MANAGER, JMSServerManager.class, service.getJmsServer())
                    .setInitialMode(Mode.ACTIVE)
                    .install();
        }

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

    static String[] jndiBindings(final ModelNode node) {
        if(node.hasDefined(ENTRIES)) {
            final Set<String> bindings = new HashSet<String>();
            for(final ModelNode entry : node.get(ENTRIES).asList()) {
                bindings.add(entry.asString());
            }
            return bindings.toArray(new String[bindings.size()]);
        }
        return NO_BINDINGS;
    }

}
