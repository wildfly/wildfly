/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.jms.JMSQueueService.JMS_QUEUE_PREFIX;

import org.apache.activemq.artemis.jms.server.JMSServerManager;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingServices;

/**
 * Update handler removing a queue from the Jakarta Messaging subsystem. The
 * runtime action will remove the corresponding {@link JMSQueueService}.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public class JMSQueueRemove extends AbstractRemoveStepHandler {

    static final JMSQueueRemove INSTANCE = new JMSQueueRemove();

    private JMSQueueRemove() {
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        final ServiceName jmsServiceName = JMSServices.getJmsManagerBaseServiceName(serviceName);
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        ServiceController<?> service = context.getServiceRegistry(true).getService(jmsServiceName);
        JMSServerManager server = JMSServerManager.class.cast(service.getValue());
        try {
            server.destroyQueue(JMS_QUEUE_PREFIX + name, true);
        } catch (Exception e) {
            throw new OperationFailedException(e);
        }

        context.removeService(JMSServices.getJmsQueueBaseServiceName(serviceName).append(name));

        for (String entry : CommonAttributes.DESTINATION_ENTRIES.unwrap(context, model)) {
            final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(entry);
            ServiceName binderServiceName = bindInfo.getBinderServiceName();
            context.removeService(binderServiceName);
        }

        for (String legacyEntry: CommonAttributes.LEGACY_ENTRIES.unwrap(context, model)) {
            final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(legacyEntry);
            ServiceName binderServiceName = bindInfo.getBinderServiceName();
            context.removeService(binderServiceName);
        }
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        JMSQueueAdd.INSTANCE.performRuntime(context, operation, model);
    }
}
