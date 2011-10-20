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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hornetq.core.config.BroadcastGroupConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.ConnectorServiceConfiguration;
import org.jboss.as.connector.subsystems.jca.ParamsUtils;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Handler for adding a connector service.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ConnectorServiceAdd extends AbstractAddStepHandler implements DescriptionProvider {

    /**
     * Create an "add" operation using the existing model
     */
    public static ModelNode getAddOperation(final ModelNode address, ModelNode subModel) {

        final ModelNode operation = org.jboss.as.controller.operations.common.Util.getEmptyOperation(ADD, address);
        for (AttributeDefinition attr : CommonAttributes.CONNECTOR_SERVICE_ATTRIBUTES) {
            if (subModel.hasDefined(attr.getName())) {
                operation.get(attr.getName()).set(subModel.get(attr.getName()));
            }
        }
        return operation;
    }

    public static final ConnectorServiceAdd INSTANCE = new ConnectorServiceAdd();

    private ConnectorServiceAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        model.setEmptyObject();

        for (final AttributeDefinition attributeDefinition : CommonAttributes.CONNECTOR_SERVICE_ATTRIBUTES) {
            attributeDefinition.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        ServiceRegistry registry = context.getServiceRegistry(false);
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> hqService = registry.getService(hqServiceName);
        if (hqService != null) {
            context.reloadRequired();
        }
        // else MessagingSubsystemAdd will add a handler that calls addConnectorServiceConfigs
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return MessagingDescriptions.getConnectorServiceAdd(locale);
    }

    static void addConnectorServiceConfigs(final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.CONNECTOR_SERVICE)) {
            final List<ConnectorServiceConfiguration> configs = configuration.getConnectorServiceConfigurations();
            for (Property prop : model.get(CommonAttributes.CONNECTOR_SERVICE).asPropertyList()) {
                configs.add(createConnectorServiceConfiguration(prop.getName(), prop.getValue()));
            }
        }
    }

    static ConnectorServiceConfiguration createConnectorServiceConfiguration(final String name, final ModelNode model) throws OperationFailedException {

        final String factoryClass = CommonAttributes.FACTORY_CLASS.validateResolvedOperation(model).asString();
        final Map<String, Object> params = new HashMap<String, Object>();
        if (model.hasDefined(CommonAttributes.PARAM)) {
            for (Property property : model.get(CommonAttributes.PARAM).asPropertyList()) {
                params.put(property.getName(), property.getValue().asString());
            }
        }

        return new ConnectorServiceConfiguration(factoryClass, params, name);
    }
}
