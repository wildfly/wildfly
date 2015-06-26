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

package org.jboss.as.jacorb;

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.DeprecationData;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jacorb.logging.JacORBLogger;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 */
public class JacORBSubsystemResource extends SimpleResourceDefinition {
    public static final JacORBSubsystemResource INSTANCE = new JacORBSubsystemResource();

    private JacORBSubsystemResource() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, JacORBExtension.SUBSYSTEM_NAME), JacORBExtension
                .getResourceDescriptionResolver(), JacORBSubsystemAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE, null,
                null, new DeprecationData((JacORBExtension.DEPRECATED_SINCE)));
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration registry) {
        OperationStepHandler attributeHander = new JacorbReloadRequiredWriteAttributeHandler(
                JacORBSubsystemDefinitions.SUBSYSTEM_ATTRIBUTES);
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
        protected void validateUpdatedModel(final OperationContext context, final Resource resource)
                throws OperationFailedException {
            final ModelNode model = resource.getModel();
            if (!context.getProcessType().equals(ProcessType.HOST_CONTROLLER)) {
                final List<String> propertiesToReject = new LinkedList<String>();
                for (final AttributeDefinition attribute : JacORBSubsystemDefinitions.ON_OFF_ATTRIBUTES_TO_REJECT) {
                    if (model.hasDefined(attribute.getName())
                            && model.get(attribute.getName()).equals(JacORBSubsystemDefinitions.DEFAULT_ENABLED_PROPERTY)) {
                        propertiesToReject.add(attribute.getName());
                    }
                }
                for (final AttributeDefinition attribute : JacORBSubsystemDefinitions.ATTRIBUTES_TO_REJECT) {
                    if (model.hasDefined(attribute.getName())) {
                        propertiesToReject.add(attribute.getName());
                    }
                }
                if (!propertiesToReject.isEmpty()) {
                    throw JacORBLogger.ROOT_LOGGER.cannotEmulateProperties(propertiesToReject);
                }
            }
        }

        @Override
        protected void finishModelStage(final OperationContext context, final ModelNode operation, final String attributeName,
                final ModelNode newValue, ModelNode oldValue, Resource model) throws OperationFailedException {
            // Make sure that security=on becomes security=identity
            if (attributeName.equals(JacORBSubsystemConstants.ORB_INIT_SECURITY)
                    && newValue.asString().equals(JacORBSubsystemConstants.ON)) {
                newValue.set(JacORBSubsystemConstants.IDENTITY);
            }
            super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        MigrateOperation.registerOperation(resourceRegistration, getResourceDescriptionResolver());
    }
}
