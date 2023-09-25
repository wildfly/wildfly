/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.coordinator;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.Constants;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.microprofile.lra.coordinator.MicroProfileLRACoordinatorExtension.SUBSYSTEM_NAME;


public class MicroProfileLRACoordinatorSubsystemDefinition extends SimpleResourceDefinition {

    static final PathElement PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    static final ParentResourceDescriptionResolver RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, MicroProfileLRACoordinatorExtension.class);

    static final String LRA_COORDINATOR_CAPABILITY_NAME = "org.wildfly.microprofile.lra.coordinator";
    static final String LRA_RECOVERY_SERVICE_CAPABILITY_NAME = "org.wildfly.microprofile.lra.recovery";
    static final String REF_JTA_RECOVERY_CAPABILITY = "org.wildfly.transactions.xa-resource-recovery-registry";

    static final RuntimeCapability<Void> LRA_COORDINATOR_CAPABILITY = RuntimeCapability.Builder
        .of(LRA_COORDINATOR_CAPABILITY_NAME)
        .setServiceType(Void.class)
        .build();
    static final RuntimeCapability<Void> LRA_RECOVERY_SERVICE_CAPABILITY = RuntimeCapability.Builder
        .of(LRA_RECOVERY_SERVICE_CAPABILITY_NAME)
        .setServiceType(Void.class)
        .build();

    static final SimpleAttributeDefinition SERVER =
        new SimpleAttributeDefinitionBuilder(CommonAttributes.SERVER, ModelType.STRING, true)
            .setAllowExpression(true)
            .setXmlName(CommonAttributes.SERVER)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(Constants.DEFAULT_SERVER))
            .build();

    static final SimpleAttributeDefinition HOST =
        new SimpleAttributeDefinitionBuilder(CommonAttributes.HOST, ModelType.STRING, true)
            .setAllowExpression(true)
            .setXmlName(CommonAttributes.HOST)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(Constants.DEFAULT_HOST))
            .build();

    static final AttributeDefinition[] ATTRIBUTES = {SERVER, HOST};

    MicroProfileLRACoordinatorSubsystemDefinition() {
        super(new Parameters(PATH, RESOLVER)
            .setAddHandler(new MicroProfileLRACoordinatorAdd())
            .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
            .setCapabilities(LRA_COORDINATOR_CAPABILITY, LRA_RECOVERY_SERVICE_CAPABILITY));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(SERVER, null, new ReloadRequiredWriteAttributeHandler(SERVER));
        resourceRegistration.registerReadWriteAttribute(HOST, null, new ReloadRequiredWriteAttributeHandler(HOST));
    }

}