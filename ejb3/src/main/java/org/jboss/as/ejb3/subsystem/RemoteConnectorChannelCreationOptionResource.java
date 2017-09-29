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
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RestartParentResourceAddHandler;
import org.jboss.as.controller.RestartParentResourceRemoveHandler;
import org.jboss.as.controller.RestartParentWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ejb3.remote.EJBRemoteConnectorService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;


/**
 * {@link SimpleResourceDefinition} for the channel creation option(s) that are configured for the EJB remote
 * channel
 *
 * @author Jaikiran Pai
 */
class RemoteConnectorChannelCreationOptionResource extends SimpleResourceDefinition {

    static final RemoteConnectorChannelCreationOptionResource INSTANCE = new RemoteConnectorChannelCreationOptionResource();

    /**
     * Attribute definition of the channel creation option "value"
     */
    static final SimpleAttributeDefinition CHANNEL_CREATION_OPTION_VALUE = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.VALUE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    /**
     * Attribute definition of the channel creation option "type"
     */
    static final SimpleAttributeDefinition CHANNEL_CREATION_OPTION_TYPE = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.TYPE, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).setValidator(AllowedChannelOptionTypesValidator.INSTANCE).build();

    RemoteConnectorChannelCreationOptionResource() {
        super(PathElement.pathElement(EJB3SubsystemModel.CHANNEL_CREATION_OPTIONS),
                EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.CHANNEL_CREATION_OPTIONS),
                new ChannelCreationOptionAdd(),
                new ChannelCreationOptionRemove());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(CHANNEL_CREATION_OPTION_VALUE, null, new ChannelCreationOptionWriteAttributeHandler(CHANNEL_CREATION_OPTION_VALUE));
        resourceRegistration.registerReadWriteAttribute(CHANNEL_CREATION_OPTION_TYPE, null, new ChannelCreationOptionWriteAttributeHandler(CHANNEL_CREATION_OPTION_TYPE));
    }

    private static void recreateParentService(OperationContext context, ModelNode ejb3RemoteServiceModelNode) throws OperationFailedException {
        EJB3RemoteServiceAdd.INSTANCE.installRuntimeServices(context, ejb3RemoteServiceModelNode);
    }

    /**
     * A write handler for the "value" attribute of the channel creation option
     */
    private static class ChannelCreationOptionWriteAttributeHandler extends RestartParentWriteAttributeHandler {

        ChannelCreationOptionWriteAttributeHandler(final AttributeDefinition attributeDefinition) {
            super(EJB3SubsystemModel.SERVICE, attributeDefinition);
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
            RemoteConnectorChannelCreationOptionResource.recreateParentService(context, parentModel);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return EJBRemoteConnectorService.SERVICE_NAME;
        }
    }

    /**
     * An add handler for channel creation option
     */
    private static class ChannelCreationOptionAdd extends RestartParentResourceAddHandler {

        private ChannelCreationOptionAdd() {
            super(EJB3SubsystemModel.SERVICE);
        }

        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            RemoteConnectorChannelCreationOptionResource.CHANNEL_CREATION_OPTION_VALUE.validateAndSet(operation, model);
            RemoteConnectorChannelCreationOptionResource.CHANNEL_CREATION_OPTION_TYPE.validateAndSet(operation, model);
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
            RemoteConnectorChannelCreationOptionResource.recreateParentService(context, parentModel);
        }

        @Override
        protected PathAddress getParentAddress(PathAddress address) {
            for (int i = address.size() - 1; i >= 0; i--) {
                PathElement pe = address.getElement(i);
                if (pe.getKey().equals("service") && pe.getValue().equals("remote")) {
                    return address.subAddress(0, i + 1);
                }
            }
            return null;
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return EJBRemoteConnectorService.SERVICE_NAME;
        }
    }

    /**
     * Remove handler for channel creation option
     */
    private static class ChannelCreationOptionRemove extends RestartParentResourceRemoveHandler {
        private ChannelCreationOptionRemove() {
            super(EJB3SubsystemModel.SERVICE);
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode ejb3RemoteServiceModelNode) throws OperationFailedException {
            RemoteConnectorChannelCreationOptionResource.recreateParentService(context, ejb3RemoteServiceModelNode);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return EJBRemoteConnectorService.SERVICE_NAME;
        }
    }

}
