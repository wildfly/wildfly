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

package org.wildfly.extension.messaging.activemq.ha;

import static org.jboss.as.controller.OperationContext.Stage.MODEL;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HA_POLICY;

import java.util.Collection;

import org.apache.activemq.artemis.core.config.HAPolicyConfiguration;
import org.apache.activemq.artemis.core.config.ScaleDownConfiguration;
import org.apache.activemq.artemis.core.config.ha.LiveOnlyPolicyConfiguration;
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

    public static final LiveOnlyDefinition INSTANCE = new LiveOnlyDefinition();

    private LiveOnlyDefinition() {
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

    static HAPolicyConfiguration buildConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        ScaleDownConfiguration scaleDownConfiguration = ScaleDownAttributes.addScaleDownConfiguration(context, model);
        return new LiveOnlyPolicyConfiguration(scaleDownConfiguration);
    }
}