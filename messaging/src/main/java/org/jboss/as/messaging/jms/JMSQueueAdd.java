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
import static org.jboss.as.messaging.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.CommonAttributes.SELECTOR;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hornetq.jms.server.JMSServerManager;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;

/**
 * Update handler adding a queue to the JMS subsystem. The
 * runtime action will create the {@link JMSQueueService}.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public class JMSQueueAdd extends AbstractAddStepHandler {

    public static final String OPERATION_NAME = ADD;

    /**
     * Create an "add" operation using the existing model
     */
    public static ModelNode getOperation(ModelNode address, ModelNode existing) {
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

    public static final JMSQueueAdd INSTANCE = new JMSQueueAdd();
    private static final String[] NO_BINDINGS = new String[0];

    protected void populateModel(ModelNode operation, ModelNode model) {
        final String selector;
        if (operation.hasDefined(SELECTOR)) {
            selector = operation.get(SELECTOR).asString();
            model.get(SELECTOR).set(selector);
        } else {
            selector = null;
        }
        if (operation.hasDefined(DURABLE)) {
            model.get(DURABLE).set(operation.get(DURABLE));
        }
        if (operation.hasDefined(ENTRIES)) {
            model.get(ENTRIES).set(operation.get(ENTRIES));
        }
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final String selector;
        if (operation.hasDefined(SELECTOR)) {
            selector = operation.get(SELECTOR).asString();
        } else {
            selector = null;
        }

        final JMSQueueService service = new JMSQueueService(name, selector,
                operation.get(DURABLE).asBoolean(true), jndiBindings(operation));
        final ServiceName serviceName = JMSServices.JMS_QUEUE_BASE.append(name);
        newControllers.add(context.getServiceTarget().addService(serviceName, service)
                .addDependency(JMSServices.JMS_MANAGER, JMSServerManager.class, service.getJmsServer())
                .addListener(verificationHandler)
                .setInitialMode(Mode.ACTIVE)
                .install());

    }


    static String[] jndiBindings(final ModelNode node) {
        if (node.hasDefined(ENTRIES)) {
            final Set<String> bindings = new HashSet<String>();
            for (final ModelNode entry : node.get(ENTRIES).asList()) {
                bindings.add(entry.asString());
            }
            return bindings.toArray(new String[bindings.size()]);
        }
        return NO_BINDINGS;
    }

}
