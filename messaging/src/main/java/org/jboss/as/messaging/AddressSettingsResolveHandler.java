/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.AddressSettingDefinition.ADDRESS_FULL_MESSAGE_POLICY;
import static org.jboss.as.messaging.AddressSettingDefinition.EXPIRY_DELAY;
import static org.jboss.as.messaging.AddressSettingDefinition.LAST_VALUE_QUEUE;
import static org.jboss.as.messaging.AddressSettingDefinition.MAX_DELIVERY_ATTEMPTS;
import static org.jboss.as.messaging.AddressSettingDefinition.MAX_REDELIVERY_DELAY;
import static org.jboss.as.messaging.AddressSettingDefinition.MAX_SIZE_BYTES;
import static org.jboss.as.messaging.AddressSettingDefinition.MESSAGE_COUNTER_HISTORY_DAY_LIMIT;
import static org.jboss.as.messaging.AddressSettingDefinition.PAGE_MAX_CACHE_SIZE;
import static org.jboss.as.messaging.AddressSettingDefinition.PAGE_SIZE_BYTES;
import static org.jboss.as.messaging.AddressSettingDefinition.REDELIVERY_DELAY;
import static org.jboss.as.messaging.AddressSettingDefinition.REDELIVERY_MULTIPLIER;
import static org.jboss.as.messaging.AddressSettingDefinition.REDISTRIBUTION_DELAY;
import static org.jboss.as.messaging.AddressSettingDefinition.SEND_TO_DLA_ON_NO_ROUTE;
import static org.jboss.as.messaging.CommonAttributes.DEAD_LETTER_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.EXPIRY_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.RESOLVE_ADDRESS_SETTING;
import static org.jboss.as.messaging.HornetQActivationService.ignoreOperationIfServerNotActive;
import static org.jboss.as.messaging.OperationDefinitionHelper.createNonEmptyStringAttribute;

import java.util.EnumSet;

import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.settings.impl.AddressSettings;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Operation handler to resolve address settings.
 *
 * WildFly address-setting resource represents a setting for a given address (which can be a wildcard address).
 * HornetQ uses a hierarchy of address-settings (based on address wildcards) and "merges" the hierarchy of settings
 * to obtain the settings for a specific address.
 *
 * This handler resolves the address settings values for the specified address (even though there may not be an
 * address-setting that exists at that address).
 *
 * For example, the user adds an address-settings for '#' (the most generic wildcard address) and specifies its settings.
 *
 * It can then call the operation /subsystem=messaging/hornetq-server=default/resolve-address-setting(hornetq-address=jms.queue.myQueue)
 * to retrieve the *resolved* settings for the jms.queue.myQueue address (which will be inherited from #).
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2014 Red Hat Inc.
 */
public class AddressSettingsResolveHandler extends AbstractRuntimeOnlyHandler {

    static final AddressSettingsResolveHandler INSTANCE = new AddressSettingsResolveHandler();

    private static final AttributeDefinition HORNETQ_ADDRESS = createNonEmptyStringAttribute(CommonAttributes.HORNETQ_ADDRESS);

    protected AddressSettingsResolveHandler() { }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (ignoreOperationIfServerNotActive(context, operation)) {
            return;
        }

        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(address);

        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        HornetQServer hqServer = HornetQServer.class.cast(hqService.getValue());

        final String hornetqAddress = HORNETQ_ADDRESS.resolveModelAttribute(context, operation).asString();
        AddressSettings settings = hqServer.getAddressSettingsRepository().getMatch(hornetqAddress);

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
        result.get(LAST_VALUE_QUEUE.getName()).set(settings.isLastValueQueue());
        result.get(MAX_DELIVERY_ATTEMPTS.getName()).set(settings.getMaxDeliveryAttempts());
        result.get(MAX_REDELIVERY_DELAY.getName()).set(settings.getMaxRedeliveryDelay());
        result.get(MAX_SIZE_BYTES.getName()).set(settings.getMaxSizeBytes());
        result.get(MESSAGE_COUNTER_HISTORY_DAY_LIMIT.getName()).set(settings.getMessageCounterHistoryDayLimit());
        result.get(PAGE_MAX_CACHE_SIZE.getName()).set(settings.getPageCacheMaxSize());
        result.get(PAGE_SIZE_BYTES.getName()).set(settings.getPageSizeBytes());
        result.get(REDELIVERY_DELAY.getName()).set(settings.getRedeliveryDelay());
        result.get(REDELIVERY_MULTIPLIER.getName()).set(settings.getRedeliveryMultiplier());
        result.get(REDISTRIBUTION_DELAY.getName()).set(settings.getRedistributionDelay());
        result.get(SEND_TO_DLA_ON_NO_ROUTE.getName()).set(settings.isSendToDLAOnNoRoute());

        context.stepCompleted();
    }

    public static void registerOperationHandler(ManagementResourceRegistration registry, ResourceDescriptionResolver resolver) {
        SimpleOperationDefinition op = new SimpleOperationDefinitionBuilder(RESOLVE_ADDRESS_SETTING, resolver)
                .withFlags(EnumSet.of(OperationEntry.Flag.READ_ONLY, OperationEntry.Flag.RUNTIME_ONLY))
                .addParameter(HORNETQ_ADDRESS)
                .setReplyType(ModelType.LIST)
                .setReplyParameters(AddressSettingDefinition.ATTRIBUTES)
                .build();
        registry.registerOperationHandler(op, AddressSettingsResolveHandler.INSTANCE);
    }
}
