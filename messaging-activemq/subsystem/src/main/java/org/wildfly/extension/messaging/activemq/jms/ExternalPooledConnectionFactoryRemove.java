/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.ENTRIES;

import org.jboss.as.controller.OperationContext;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Operation Handler to remove a Jakarta Messaging external pooled Connection Factory.
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ExternalPooledConnectionFactoryRemove extends PooledConnectionFactoryRemove {

    public static final ExternalPooledConnectionFactoryRemove INSTANCE = new ExternalPooledConnectionFactoryRemove();

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        ServiceName serviceName = MessagingServices.getActiveMQServiceName();
        context.removeService(JMSServices.getPooledConnectionFactoryBaseServiceName(serviceName).append(context.getCurrentAddressValue()));
        removeJNDIAliases(context, model.require(ENTRIES.getName()).asList());
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        // TODO:  RE-ADD SERVICES
    }
}
