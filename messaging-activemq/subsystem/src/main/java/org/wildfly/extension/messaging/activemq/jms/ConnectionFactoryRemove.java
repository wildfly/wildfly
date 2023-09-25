/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import static org.wildfly.extension.messaging.activemq.MessagingServices.isSubsystemResource;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.MessagingServices;

/**
 * Update handler removing a connection factory from the Jakarta Messaging subsystem. The
 * runtime action will remove the corresponding {@link ConnectionFactoryService}.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public class ConnectionFactoryRemove extends AbstractRemoveStepHandler {

    public static final ConnectionFactoryRemove INSTANCE = new ConnectionFactoryRemove();

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        ServiceName serviceName;
        if(isSubsystemResource(context)) {
            serviceName = MessagingServices.getActiveMQServiceName();
        } else {
            serviceName = MessagingServices.getActiveMQServiceName(context.getCurrentAddress());
        }
        context.removeService(JMSServices.getConnectionFactoryBaseServiceName(serviceName).append(context.getCurrentAddressValue()));
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        ConnectionFactoryAdd.INSTANCE.performRuntime(context, operation, model);
    }
}
