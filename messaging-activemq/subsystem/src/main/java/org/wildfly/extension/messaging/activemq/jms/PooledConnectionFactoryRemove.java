/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.ENTRIES;

import java.util.List;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 *         Date: 5/13/11
 *         Time: 3:30 PM
 */
public class PooledConnectionFactoryRemove extends AbstractRemoveStepHandler {

    public static final PooledConnectionFactoryRemove INSTANCE = new PooledConnectionFactoryRemove();

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        ServiceName serviceName = MessagingServices.getActiveMQServiceName(context.getCurrentAddress());
        context.removeService(JMSServices.getPooledConnectionFactoryBaseServiceName(serviceName).append(context.getCurrentAddressValue()));
        removeJNDIAliases(context, model.require(ENTRIES.getName()).asList());
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        // TODO:  RE-ADD SERVICES
    }

    /**
     * Remove JNDI alias' binder services.
     *
     * The 1st JNDI entry is not removed by this method as it is already handled when removing
     * the pooled-connection-factory service
     */
    protected void removeJNDIAliases(OperationContext context, List<ModelNode> entries) {
        if (entries.size() > 1) {
            for (int i = 1; i < entries.size() ; i++) {
                ContextNames.BindInfo aliasBindInfo = ContextNames.bindInfoFor(entries.get(i).asString());
                context.removeService(aliasBindInfo.getBinderServiceName());
            }
        }
    }
}
