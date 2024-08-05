/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;

/**
 * A {@link org.jboss.as.controller.ResourceDefinition} for the Jakarta Enterprise Beans async service
 * <p/>
 * @author Stuart Douglas
 */
public class EJB3AsyncResourceDefinition extends SimpleResourceDefinition {

    public static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.ejb3.async").build();

    static final SimpleAttributeDefinition THREAD_POOL_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.THREAD_POOL_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setCapabilityReference(CapabilityReferenceRecorder.builder(CAPABILITY, EJB3SubsystemRootResourceDefinition.EXECUTOR_SERVICE_DESCRIPTOR).build())
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { THREAD_POOL_NAME };

    EJB3AsyncResourceDefinition() {
        super(new Parameters(EJB3SubsystemModel.ASYNC_SERVICE_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.ASYNC))
                .setAddHandler(new EJB3AsyncServiceAdd())
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(CAPABILITY));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            // TODO: Make this RESTART_NONE by updating AsynchronousMergingProcessor
            resourceRegistration.registerReadWriteAttribute(attr, null, ReloadRequiredWriteAttributeHandler.INSTANCE);
        }
    }
}
