/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms.legacy;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.LEGACY;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq.jms.JMSServices;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class LegacyConnectionFactoryRemove extends AbstractRemoveStepHandler {

    static final LegacyConnectionFactoryRemove INSTANCE = new LegacyConnectionFactoryRemove();

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        final ServiceName activeMQServerServiceName = MessagingServices.getActiveMQServiceName(context.getCurrentAddress());
        final ServiceName serviceName = JMSServices.getConnectionFactoryBaseServiceName(activeMQServerServiceName).append(LEGACY, name);

        context.removeService(serviceName);

        for (String legacyEntry : LegacyConnectionFactoryDefinition.ENTRIES.unwrap(context, model)) {
            final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(legacyEntry);
            ServiceName binderServiceName = bindInfo.getBinderServiceName();
            context.removeService(binderServiceName);
        }
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        LegacyConnectionFactoryAdd.INSTANCE.performRuntime(context, operation, model);
    }
}
