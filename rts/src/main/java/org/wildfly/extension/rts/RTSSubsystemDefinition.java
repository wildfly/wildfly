/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.rts;

import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.rts.configuration.Attribute;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
public final class RTSSubsystemDefinition extends SimpleResourceDefinition {

    static final String XA_RESOURCE_RECOVERY_CAPABILITY = "org.wildfly.transactions.xa-resource-recovery-registry";
    /** Private capability that currently just represents the existence of the subsystem */
    public static final RuntimeCapability<Void> RTS_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.extension.rts", false, Void.class)
            .addRequirements(XA_RESOURCE_RECOVERY_CAPABILITY)
            .build();

    public static final RuntimeCapability<Void> RTS_COORDINATOR_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.extension.rts.coordinator", false, Void.class)
            .addRequirements(XA_RESOURCE_RECOVERY_CAPABILITY)
            .build();

    public static final RuntimeCapability<Void> RTS_PARTICIPANT_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.extension.rts.participant", false, Void.class)
            .addRequirements(XA_RESOURCE_RECOVERY_CAPABILITY)
            .build();

    public static final RuntimeCapability<Void> RTS_VOLATILE_PARTICIPANT_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.extension.rts.volatile-participant", false, Void.class)
            .addRequirements(XA_RESOURCE_RECOVERY_CAPABILITY)
            .build();

    protected static final SimpleAttributeDefinition SERVER =
            new SimpleAttributeDefinitionBuilder(Attribute.SERVER.getLocalName(), ModelType.STRING, true)
                    .setAllowExpression(false)
                    .setXmlName(Attribute.SERVER.getLocalName())
                    .setFlags(AttributeAccess.Flag.RESTART_JVM)
                    .build();

    protected static final SimpleAttributeDefinition HOST =
            new SimpleAttributeDefinitionBuilder(Attribute.HOST.getLocalName(), ModelType.STRING, true)
                    .setAllowExpression(false)
                    .setXmlName(Attribute.HOST.getLocalName())
                    .setFlags(AttributeAccess.Flag.RESTART_JVM)
                    .build();

    protected static final SimpleAttributeDefinition SOCKET_BINDING =
            new SimpleAttributeDefinitionBuilder(Attribute.SOCKET_BINDING.getLocalName(), ModelType.STRING, true)
                    .setAllowExpression(false)
                    .setXmlName(Attribute.SOCKET_BINDING.getLocalName())
                    .setFlags(AttributeAccess.Flag.RESTART_JVM)
                    .build();

    RTSSubsystemDefinition() {
        super(new Parameters(RTSSubsystemExtension.SUBSYSTEM_PATH, RTSSubsystemExtension.getResourceDescriptionResolver(null))
                .setAddHandler(RTSSubsystemAdd.INSTANCE)
                .setRemoveHandler(RTSSubsystemRemove.INSTANCE)
                .setCapabilities(RTS_CAPABILITY)
        );
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(SERVER, null, new ReloadRequiredWriteAttributeHandler(SERVER));
        resourceRegistration.registerReadWriteAttribute(HOST, null, new ReloadRequiredWriteAttributeHandler(HOST));
        resourceRegistration.registerReadWriteAttribute(SOCKET_BINDING, null, new ReloadRequiredWriteAttributeHandler(SOCKET_BINDING));
    }
}
