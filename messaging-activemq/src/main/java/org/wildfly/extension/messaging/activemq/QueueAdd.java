/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

/**
 *
 */
package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.DURABLE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.FILTER;

import java.util.List;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Core queue add update.
 *
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class QueueAdd extends AbstractAddStepHandler {

    public static final QueueAdd INSTANCE = new QueueAdd(QueueDefinition.ATTRIBUTES);

    private QueueAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final AttributeDefinition attributeDefinition : QueueDefinition.ATTRIBUTES) {
            attributeDefinition.validateAndSet(operation, model);
        }
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> serverService = registry.getService(serviceName);
        if (serverService != null) {
            PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            final String queueName = address.getLastElement().getValue();
            final CoreQueueConfiguration queueConfiguration = createCoreQueueConfiguration(context, queueName, model);
            final QueueService service = new QueueService(queueConfiguration, false);
            final ServiceName queueServiceName = MessagingServices.getQueueBaseServiceName(serviceName).append(queueName);
            context.getServiceTarget().addService(queueServiceName, service)
                    .addDependency(ActiveMQActivationService.getServiceName(serviceName))
                    .addDependency(serviceName, ActiveMQServer.class, service.getActiveMQServer())
                    .setInitialMode(Mode.PASSIVE)
                    .install();
        }
        // else the initial subsystem install is not complete; MessagingSubsystemAdd will add a
        // handler that calls addQueueConfigs
    }

    static void addQueueConfigs(final OperationContext context, final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.QUEUE)) {
            final List<CoreQueueConfiguration> configs = configuration.getQueueConfigurations();
            for (Property prop : model.get(CommonAttributes.QUEUE).asPropertyList()) {
                configs.add(createCoreQueueConfiguration(context, prop.getName(), prop.getValue()));

            }
        }
    }

    private static CoreQueueConfiguration createCoreQueueConfiguration(final OperationContext context, String name, ModelNode model) throws OperationFailedException {
        final String queueAddress = QueueDefinition.ADDRESS.resolveModelAttribute(context, model).asString();
        final ModelNode filterNode =  FILTER.resolveModelAttribute(context, model);
        final String filter = filterNode.isDefined() ? filterNode.asString() : null;
        final boolean durable = DURABLE.resolveModelAttribute(context, model).asBoolean();

        return new CoreQueueConfiguration()
                .setAddress(queueAddress)
                .setName(name)
                .setFilterString(filter)
                .setDurable(durable);
    }

}
