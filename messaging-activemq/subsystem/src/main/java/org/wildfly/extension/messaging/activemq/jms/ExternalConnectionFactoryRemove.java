/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.MessagingServices;

/**
 * Update handler removing a connection factory from the Jakarta Messaging subsystem. The
 * runtime action will remove the corresponding {@link ConnectionFactoryService}.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public class ExternalConnectionFactoryRemove extends AbstractRemoveStepHandler {

    public static final ExternalConnectionFactoryRemove INSTANCE = new ExternalConnectionFactoryRemove();

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();
        context.removeService(MessagingServices.getActiveMQServiceName(name));
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        ConnectionFactoryAdd.INSTANCE.performRuntime(context, operation, model);
    }
}
