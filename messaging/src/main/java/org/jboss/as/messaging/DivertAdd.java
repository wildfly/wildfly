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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;
import java.util.Locale;

import org.hornetq.api.core.management.HornetQServerControl;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.DivertConfiguration;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.connector.subsystems.datasources.Util;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.messaging.jms.JMSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Handler for adding a divert.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DivertAdd extends AbstractAddStepHandler implements DescriptionProvider {

    /**
     * Create an "add" operation using the existing model
     */
    public static ModelNode getAddOperation(final ModelNode address, ModelNode subModel) {

        final ModelNode operation = org.jboss.as.controller.operations.common.Util.getOperation(ADD, address, subModel);

        return operation;
    }

    private final Configuration configuration;

    public DivertAdd(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        model.setEmptyObject();

        for (final AttributeDefinition attributeDefinition : CommonAttributes.DIVERT_ATTRIBUTES) {
            attributeDefinition.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        final String name = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final ModelNode routingNode = CommonAttributes.ROUTING_NAME.validateResolvedOperation(model);
        final String routingName = routingNode.isDefined() ? routingNode.asString() : null;
        final String address = CommonAttributes.DIVERT_ADDRESS.validateResolvedOperation(model).asString();
        final String forwardingAddress = CommonAttributes.FORWARDING_ADDRESS.validateResolvedOperation(model).asString();
        final boolean exclusive = CommonAttributes.EXCLUSIVE.validateResolvedOperation(model).asBoolean();
        final ModelNode filterNode = CommonAttributes.FILTER.validateResolvedOperation(model);
        final String filter = filterNode.isDefined() ? filterNode.asString() : null;
        final ModelNode transformerNode =  CommonAttributes.TRANSFORMER_CLASS_NAME.validateResolvedOperation(model);
        final String transformerClassName = transformerNode.isDefined() ? transformerNode.asString() : null;

        ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceController<?> hqService = registry.getService(MessagingServices.JBOSS_MESSAGING);
        if (hqService != null) {

            // The original subsystem initialization is complete; use the control object to create the divert
            if (hqService.getState() != ServiceController.State.UP) {
                throw new IllegalStateException(String.format("Service %s is not in state %s, it is in state %s",
                        MessagingServices.JBOSS_MESSAGING, ServiceController.State.UP, hqService.getState()));
            }

            HornetQServerControl serverControl = HornetQServer.class.cast(hqService.getValue()).getHornetQServerControl();
            try {
                serverControl.createDivert(name, routingName, address, forwardingAddress, exclusive, filter, transformerClassName);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                // TODO should this be an OFE instead?
                throw new RuntimeException(e);
            }

        } else {
            // The initial subsystem install is not complete; just add our bit to the overall configuration
            List<DivertConfiguration> divertConfigs = configuration.getDivertConfigurations();
            DivertConfiguration divertConfig = new DivertConfiguration(name, routingName, address, forwardingAddress, exclusive, filter, transformerClassName);
            divertConfigs.add(divertConfig);
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return MessagingDescriptions.getDivertAdd(locale);
    }
}
