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

package org.jboss.as.messaging.ha;

import static org.jboss.as.controller.OperationContext.Stage.MODEL;
import static org.jboss.as.messaging.AlternativeAttributeCheckHandler.checkAlternatives;
import static org.jboss.as.messaging.CommonAttributes.HA_POLICY;
import static org.jboss.as.messaging.CommonAttributes.LIVE_ONLY;
import static org.jboss.as.messaging.ha.ScaleDownAttributes.SCALE_DOWN_CONNECTORS;
import static org.jboss.as.messaging.ha.ScaleDownAttributes.SCALE_DOWN_DISCOVERY_GROUP_NAME;

import java.util.Collection;

import org.hornetq.core.config.HAPolicyConfiguration;
import org.hornetq.core.config.ScaleDownConfiguration;
import org.hornetq.core.config.ha.LiveOnlyPolicyConfiguration;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.messaging.AlternativeAttributeCheckHandler;
import org.jboss.as.messaging.HornetQReloadRequiredHandlers;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class LiveOnlyDefinition extends PersistentResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(HA_POLICY, LIVE_ONLY);

    private static Collection<AttributeDefinition> ATTRIBUTES = ScaleDownAttributes.SCALE_DOWN_ATTRIBUTES;

    private static final AbstractAddStepHandler ADD  = new HornetQReloadRequiredHandlers.AddStepHandler(ATTRIBUTES) {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            super.execute(context, operation);
            context.addStep(ManagementHelper.checkNoOtherSibling(HA_POLICY), MODEL);
        }

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            checkAlternatives(operation, SCALE_DOWN_CONNECTORS.getName(), SCALE_DOWN_DISCOVERY_GROUP_NAME.getName(), true);

            super.populateModel(operation, model);
        }
    };

    private static final AbstractWriteAttributeHandler WRITE_ATTRIBUTE = new HornetQReloadRequiredHandlers.WriteAttributeHandler(ATTRIBUTES) {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.addStep(new AlternativeAttributeCheckHandler(ATTRIBUTES), MODEL);

            super.execute(context, operation);
        }
    };

    public static final LiveOnlyDefinition INSTANCE = new LiveOnlyDefinition();

    private LiveOnlyDefinition() {
        super(PATH,
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