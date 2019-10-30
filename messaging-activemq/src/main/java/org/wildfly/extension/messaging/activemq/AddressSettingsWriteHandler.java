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

import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.settings.HierarchicalRepository;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class AddressSettingsWriteHandler extends AbstractWriteAttributeHandler<AddressSettingsWriteHandler.RevertHandback> {

    static final AddressSettingsWriteHandler INSTANCE = new AddressSettingsWriteHandler();

    protected AddressSettingsWriteHandler() {
        super(AddressSettingDefinition.ATTRIBUTES);
    }


    @Override
    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue, ModelNode oldValue, Resource model) throws OperationFailedException {
        super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);

        AddressSettingsValidator.validateModel(context, operation, model);
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode resolvedValue,
                                           final ModelNode currentValue, final HandbackHolder<RevertHandback> handbackHolder) throws OperationFailedException {
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        final ActiveMQServer server = getActiveMQServer(context, operation);
        if(server != null) {
            final ModelNode model = resource.getModel();
            final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
            final AddressSettings settings = AddressSettingAdd.createSettings(context, model);
            final HierarchicalRepository<AddressSettings> repository = server.getAddressSettingsRepository();
            final String match = address.getLastElement().getValue();
            final AddressSettings existingSettings = repository.getMatch(match);
            repository.addMatch(match, settings);
            if(existingSettings != null) {
                handbackHolder.setHandback(new RevertHandback() {
                    @Override
                    public void doRevertUpdateToRuntime() {
                        // Restore the old settings
                        repository.addMatch(address.getLastElement().getValue(), existingSettings);
                    }
                });
            }
        }
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode valueToRestore,
                                         final ModelNode valueToRevert, final RevertHandback handback) throws OperationFailedException {
        if(handback != null) {
            handback.doRevertUpdateToRuntime();
        }
    }

    interface RevertHandback {
        void doRevertUpdateToRuntime();
    }

}
