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

import static org.wildfly.extension.messaging.activemq.CommonAttributes.DURABLE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.FILTER;
import static org.wildfly.extension.messaging.activemq.QueueDefinition.DEFAULT_ROUTING_TYPE;

import java.util.List;
import java.util.function.Supplier;
import org.apache.activemq.artemis.api.core.RoutingType;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

import static org.wildfly.extension.messaging.activemq.QueueDefinition.ROUTING_TYPE;

import java.util.Locale;

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

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final AttributeDefinition attributeDefinition : QueueDefinition.ATTRIBUTES) {
            attributeDefinition.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(context.getCurrentAddress());
        ServiceController<?> serverService = registry.getService(serviceName);
        if (serverService != null) {
            final String queueName = context.getCurrentAddressValue();
            final CoreQueueConfiguration queueConfiguration = createCoreQueueConfiguration(context, queueName, model);
            final ServiceName queueServiceName = MessagingServices.getQueueBaseServiceName(serviceName).append(queueName);
            final ServiceBuilder sb = context.getServiceTarget().addService(queueServiceName);
            sb.requires(ActiveMQActivationService.getServiceName(serviceName));
            Supplier<ActiveMQServer> serverSupplier = sb.requires(serviceName);
            final QueueService service = new QueueService(serverSupplier, queueConfiguration, false, true);
            sb.setInitialMode(Mode.PASSIVE);
            sb.setInstance(service);
            sb.install();
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
        final String filter = FILTER.resolveModelAttribute(context, model).asStringOrNull();
        final String routing;
        if(DEFAULT_ROUTING_TYPE != null && ! model.hasDefined(ROUTING_TYPE.getName())) {
            routing = RoutingType.valueOf(DEFAULT_ROUTING_TYPE.toUpperCase(Locale.ENGLISH)).toString();
        } else {
            routing = ROUTING_TYPE.resolveModelAttribute(context, model).asString();
        }
        final boolean durable = DURABLE.resolveModelAttribute(context, model).asBoolean();

        return new CoreQueueConfiguration()
                .setAddress(queueAddress)
                .setName(name)
                .setFilterString(filter)
                .setDurable(durable)
                .setRoutingType(RoutingType.valueOf(routing));
    }

}
