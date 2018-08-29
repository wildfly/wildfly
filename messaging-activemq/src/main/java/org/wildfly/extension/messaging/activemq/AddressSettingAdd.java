/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.ActiveMQActivationService.getActiveMQServer;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.DEAD_LETTER_ADDRESS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.EXPIRY_ADDRESS;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.settings.impl.AddressFullMessagePolicy;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.core.settings.impl.SlowConsumerPolicy;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@code OperationStepHandler} adding a new address setting.
 *
 * @author Emanuel Muckenhuber
 */
class AddressSettingAdd extends AbstractAddStepHandler {

    static final OperationStepHandler INSTANCE = new AddressSettingAdd(AddressSettingDefinition.ATTRIBUTES);

    private AddressSettingAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, final Resource resource) throws OperationFailedException {
        super.populateModel(context, operation, resource);

        context.addStep(AddressSettingsValidator.ADD_VALIDATOR, OperationContext.Stage.MODEL);
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final ActiveMQServer server = getActiveMQServer(context, operation);
        if(server != null) {
            final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
            final AddressSettings settings = createSettings(context, model);
            server.getAddressSettingsRepository().addMatch(address.getLastElement().getValue(), settings);
        }
    }

    /**
     * Create a setting.
     *
     * @param context the operation context
     * @param config the detyped config
     * @return the address settings
     *
     * @throws OperationFailedException if the model is invalid
     */
    static AddressSettings createSettings(final OperationContext context, final ModelNode config) throws OperationFailedException {
        final AddressSettings settings = new AddressSettings();

        if (config.hasDefined(AddressSettingDefinition.ADDRESS_FULL_MESSAGE_POLICY.getName())) {
            final AddressFullMessagePolicy addressPolicy = AddressFullMessagePolicy.valueOf(AddressSettingDefinition.ADDRESS_FULL_MESSAGE_POLICY.resolveModelAttribute(context, config).asString());
            settings.setAddressFullMessagePolicy(addressPolicy);
        }
        if (config.hasDefined(DEAD_LETTER_ADDRESS.getName())) {
            settings.setDeadLetterAddress(asSimpleString(DEAD_LETTER_ADDRESS.resolveModelAttribute(context, config), null));
        }
        if (config.hasDefined(AddressSettingDefinition.LAST_VALUE_QUEUE.getName())) {
            settings.setDefaultLastValueQueue(AddressSettingDefinition.LAST_VALUE_QUEUE.resolveModelAttribute(context, config).asBoolean());
        }
        if (config.hasDefined(AddressSettingDefinition.MAX_DELIVERY_ATTEMPTS.getName())) {
            settings.setMaxDeliveryAttempts(AddressSettingDefinition.MAX_DELIVERY_ATTEMPTS.resolveModelAttribute(context, config).asInt());
        }
        if (config.hasDefined(AddressSettingDefinition.MAX_SIZE_BYTES.getName())) {
            settings.setMaxSizeBytes(AddressSettingDefinition.MAX_SIZE_BYTES.resolveModelAttribute(context, config).asLong());
        }
        if (config.hasDefined(AddressSettingDefinition.MESSAGE_COUNTER_HISTORY_DAY_LIMIT.getName())) {
            settings.setMessageCounterHistoryDayLimit(AddressSettingDefinition.MESSAGE_COUNTER_HISTORY_DAY_LIMIT.resolveModelAttribute(context, config).asInt());
        }
        if (config.hasDefined(CommonAttributes.EXPIRY_ADDRESS.getName())) {
            settings.setExpiryAddress(asSimpleString(EXPIRY_ADDRESS.resolveModelAttribute(context, config), null));
        }
        if (config.hasDefined(AddressSettingDefinition.EXPIRY_DELAY.getName())) {
            settings.setExpiryDelay(AddressSettingDefinition.EXPIRY_DELAY.resolveModelAttribute(context, config).asLong());
        }
        if (config.hasDefined(AddressSettingDefinition.REDELIVERY_DELAY.getName())) {
            settings.setRedeliveryDelay(AddressSettingDefinition.REDELIVERY_DELAY.resolveModelAttribute(context, config).asLong());
        }
        if (config.hasDefined(AddressSettingDefinition.REDELIVERY_MULTIPLIER.getName())) {
            settings.setRedeliveryMultiplier(AddressSettingDefinition.REDELIVERY_MULTIPLIER.resolveModelAttribute(context, config).asDouble());
        }
        if (config.hasDefined(AddressSettingDefinition.MAX_REDELIVERY_DELAY.getName())) {
            settings.setMaxRedeliveryDelay(AddressSettingDefinition.MAX_REDELIVERY_DELAY.resolveModelAttribute(context, config).asLong());
        }
        if (config.hasDefined(AddressSettingDefinition.REDISTRIBUTION_DELAY.getName())) {
            settings.setRedistributionDelay(AddressSettingDefinition.REDISTRIBUTION_DELAY.resolveModelAttribute(context, config).asLong());
        }
        if (config.hasDefined(AddressSettingDefinition.PAGE_SIZE_BYTES.getName())) {
            settings.setPageSizeBytes(AddressSettingDefinition.PAGE_SIZE_BYTES.resolveModelAttribute(context, config).asLong());
        }
        if (config.hasDefined(AddressSettingDefinition.PAGE_MAX_CACHE_SIZE.getName())) {
            settings.setPageCacheMaxSize(AddressSettingDefinition.PAGE_MAX_CACHE_SIZE.resolveModelAttribute(context, config).asInt());
        }
        if (config.hasDefined(AddressSettingDefinition.SEND_TO_DLA_ON_NO_ROUTE.getName())) {
            settings.setSendToDLAOnNoRoute(AddressSettingDefinition.SEND_TO_DLA_ON_NO_ROUTE.resolveModelAttribute(context, config).asBoolean());
        }
        if (config.hasDefined(AddressSettingDefinition.SLOW_CONSUMER_CHECK_PERIOD.getName())) {
            settings.setSlowConsumerCheckPeriod(AddressSettingDefinition.SLOW_CONSUMER_CHECK_PERIOD.resolveModelAttribute(context, config).asLong());
        }
        if (config.hasDefined(AddressSettingDefinition.SLOW_CONSUMER_POLICY.getName())) {
            final SlowConsumerPolicy slowConsumerPolicy = SlowConsumerPolicy.valueOf(AddressSettingDefinition.SLOW_CONSUMER_POLICY.resolveModelAttribute(context, config).asString());
            settings.setSlowConsumerPolicy(slowConsumerPolicy);
        }
        if (config.hasDefined(AddressSettingDefinition.SLOW_CONSUMER_THRESHOLD.getName())) {
            settings.setSlowConsumerThreshold(AddressSettingDefinition.SLOW_CONSUMER_THRESHOLD.resolveModelAttribute(context, config).asLong());
        }
        // always set the auto-create|delete-jms-queues attributes as their default attribute values differ from Artemis defaults.
        settings.setAutoCreateJmsQueues(AddressSettingDefinition.AUTO_CREATE_JMS_QUEUES.resolveModelAttribute(context, config).asBoolean());
        settings.setAutoDeleteJmsQueues(AddressSettingDefinition.AUTO_DELETE_JMS_QUEUES.resolveModelAttribute(context, config).asBoolean());
        settings.setAutoCreateQueues(AddressSettingDefinition.AUTO_CREATE_QUEUES.resolveModelAttribute(context, config).asBoolean());
        settings.setAutoDeleteQueues(AddressSettingDefinition.AUTO_DELETE_QUEUES.resolveModelAttribute(context, config).asBoolean());
        settings.setAutoCreateAddresses(AddressSettingDefinition.AUTO_CREATE_ADDRESSES.resolveModelAttribute(context, config).asBoolean());
        settings.setAutoDeleteAddresses(AddressSettingDefinition.AUTO_DELETE_ADDRESSES.resolveModelAttribute(context, config).asBoolean());
        return settings;
    }

    static SimpleString asSimpleString(final ModelNode node, final String defVal) {
        return SimpleString.toSimpleString(node.getType() != ModelType.UNDEFINED ? node.asString() : defVal);
    }

}
