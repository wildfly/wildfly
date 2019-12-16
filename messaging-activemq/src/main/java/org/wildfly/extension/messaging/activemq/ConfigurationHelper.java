/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.DURABLE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.FILTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SELECTOR;
import static org.wildfly.extension.messaging.activemq.QueueDefinition.DEFAULT_ROUTING_TYPE;
import static org.wildfly.extension.messaging.activemq.QueueDefinition.ROUTING_TYPE;

import java.util.List;
import java.util.Locale;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.utils.SelectorTranslator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Helper to create Artemis configuration.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class ConfigurationHelper {

    static void addQueueConfigurations(final OperationContext context, final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.QUEUE)) {
            final List<CoreQueueConfiguration> configs = configuration.getQueueConfigurations();
            for (Property prop : model.get(CommonAttributes.QUEUE).asPropertyList()) {
                configs.add(createCoreQueueConfiguration(context, prop.getName(), prop.getValue()));
            }
        }
        if (model.hasDefined(CommonAttributes.JMS_QUEUE)) {
            final List<CoreQueueConfiguration> configs = configuration.getQueueConfigurations();
            for (Property prop : model.get(CommonAttributes.JMS_QUEUE).asPropertyList()) {
                configs.add(createJMSDestinationConfiguration(context, prop.getName(), prop.getValue(), true));
            }
        }
        if (model.hasDefined(CommonAttributes.JMS_TOPIC)) {
            final List<CoreQueueConfiguration> configs = configuration.getQueueConfigurations();
            for (Property prop : model.get(CommonAttributes.JMS_TOPIC).asPropertyList()) {
                configs.add(createJMSDestinationConfiguration(context, prop.getName(), prop.getValue(), false));
            }
        }
    }

    static CoreQueueConfiguration createCoreQueueConfiguration(final OperationContext context, String name, ModelNode model) throws OperationFailedException {
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

    static CoreQueueConfiguration createJMSDestinationConfiguration(final OperationContext context, String name, ModelNode model, boolean isQueue) throws OperationFailedException {
        final String selector = SELECTOR.resolveModelAttribute(context, model).asStringOrNull();
        final boolean durable = DURABLE.resolveModelAttribute(context, model).asBoolean();
        final String destinationAddress = isQueue ? "jms.queue." + name : "jms.topic." + name;

        return new CoreQueueConfiguration()
                .setAddress(destinationAddress)
                .setName(destinationAddress)
                .setFilterString(SelectorTranslator.convertToActiveMQFilterString(selector))
                .setDurable(durable)
                .setRoutingType(isQueue ? RoutingType.ANYCAST : RoutingType.MULTICAST);
    }
}
