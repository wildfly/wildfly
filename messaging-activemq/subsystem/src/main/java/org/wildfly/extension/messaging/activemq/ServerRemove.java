/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.jms.JMSServices;

/**
 * Remove an ActiveMQ Server.
 *
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class ServerRemove extends AbstractRemoveStepHandler {

    static final ServerRemove INSTANCE = new ServerRemove();

    private ServerRemove() {
    }


    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final String serverName = context.getCurrentAddressValue();
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(serverName);
        context.removeService(JMSServices.getJmsManagerBaseServiceName(serviceName));
        context.removeService(MessagingServices.getActiveMQServiceName(serverName));
        // remove services related to broadcast-group/discovery-group that are started
        // when the server is added
        if (model.hasDefined(CommonAttributes.BROADCAST_GROUP)) {
            for (String name : model.get(CommonAttributes.BROADCAST_GROUP).keys()) {
                context.removeService(GroupBindingService.getBroadcastBaseServiceName(serviceName).append(name));
            }
        }
        if (model.hasDefined(CommonAttributes.DISCOVERY_GROUP)) {
            for (String name : model.get(CommonAttributes.DISCOVERY_GROUP).keys()) {
                context.removeService(GroupBindingService.getDiscoveryBaseServiceName(serviceName).append(name));
            }
        }
    }
}
