/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.ActiveMQActivationService.ignoreOperationIfServerNotActive;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.ADDRESS_FULL_MESSAGE_POLICY;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.AUTO_CREATE_ADDRESSES;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.AUTO_CREATE_JMS_QUEUES;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.AUTO_CREATE_QUEUES;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.AUTO_DELETE_ADDRESSES;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.AUTO_DELETE_CREATED_QUEUES;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.AUTO_DELETE_JMS_QUEUES;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.AUTO_DELETE_QUEUES;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.EXPIRY_DELAY;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.LAST_VALUE_QUEUE;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.MAX_DELIVERY_ATTEMPTS;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.MAX_READ_PAGE_BYTES;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.MAX_REDELIVERY_DELAY;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.MAX_SIZE_BYTES;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.MESSAGE_COUNTER_HISTORY_DAY_LIMIT;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.PAGE_MAX_CACHE_SIZE;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.PAGE_SIZE_BYTES;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.REDELIVERY_DELAY;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.REDELIVERY_MULTIPLIER;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.REDISTRIBUTION_DELAY;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.SEND_TO_DLA_ON_NO_ROUTE;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.SLOW_CONSUMER_CHECK_PERIOD;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.SLOW_CONSUMER_POLICY;
import static org.wildfly.extension.messaging.activemq.AddressSettingDefinition.SLOW_CONSUMER_THRESHOLD;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.DEAD_LETTER_ADDRESS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.EXPIRY_ADDRESS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.RESOLVE_ADDRESS_SETTING;
import static org.wildfly.extension.messaging.activemq.OperationDefinitionHelper.createNonEmptyStringAttribute;

import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Operation handler to resolve address settings.
 *
 * WildFly address-setting resource represents a setting for a given address (which can be a wildcard address).
 * ActiveMQ uses a hierarchy of address-settings (based on address wildcards) and "merges" the hierarchy of settings
 * to obtain the settings for a specific address.
 *
 * This handler resolves the address settings values for the specified address (even though there may not be an
 * address-setting that exists at that address).
 *
 * For example, the user adds an address-settings for '#' (the most generic wildcard address) and specifies its settings.
 *
 * It can then call the operation /subsystem=messaging-activemq/server=default/resolve-address-setting(activemq-address=jms.queue.myQueue)
 * to retrieve the *resolved* settings for the jms.queue.myQueue address (which will be inherited from #).
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2014 Red Hat Inc.
 */
public class AddressSettingsResolveHandler extends AbstractRuntimeOnlyHandler {

    static final AddressSettingsResolveHandler INSTANCE = new AddressSettingsResolveHandler();

    private static final AttributeDefinition ACTIVEMQ_ADDRESS = createNonEmptyStringAttribute(CommonAttributes.ACTIVEMQ_ADDRESS);

    protected AddressSettingsResolveHandler() { }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (ignoreOperationIfServerNotActive(context, operation)) {
            return;
        }

        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(address);

        ServiceController<?> service = context.getServiceRegistry(false).getService(serviceName);
        ActiveMQServer server = ActiveMQServer.class.cast(ActiveMQBroker.class.cast(service.getValue()).getDelegate());

        final String activeMQAddress = ACTIVEMQ_ADDRESS.resolveModelAttribute(context, operation).asString();
        AddressSettings settings = server.getAddressSettingsRepository().getMatch(activeMQAddress);

        ModelNode result = context.getResult();

        result.get(ADDRESS_FULL_MESSAGE_POLICY.getName()).set(settings.getAddressFullMessagePolicy().toString());
        ModelNode deadLetterAddress = result.get(DEAD_LETTER_ADDRESS.getName());
        if (settings.getDeadLetterAddress() != null) {
            deadLetterAddress.set(settings.getDeadLetterAddress().toString());
        }
        ModelNode expiryAddress = result.get(EXPIRY_ADDRESS.getName());
        if (settings.getExpiryAddress() != null) {
            expiryAddress.set(settings.getExpiryAddress().toString());
        }
        result.get(EXPIRY_DELAY.getName()).set(settings.getExpiryDelay());
        result.get(LAST_VALUE_QUEUE.getName()).set(settings.isDefaultLastValueQueue());
        result.get(MAX_DELIVERY_ATTEMPTS.getName()).set(settings.getMaxDeliveryAttempts());
        result.get(MAX_READ_PAGE_BYTES.getName()).set(settings.getMaxReadPageBytes());
        result.get(MAX_REDELIVERY_DELAY.getName()).set(settings.getMaxRedeliveryDelay());
        result.get(MAX_SIZE_BYTES.getName()).set(settings.getMaxSizeBytes());
        result.get(MESSAGE_COUNTER_HISTORY_DAY_LIMIT.getName()).set(settings.getMessageCounterHistoryDayLimit());
        result.get(PAGE_MAX_CACHE_SIZE.getName()).set(settings.getPageCacheMaxSize());
        result.get(PAGE_SIZE_BYTES.getName()).set(settings.getPageSizeBytes());
        result.get(REDELIVERY_DELAY.getName()).set(settings.getRedeliveryDelay());
        result.get(REDELIVERY_MULTIPLIER.getName()).set(settings.getRedeliveryMultiplier());
        result.get(REDISTRIBUTION_DELAY.getName()).set(settings.getRedistributionDelay());
        result.get(SEND_TO_DLA_ON_NO_ROUTE.getName()).set(settings.isSendToDLAOnNoRoute());
        result.get(SLOW_CONSUMER_CHECK_PERIOD.getName()).set(settings.getSlowConsumerCheckPeriod());
        result.get(SLOW_CONSUMER_POLICY.getName()).set(settings.getSlowConsumerPolicy().toString());
        result.get(SLOW_CONSUMER_THRESHOLD.getName()).set(settings.getSlowConsumerThreshold());
        result.get(AUTO_CREATE_JMS_QUEUES.getName()).set(settings.isAutoCreateJmsQueues());
        result.get(AUTO_DELETE_JMS_QUEUES.getName()).set(settings.isAutoDeleteJmsQueues());
        result.get(AUTO_CREATE_ADDRESSES.getName()).set(settings.isAutoCreateAddresses());
        result.get(AUTO_DELETE_ADDRESSES.getName()).set(settings.isAutoDeleteAddresses());
        result.get(AUTO_CREATE_QUEUES.getName()).set(settings.isAutoCreateQueues());
        result.get(AUTO_DELETE_QUEUES.getName()).set(settings.isAutoDeleteQueues());
        result.get(AUTO_DELETE_CREATED_QUEUES.getName()).set(settings.isAutoDeleteCreatedQueues());
    }

    public static void registerOperationHandler(ManagementResourceRegistration registry, ResourceDescriptionResolver resolver) {
        SimpleOperationDefinition op = new SimpleOperationDefinitionBuilder(RESOLVE_ADDRESS_SETTING, resolver)
                .setReadOnly()
                .setRuntimeOnly()
                .addParameter(ACTIVEMQ_ADDRESS)
                .setReplyType(ModelType.LIST)
                .setReplyParameters(AddressSettingDefinition.ATTRIBUTES)
                .build();
        registry.registerOperationHandler(op, AddressSettingsResolveHandler.INSTANCE);
    }
}
