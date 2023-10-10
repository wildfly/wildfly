/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
            boolean isRootAddressMatch = server.getConfiguration().getWildcardConfiguration().getAnyWordsString().equals(context.getCurrentAddressValue());
            final AddressSettings settings = AddressSettingAdd.createSettings(context, model, isRootAddressMatch);
            final HierarchicalRepository<AddressSettings> repository = server.getAddressSettingsRepository();
            final String match = context.getCurrentAddressValue();
            final AddressSettings existingSettings = repository.getMatch(match);
            repository.addMatch(match, settings);
            if(existingSettings != null) {
                handbackHolder.setHandback(new RevertHandback() {
                    @Override
                    public void doRevertUpdateToRuntime() {
                        // Restore the old settings
                        repository.addMatch(match, existingSettings);
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
