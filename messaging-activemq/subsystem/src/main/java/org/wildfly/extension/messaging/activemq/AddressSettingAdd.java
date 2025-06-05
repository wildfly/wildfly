/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

        context.addStep(AddressSettingsValidator.ADD_VALIDATOR, OperationContext.Stage.MODEL, false);
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.isDefaultRequiresRuntime() && !context.isBooting();
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final ActiveMQServer server = getActiveMQServer(context, operation);
        if (server != null) {
            boolean isRootAddressMatch = server.getConfiguration().getWildcardConfiguration().getAnyWordsString().equals(context.getCurrentAddressValue());
            final AddressSettings settings = createSettings(context, model, isRootAddressMatch);
            if (isRootAddressMatch) {
                settings.merge(createDefaultAddressSettings());
            }
            server.getAddressSettingsRepository().addMatch(context.getCurrentAddressValue(), settings);
        }
    }

    /**
     * Create a setting.
     *
     * @param context the operation context
     * @param config  the detyped config
     * @return the address settings
     * @throws OperationFailedException if the model is invalid
     */
    static AddressSettings createSettings(final OperationContext context, final ModelNode config, boolean isRootAddressMatch) throws OperationFailedException {
        final AddressSettings settings = new AddressSettings();
        if (config.hasDefined(AddressSettingDefinition.ADDRESS_FULL_MESSAGE_POLICY.getName())) {
            final AddressFullMessagePolicy addressPolicy = AddressFullMessagePolicy.valueOf(AddressSettingDefinition.ADDRESS_FULL_MESSAGE_POLICY.resolveModelAttribute(context, config).asString());
            settings.setAddressFullMessagePolicy(addressPolicy);
        }
        // always set the auto-create|delete-jms-queues attributes as their default attribute values differ from Artemis defaults.
        settings.setAutoCreateJmsQueues(AddressSettingDefinition.AUTO_CREATE_JMS_QUEUES.resolveModelAttribute(context, config).asBoolean());
        settings.setAutoDeleteJmsQueues(AddressSettingDefinition.AUTO_DELETE_JMS_QUEUES.resolveModelAttribute(context, config).asBoolean());
        settings.setAutoCreateQueues(AddressSettingDefinition.AUTO_CREATE_QUEUES.resolveModelAttribute(context, config).asBoolean());
        settings.setAutoDeleteQueues(AddressSettingDefinition.AUTO_DELETE_QUEUES.resolveModelAttribute(context, config).asBoolean());

        // Internally AddressSettings use the null values for attributes as special value, for example when merging with a different config
        //  Thus we have to check if the value is defined before setting it (We do not want to set default value here)
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.AUTO_CREATE_ADDRESSES.getName())) {
            settings.setAutoCreateAddresses(AddressSettingDefinition.AUTO_CREATE_ADDRESSES.resolveModelAttribute(context, config).asBoolean());
        }
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.AUTO_DELETE_ADDRESSES.getName())) {
            settings.setAutoDeleteAddresses(AddressSettingDefinition.AUTO_DELETE_ADDRESSES.resolveModelAttribute(context, config).asBoolean());
        }
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.AUTO_DELETE_CREATED_QUEUES.getName())) {
            settings.setAutoDeleteCreatedQueues(AddressSettingDefinition.AUTO_DELETE_CREATED_QUEUES.resolveModelAttribute(context, config).asBoolean());
        }
        if (isRootAddressMatch || config.hasDefined(DEAD_LETTER_ADDRESS.getName())) {
            settings.setDeadLetterAddress(asSimpleString(DEAD_LETTER_ADDRESS.resolveModelAttribute(context, config), null));
        }
        if (isRootAddressMatch || config.hasDefined(CommonAttributes.EXPIRY_ADDRESS.getName())) {
            settings.setExpiryAddress(asSimpleString(EXPIRY_ADDRESS.resolveModelAttribute(context, config), null));
        }
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.EXPIRY_DELAY.getName())) {
            settings.setExpiryDelay(AddressSettingDefinition.EXPIRY_DELAY.resolveModelAttribute(context, config).asLong());
        }
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.LAST_VALUE_QUEUE.getName())) {
            settings.setDefaultLastValueQueue(AddressSettingDefinition.LAST_VALUE_QUEUE.resolveModelAttribute(context, config).asBoolean());
        }
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.MAX_DELIVERY_ATTEMPTS.getName())) {
            settings.setMaxDeliveryAttempts(AddressSettingDefinition.MAX_DELIVERY_ATTEMPTS.resolveModelAttribute(context, config).asInt());
        }
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.MAX_SIZE_BYTES.getName())) {
            settings.setMaxSizeBytes(AddressSettingDefinition.MAX_SIZE_BYTES.resolveModelAttribute(context, config).asLong());
        }
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.MESSAGE_COUNTER_HISTORY_DAY_LIMIT.getName())) {
            settings.setMessageCounterHistoryDayLimit(AddressSettingDefinition.MESSAGE_COUNTER_HISTORY_DAY_LIMIT.resolveModelAttribute(context, config).asInt());
        }
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.PAGE_MAX_CACHE_SIZE.getName())) {
            settings.setPageCacheMaxSize(AddressSettingDefinition.PAGE_MAX_CACHE_SIZE.resolveModelAttribute(context, config).asInt());
        }
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.PAGE_SIZE_BYTES.getName())) {
            settings.setPageSizeBytes(AddressSettingDefinition.PAGE_SIZE_BYTES.resolveModelAttribute(context, config).asInt());
        }
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.REDELIVERY_DELAY.getName())) {
            settings.setRedeliveryDelay(AddressSettingDefinition.REDELIVERY_DELAY.resolveModelAttribute(context, config).asLong());
        }
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.REDELIVERY_MULTIPLIER.getName())) {
            settings.setRedeliveryMultiplier(AddressSettingDefinition.REDELIVERY_MULTIPLIER.resolveModelAttribute(context, config).asDouble());
        }
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.REDISTRIBUTION_DELAY.getName())) {
            settings.setRedistributionDelay(AddressSettingDefinition.REDISTRIBUTION_DELAY.resolveModelAttribute(context, config).asLong());
        }
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.SEND_TO_DLA_ON_NO_ROUTE.getName())) {
            settings.setSendToDLAOnNoRoute(AddressSettingDefinition.SEND_TO_DLA_ON_NO_ROUTE.resolveModelAttribute(context, config).asBoolean());
        }
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.SLOW_CONSUMER_CHECK_PERIOD.getName())) {
            settings.setSlowConsumerCheckPeriod(AddressSettingDefinition.SLOW_CONSUMER_CHECK_PERIOD.resolveModelAttribute(context, config).asLong());
        }
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.SLOW_CONSUMER_POLICY.getName())) {
            final SlowConsumerPolicy slowConsumerPolicy = SlowConsumerPolicy.valueOf(AddressSettingDefinition.SLOW_CONSUMER_POLICY.resolveModelAttribute(context, config).asString());
            settings.setSlowConsumerPolicy(slowConsumerPolicy);
        }
        if (isRootAddressMatch || config.hasDefined(AddressSettingDefinition.SLOW_CONSUMER_THRESHOLD.getName())) {
            settings.setSlowConsumerThreshold(AddressSettingDefinition.SLOW_CONSUMER_THRESHOLD.resolveModelAttribute(context, config).asLong());
        }
        // these do not have default values
        if (config.hasDefined(AddressSettingDefinition.MAX_READ_PAGE_BYTES.getName())) {
            settings.setMaxReadPageBytes(AddressSettingDefinition.MAX_READ_PAGE_BYTES.resolveModelAttribute(context, config).asInt());
        }
        if (config.hasDefined(AddressSettingDefinition.MAX_REDELIVERY_DELAY.getName())) {
            settings.setMaxRedeliveryDelay(AddressSettingDefinition.MAX_REDELIVERY_DELAY.resolveModelAttribute(context, config).asLong());
        }
        return settings;
    }

    static AddressSettings createDefaultAddressSettings() {
        final AddressSettings settings = new AddressSettings();
        settings.setAddressFullMessagePolicy(AddressFullMessagePolicy.valueOf(AddressSettingDefinition.ADDRESS_FULL_MESSAGE_POLICY.getDefaultValue().asString()));

        // always set the auto-create|delete-jms-queues attributes as their default attribute values differ from Artemis defaults.
        settings.setAutoCreateJmsQueues(AddressSettingDefinition.AUTO_CREATE_JMS_QUEUES.getDefaultValue().asBoolean());
        settings.setAutoDeleteJmsQueues(AddressSettingDefinition.AUTO_DELETE_JMS_QUEUES.getDefaultValue().asBoolean());
        settings.setAutoCreateQueues(AddressSettingDefinition.AUTO_CREATE_QUEUES.getDefaultValue().asBoolean());
        settings.setAutoDeleteQueues(AddressSettingDefinition.AUTO_DELETE_QUEUES.getDefaultValue().asBoolean());
        settings.setAutoCreateAddresses(AddressSettingDefinition.AUTO_CREATE_ADDRESSES.getDefaultValue().asBoolean());
        settings.setAutoDeleteAddresses(AddressSettingDefinition.AUTO_DELETE_ADDRESSES.getDefaultValue().asBoolean());
        settings.setAutoDeleteCreatedQueues(AddressSettingDefinition.AUTO_DELETE_CREATED_QUEUES.getDefaultValue().asBoolean());
        settings.setDeadLetterAddress(asSimpleString(DEAD_LETTER_ADDRESS.getDefaultValue(), null));
        settings.setExpiryAddress(asSimpleString(EXPIRY_ADDRESS.getDefaultValue(), null));
        settings.setExpiryDelay(AddressSettingDefinition.EXPIRY_DELAY.getDefaultValue().asLong());
        settings.setDefaultLastValueQueue(AddressSettingDefinition.LAST_VALUE_QUEUE.getDefaultValue().asBoolean());
        settings.setMaxDeliveryAttempts(AddressSettingDefinition.MAX_DELIVERY_ATTEMPTS.getDefaultValue().asInt());
        settings.setMaxSizeBytes(AddressSettingDefinition.MAX_SIZE_BYTES.getDefaultValue().asLong());
        settings.setMessageCounterHistoryDayLimit(AddressSettingDefinition.MESSAGE_COUNTER_HISTORY_DAY_LIMIT.getDefaultValue().asInt());
        settings.setPageCacheMaxSize(AddressSettingDefinition.PAGE_MAX_CACHE_SIZE.getDefaultValue().asInt());
        settings.setPageSizeBytes(AddressSettingDefinition.PAGE_SIZE_BYTES.getDefaultValue().asInt());
        settings.setRedeliveryDelay(AddressSettingDefinition.REDELIVERY_DELAY.getDefaultValue().asLong());
        settings.setRedeliveryMultiplier(AddressSettingDefinition.REDELIVERY_MULTIPLIER.getDefaultValue().asDouble());
        settings.setRedistributionDelay(AddressSettingDefinition.REDISTRIBUTION_DELAY.getDefaultValue().asLong());
        settings.setSendToDLAOnNoRoute(AddressSettingDefinition.SEND_TO_DLA_ON_NO_ROUTE.getDefaultValue().asBoolean());
        settings.setSlowConsumerCheckPeriod(AddressSettingDefinition.SLOW_CONSUMER_CHECK_PERIOD.getDefaultValue().asLong());
        settings.setSlowConsumerPolicy(SlowConsumerPolicy.valueOf(AddressSettingDefinition.SLOW_CONSUMER_POLICY.getDefaultValue().asString()));
        settings.setSlowConsumerThreshold(AddressSettingDefinition.SLOW_CONSUMER_THRESHOLD.getDefaultValue().asLong());
        return settings;
    }

    static SimpleString asSimpleString(final ModelNode node, final String defVal) {
        return SimpleString.toSimpleString(node != null && node.getType() != ModelType.UNDEFINED ? node.asString() : defVal);
    }

}
