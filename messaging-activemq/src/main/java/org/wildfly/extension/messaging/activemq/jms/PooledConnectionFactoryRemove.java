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

package org.wildfly.extension.messaging.activemq.jms;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.ENTRIES;

import java.util.List;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
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



    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        final ModelNode operationAddress = operation.require(OP_ADDR);
        final PathAddress address = PathAddress.pathAddress(operationAddress);
        final String name = address.getLastElement().getValue();
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));

        context.removeService(JMSServices.getPooledConnectionFactoryBaseServiceName(serviceName).append(name));

        removeJNDIAliases(context, model.require(ENTRIES.getName()).asList());
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        // TODO:  RE-ADD SERVICES
    }

    /**
     * Remove JNDI alias' binder services.
     *
     * The 1st JNDI entry is not removed by this method as it is already handled when removing
     * the pooled-connection-factory service
     */
    private void removeJNDIAliases(OperationContext context, List<ModelNode> entries) {
        if (entries.size() > 1) {
            for (int i = 1; i < entries.size() ; i++) {
                ContextNames.BindInfo aliasBindInfo = ContextNames.bindInfoFor(entries.get(i).asString());
                context.removeService(aliasBindInfo.getBinderServiceName());
            }
        }
    }
}
