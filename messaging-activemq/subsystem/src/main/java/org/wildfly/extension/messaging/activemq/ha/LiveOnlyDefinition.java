/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.ha;

import static org.jboss.as.controller.OperationContext.Stage.MODEL;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HA_POLICY;

import java.util.Collection;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.ActiveMQReloadRequiredHandlers;
import org.wildfly.extension.messaging.activemq.MessagingExtension;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class LiveOnlyDefinition extends PersistentResourceDefinition {

    private static final Collection<AttributeDefinition> ATTRIBUTES = ScaleDownAttributes.SCALE_DOWN_ATTRIBUTES;

    private static final AbstractAddStepHandler ADD  = new ActiveMQReloadRequiredHandlers.AddStepHandler(ATTRIBUTES) {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            super.execute(context, operation);
            context.addStep(ManagementHelper.checkNoOtherSibling(HA_POLICY), MODEL);
        }

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            super.populateModel(operation, model);
        }
    };

    private static final AbstractWriteAttributeHandler WRITE_ATTRIBUTE = new ActiveMQReloadRequiredHandlers.WriteAttributeHandler(ATTRIBUTES);

    public LiveOnlyDefinition() {
        super(MessagingExtension.LIVE_ONLY_PATH,
                MessagingExtension.getResourceDescriptionResolver(HA_POLICY),
                ADD,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attribute : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, WRITE_ATTRIBUTE);
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

}