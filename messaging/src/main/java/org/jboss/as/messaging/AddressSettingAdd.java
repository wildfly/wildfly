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

package org.jboss.as.messaging;

import static org.jboss.as.messaging.CommonAttributes.DEAD_LETTER_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.EXPIRY_ADDRESS;

import java.util.List;

import org.hornetq.api.core.SimpleString;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.settings.impl.AddressFullMessagePolicy;
import org.hornetq.core.settings.impl.AddressSettings;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * {@code OperationStepHandler} adding a new address setting.
 *
 * @author Emanuel Muckenhuber
 */
class AddressSettingAdd extends AbstractAddStepHandler {

    static final OperationStepHandler INSTANCE = new AddressSettingAdd();

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for(final SimpleAttributeDefinition attribute : AddressSettingDefinition.ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler,
                                  final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final HornetQServer server = getServer(context, operation);
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
        final AddressFullMessagePolicy addressPolicy = AddressFullMessagePolicy.valueOf(AddressSettingDefinition.ADDRESS_FULL_MESSAGE_POLICY.resolveModelAttribute(context, config).asString());
        settings.setAddressFullMessagePolicy(addressPolicy);
        settings.setDeadLetterAddress(asSimpleString(DEAD_LETTER_ADDRESS.resolveModelAttribute(context, config), null));
        settings.setLastValueQueue(AddressSettingDefinition.LAST_VALUE_QUEUE.resolveModelAttribute(context, config).asBoolean());
        settings.setMaxDeliveryAttempts(AddressSettingDefinition.MAX_DELIVERY_ATTEMPTS.resolveModelAttribute(context, config).asInt());
        settings.setMaxSizeBytes(AddressSettingDefinition.MAX_SIZE_BYTES.resolveModelAttribute(context, config).asLong());
        settings.setMessageCounterHistoryDayLimit(AddressSettingDefinition.MESSAGE_COUNTER_HISTORY_DAY_LIMIT.resolveModelAttribute(context, config).asInt());
        settings.setExpiryAddress(asSimpleString(EXPIRY_ADDRESS.resolveModelAttribute(context, config), null));
        settings.setRedeliveryDelay(AddressSettingDefinition.REDELIVERY_DELAY.resolveModelAttribute(context, config).asLong());
        settings.setRedistributionDelay(AddressSettingDefinition.REDISTRIBUTION_DELAY.resolveModelAttribute(context, config).asLong());
        settings.setPageSizeBytes(AddressSettingDefinition.PAGE_SIZE_BYTES.resolveModelAttribute(context, config).asLong());
        settings.setPageCacheMaxSize(AddressSettingDefinition.PAGE_MAX_CACHE_SIZE.resolveModelAttribute(context, config).asInt());
        settings.setSendToDLAOnNoRoute(AddressSettingDefinition.SEND_TO_DLA_ON_NO_ROUTE.resolveModelAttribute(context, config).asBoolean());
        return settings;
    }

    static SimpleString asSimpleString(final ModelNode node, final String defVal) {
        return SimpleString.toSimpleString(node.getType() != ModelType.UNDEFINED ? node.asString() : defVal);
    }

    static HornetQServer getServer(final OperationContext context, ModelNode operation) {
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        final ServiceController<?> controller = context.getServiceRegistry(true).getService(hqServiceName);
        if(controller != null) {
            return HornetQServer.class.cast(controller.getValue());
        }
        return null;
    }
}
