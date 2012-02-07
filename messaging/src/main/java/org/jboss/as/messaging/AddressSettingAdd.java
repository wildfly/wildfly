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

import static org.jboss.as.messaging.CommonAttributes.ADDRESS_FULL_MESSAGE_POLICY;
import static org.jboss.as.messaging.CommonAttributes.DEAD_LETTER_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.EXPIRY_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.LVQ;
import static org.jboss.as.messaging.CommonAttributes.MAX_DELIVERY_ATTEMPTS;
import static org.jboss.as.messaging.CommonAttributes.MAX_SIZE_BYTES_NODE_NAME;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNTER_HISTORY_DAY_LIMIT;
import static org.jboss.as.messaging.CommonAttributes.PAGE_MAX_CACHE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.PAGE_SIZE_BYTES_NODE_NAME;
import static org.jboss.as.messaging.CommonAttributes.REDELIVERY_DELAY;
import static org.jboss.as.messaging.CommonAttributes.REDISTRIBUTION_DELAY;
import static org.jboss.as.messaging.CommonAttributes.SEND_TO_DLA_ON_NO_ROUTE;

import java.util.List;
import java.util.Locale;

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
import org.jboss.as.controller.descriptions.DescriptionProvider;
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
class AddressSettingAdd extends AbstractAddStepHandler implements DescriptionProvider {

    static final OperationStepHandler INSTANCE = new AddressSettingAdd();

    static final SimpleAttributeDefinition[] ATTRIBUTES = new SimpleAttributeDefinition[] { ADDRESS_FULL_MESSAGE_POLICY,
                                     DEAD_LETTER_ADDRESS, LVQ, MAX_DELIVERY_ATTEMPTS, MAX_SIZE_BYTES_NODE_NAME,
                                     MESSAGE_COUNTER_HISTORY_DAY_LIMIT, EXPIRY_ADDRESS, REDELIVERY_DELAY,
                                     REDISTRIBUTION_DELAY, PAGE_MAX_CACHE_SIZE, PAGE_SIZE_BYTES_NODE_NAME, SEND_TO_DLA_ON_NO_ROUTE } ;

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for(final SimpleAttributeDefinition attribute : ATTRIBUTES) {
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
     * Create the add operation based on an existing model.
     *
     * @param address the address
     * @param subModel the sub model
     * @return the add operation
     */
    static ModelNode createAddOperation(final ModelNode address, final ModelNode subModel) {
        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        for(final SimpleAttributeDefinition definition : ATTRIBUTES) {
            final String attribute = definition.getName();
            if(subModel.hasDefined(attribute)) {
                operation.get(attribute).set(subModel.get(attribute));
            }
        }
        return operation;
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
        final AddressFullMessagePolicy addressPolicy = AddressFullMessagePolicy.valueOf(ADDRESS_FULL_MESSAGE_POLICY.resolveModelAttribute(context, config).asString());
        settings.setAddressFullMessagePolicy(addressPolicy);
        settings.setDeadLetterAddress(asSimpleString(DEAD_LETTER_ADDRESS.resolveModelAttribute(context, config), null));
        settings.setLastValueQueue(LVQ.resolveModelAttribute(context, config).asBoolean());
        settings.setMaxDeliveryAttempts(MAX_DELIVERY_ATTEMPTS.resolveModelAttribute(context, config).asInt());
        settings.setMaxSizeBytes(MAX_SIZE_BYTES_NODE_NAME.resolveModelAttribute(context, config).asInt());
        settings.setMessageCounterHistoryDayLimit(MESSAGE_COUNTER_HISTORY_DAY_LIMIT.resolveModelAttribute(context, config).asInt());
        settings.setExpiryAddress(asSimpleString(EXPIRY_ADDRESS.resolveModelAttribute(context, config), null));
        settings.setRedeliveryDelay(REDELIVERY_DELAY.resolveModelAttribute(context, config).asInt());
        settings.setRedistributionDelay(REDISTRIBUTION_DELAY.resolveModelAttribute(context, config).asLong());
        settings.setPageSizeBytes(PAGE_SIZE_BYTES_NODE_NAME.resolveModelAttribute(context, config).asLong());
        settings.setPageCacheMaxSize(PAGE_MAX_CACHE_SIZE.resolveModelAttribute(context, config).asInt());
        settings.setSendToDLAOnNoRoute(SEND_TO_DLA_ON_NO_ROUTE.resolveModelAttribute(context, config).asBoolean());
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

    @Override
    public ModelNode getModelDescription(final Locale locale) {
        return MessagingDescriptions.getAddressSettingAdd(locale);
    }
}
