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

import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.EnumSet;

import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.settings.HierarchicalRepository;
import org.hornetq.core.settings.impl.AddressSettings;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class AddressSettingsWriteHandler implements OperationStepHandler {

    static final AddressSettingsWriteHandler INSTANCE = new AddressSettingsWriteHandler();


    public void registerAttributes(final ManagementResourceRegistration registry, boolean registerRuntimeOnly) {
        final EnumSet<AttributeAccess.Flag> flags = EnumSet.of(AttributeAccess.Flag.RESTART_NONE);
        for (AttributeDefinition attr : AddressSettingAdd.ATTRIBUTES) {
            if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr.getName(), null, this, flags);
            }
        }
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final String attribute = operation.require(ModelDescriptionConstants.NAME).asString();
        final AttributeDefinition def = getAttributeDefinition(attribute);
        if(def == null) {
            context.getFailureDescription().set(new ModelNode().set(MESSAGES.unknownAttribute(attribute)));
        } else {
            def.getValidator().validateParameter(ModelDescriptionConstants.VALUE, operation);
            resource.getModel().get(attribute).set(operation.get(ModelDescriptionConstants.VALUE));

            if(context.getType() == OperationContext.Type.SERVER) {
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        final HornetQServer server = AddressSettingAdd.getServer(context, operation);
                        PathAddress address = null;
                        HierarchicalRepository<AddressSettings> repository = null;
                        AddressSettings existingSettings = null;
                        if(server != null) {
                            final ModelNode model = resource.getModel();
                            address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
                            final AddressSettings settings = AddressSettingAdd.createSettings(context, model);
                            repository = server.getAddressSettingsRepository();
                            String match = address.getLastElement().getValue();
                            existingSettings = repository.getMatch(match);
                            repository.addMatch(match, settings);
                        }

                        if (context.completeStep() != OperationContext.ResultAction.KEEP && existingSettings != null) {
                            // Restore the old settings
                            repository.addMatch(address.getLastElement().getValue(), existingSettings);
                        }
                    }
                }, OperationContext.Stage.RUNTIME);
            }
        }
        context.completeStep();
    }

    static AttributeDefinition getAttributeDefinition(final String attributeName) {
        for(final AttributeDefinition def : AddressSettingAdd.ATTRIBUTES) {
            if(def.getName().equals(attributeName)) {
                return def;
            }
        }
        return null;
    }

}
