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

package org.jboss.as.jacorb;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar
 * @created 6.1.12 23:00
 */
public class JacORBSubsystemResource extends SimpleResourceDefinition {
    public static final JacORBSubsystemResource INSTANCE = new JacORBSubsystemResource();

    private JacORBSubsystemResource() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, JacORBExtension.SUBSYSTEM_NAME),
                JacORBExtension.getResourceDescriptionResolver(),
                JacORBSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }


    @Override
    public void registerAttributes(final ManagementResourceRegistration registry) {
        OperationStepHandler attributeHander = new JacorbReloadRequiredWriteAttributeHandler(JacORBSubsystemDefinitions.SUBSYSTEM_ATTRIBUTES);
        for (AttributeDefinition attr : JacORBSubsystemDefinitions.SUBSYSTEM_ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr, null, attributeHander);
        }
    }

    @Override
    public void registerChildren(final ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(IORSettingsDefinition.INSTANCE);
    }

    private static class JacorbReloadRequiredWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {
        public JacorbReloadRequiredWriteAttributeHandler(List<AttributeDefinition> definitions) {
            super(definitions);
        }

        @Override
        protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue,
                ModelNode oldValue, Resource model) throws OperationFailedException {
            //Make sure that security=on becomes security=identity
            if (attributeName.equals(JacORBSubsystemConstants.ORB_INIT_SECURITY) && newValue.asString().equals("on")) {
                newValue.set(JacORBSubsystemConstants.IDENTITY);
            }
            super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);
        }
    }
}
