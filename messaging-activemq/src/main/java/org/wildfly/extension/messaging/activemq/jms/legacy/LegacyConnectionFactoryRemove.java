/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
