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
package org.jboss.as.security;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Jason T. Greene
 */
class SecuritySubsystemRootResourceDefinition extends SimpleResourceDefinition {

    private static final RuntimeCapability<Void> SECURITY_SUBSYSTEM = RuntimeCapability.Builder.of("org.wildfly.legacy-security").build();
    private static final RuntimeCapability<Void> SERVER_SECURITY_MANAGER = RuntimeCapability.Builder.of("org.wildfly.legacy-security.server-security-manager")
            .build();
    private static final RuntimeCapability<Void> SUBJECT_FACTORY_CAP = RuntimeCapability.Builder.of("org.wildfly.legacy-security.subject-factory")
            .build();
    private static final RuntimeCapability<Void> JACC_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.legacy-security.jacc")
            .build();
    private static final RuntimeCapability<Void> JACC_CAPABILITY_TOMBSTONE = RuntimeCapability.Builder.of("org.wildfly.legacy-security.jacc.tombstone")
       .build();

    private static final SensitiveTargetAccessConstraintDefinition MISC_SECURITY_SENSITIVITY = new SensitiveTargetAccessConstraintDefinition(
            new SensitivityClassification(SecurityExtension.SUBSYSTEM_NAME, "misc-security", false, true, true));

    static final SecuritySubsystemRootResourceDefinition INSTANCE = new SecuritySubsystemRootResourceDefinition();

    static final SimpleAttributeDefinition DEEP_COPY_SUBJECT_MODE = new SimpleAttributeDefinitionBuilder(Constants.DEEP_COPY_SUBJECT_MODE, ModelType.BOOLEAN, true)
                    .setAccessConstraints(MISC_SECURITY_SENSITIVITY)
                    .setDefaultValue(ModelNode.FALSE)
                    .setAllowExpression(true)
                    .build();
    static final SimpleAttributeDefinition INITIALIZE_JACC = new SimpleAttributeDefinitionBuilder(Constants.INITIALIZE_JACC, ModelType.BOOLEAN, true)
            .setDefaultValue(ModelNode.TRUE)
            .setRestartJVM()
            .setAllowExpression(true)
            .build();

    private SecuritySubsystemRootResourceDefinition() {
        super(new Parameters(SecurityExtension.PATH_SUBSYSTEM,
                SecurityExtension.getResourceDescriptionResolver(SecurityExtension.SUBSYSTEM_NAME))
                .setAddHandler(SecuritySubsystemAdd.INSTANCE)
                .setRemoveHandler(new ReloadRequiredRemoveStepHandler() {

                    @Override
                    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation,
                            Resource resource) throws OperationFailedException {
                        super.recordCapabilitiesAndRequirements(context, operation, resource);
                        context.deregisterCapability(JACC_CAPABILITY.getName());
                    }
                })
                .setCapabilities(SECURITY_SUBSYSTEM, SERVER_SECURITY_MANAGER, SUBJECT_FACTORY_CAP));
        setDeprecated(SecurityExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
         resourceRegistration.registerReadWriteAttribute(DEEP_COPY_SUBJECT_MODE, null, new ReloadRequiredWriteAttributeHandler(DEEP_COPY_SUBJECT_MODE));
        resourceRegistration.registerReadWriteAttribute(INITIALIZE_JACC, null, new ReloadRequiredWriteAttributeHandler(INITIALIZE_JACC) {
            @Override
            protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> voidHandback) throws OperationFailedException {
                // As the PolicyConfigurationFactory is a singleton, once it's initialized any changes will require a restart
                CapabilityServiceSupport capabilitySupport = context.getCapabilityServiceSupport();
                final boolean elytronJacc = capabilitySupport.hasCapability("org.wildfly.security.jacc-policy");
                if (resolvedValue.asBoolean() == true && elytronJacc) {
                    throw SecurityLogger.ROOT_LOGGER.unableToEnableJaccSupport();
                }
                return super.applyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue, voidHandback);
            }

             @Override
            protected void recordCapabilitiesAndRequirements(OperationContext context,
                                                             AttributeDefinition attributeDefinition,
                                                             ModelNode newValue,
                                                             ModelNode oldValue) {
                super.recordCapabilitiesAndRequirements(context, attributeDefinition, newValue, oldValue);

                boolean shouldRegister = resolveValue(context, attributeDefinition, newValue);
                boolean registered = resolveValue(context, attributeDefinition, oldValue);

                if (!shouldRegister) {
                    context.deregisterCapability(JACC_CAPABILITY.getName());
                }
                if (!registered && shouldRegister) {
                    context.registerCapability(JACC_CAPABILITY);
                    // do not register the JACC_CAPABILITY_TOMBSTONE at this point - it will be registered on restart
                }
            }

            private boolean resolveValue(OperationContext context, AttributeDefinition attributeDefinition, ModelNode node) {
                try {
                    return attributeDefinition.resolveValue(context, node).asBoolean();
                } catch (OperationFailedException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    private static class SecuritySubsystemAdd extends AbstractBoottimeAddStepHandler {

        public static final OperationStepHandler INSTANCE = new SecuritySubsystemAdd();

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            DEEP_COPY_SUBJECT_MODE.validateAndSet(operation, model);
            INITIALIZE_JACC.validateAndSet(operation, model);
        }

        @Override
        protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource)
                throws OperationFailedException {
            super.recordCapabilitiesAndRequirements(context, operation, resource);
            if (INITIALIZE_JACC.resolveModelAttribute(context, resource.getModel()).asBoolean()) {
                context.registerCapability(JACC_CAPABILITY);
                // tombstone marks the Policy being initiated and should not be removed until restart
                if (context.isBooting()) {
                    context.registerCapability(JACC_CAPABILITY_TOMBSTONE);
                }
            }
        }
    }

    @Override
    public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(RuntimePackageDependency.required("javax.security.auth.message.api"));
    }
}
