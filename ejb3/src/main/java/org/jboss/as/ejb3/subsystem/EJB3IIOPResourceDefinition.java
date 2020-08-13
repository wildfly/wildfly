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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * A {@link org.jboss.as.controller.ResourceDefinition} for the EJB IIOP resource definition
 * <p/>
 *
 * @author Stuart Douglas
 */
public class EJB3IIOPResourceDefinition extends SimpleResourceDefinition {

    public static final String EJB3_IIOP_SETTINGS_CAPABILITY_NAME = "org.wildfly.ejb3.iiop.settings-service";
    public static final RuntimeCapability<Void> EJB3_IIOP_SETTINGS_CAPABILITY =
            RuntimeCapability.Builder.of(EJB3_IIOP_SETTINGS_CAPABILITY_NAME, IIOPSettingsService.class).build();

    static final SimpleAttributeDefinition USE_QUALIFIED_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.USE_QUALIFIED_NAME, ModelType.BOOLEAN)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .setRequired(true)
                    .build();

    static final SimpleAttributeDefinition ENABLE_BY_DEFAULT =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.ENABLE_BY_DEFAULT, ModelType.BOOLEAN)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .setRequired(true)
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { ENABLE_BY_DEFAULT, USE_QUALIFIED_NAME };
    public static final EJB3IIOPResourceDefinition INSTANCE = new EJB3IIOPResourceDefinition();

    private EJB3IIOPResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(EJB3SubsystemModel.IIOP_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.IIOP))
                .setAddHandler(new EJB3IIOPAdd(ATTRIBUTES))
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(EJB3_IIOP_SETTINGS_CAPABILITY));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(USE_QUALIFIED_NAME, null, new AbstractIIOPSettingWriteHandler(USE_QUALIFIED_NAME) {
            @Override
            void applySetting(final IIOPSettingsService service, OperationContext context, final ModelNode model) throws OperationFailedException {
                final boolean value = USE_QUALIFIED_NAME.resolveModelAttribute(context, model).asBoolean();
                service.setUseQualifiedName(value);
            }
        });

        resourceRegistration.registerReadWriteAttribute(ENABLE_BY_DEFAULT, null, new AbstractIIOPSettingWriteHandler(ENABLE_BY_DEFAULT) {
            @Override
            void applySetting(final IIOPSettingsService service, OperationContext context, final ModelNode model) throws OperationFailedException {
                final boolean value = ENABLE_BY_DEFAULT.resolveModelAttribute(context, model).asBoolean();
                service.setEnabledByDefault(value);
            }
        });
    }

    private abstract static class AbstractIIOPSettingWriteHandler extends AbstractWriteAttributeHandler<Void> {

        public AbstractIIOPSettingWriteHandler(final AttributeDefinition attribute) {
            super(attribute);
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
            final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
            applyModelToRuntime(context, model);
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
            final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
            restored.get(attributeName).set(valueToRestore);
            applyModelToRuntime(context, restored);
        }


        private void applyModelToRuntime(OperationContext context, final ModelNode model) throws OperationFailedException {
            final ServiceRegistry serviceRegistry = context.getServiceRegistry(true);
            ServiceName iiopSettingsServiceName = context.getCapabilityServiceName(EJB3_IIOP_SETTINGS_CAPABILITY_NAME, IIOPSettingsService.class);
            ServiceController<IIOPSettingsService> controller = (ServiceController<IIOPSettingsService>) serviceRegistry.getService(iiopSettingsServiceName);
            if (controller != null) {
                IIOPSettingsService service = controller.getValue();
                applySetting(service, context, model);
            }
        }

        abstract void applySetting(final IIOPSettingsService service, final OperationContext context, final ModelNode model) throws OperationFailedException;
    }
}
