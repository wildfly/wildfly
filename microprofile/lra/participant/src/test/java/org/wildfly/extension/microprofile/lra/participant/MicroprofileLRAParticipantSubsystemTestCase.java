/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.participant;

import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.Server;

import java.util.EnumSet;

@RunWith(Parameterized.class)
public class MicroprofileLRAParticipantSubsystemTestCase extends AbstractSubsystemSchemaTest<MicroProfileLRAParticipantSubsystemSchema> {

    @Parameterized.Parameters
    public static Iterable<MicroProfileLRAParticipantSubsystemSchema> parameters() {
        return EnumSet.allOf(MicroProfileLRAParticipantSubsystemSchema.class);
    }

    public MicroprofileLRAParticipantSubsystemTestCase(MicroProfileLRAParticipantSubsystemSchema schema) {
        super(MicroProfileLRAParticipantExtension.SUBSYSTEM_NAME, new MicroProfileLRAParticipantExtension(), schema, MicroProfileLRAParticipantExtension.CURRENT_SCHEMA);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        // The 'proxy-server' and 'proxy-host' attributes are capability references to the Undertow subsystem.
        // Register the referenced Undertow 'server' and 'host' capabilities as present so the
        // capability references in the parsed configuration resolve. The dynamic names must match
        // the 'proxy-server'/'proxy-host' values used in the test configuration.
        return AdditionalInitialization.withCapabilities(
                RuntimeCapability.buildDynamicCapabilityName(Server.SERVICE_DESCRIPTOR.getName(), "default-server"),
                RuntimeCapability.buildDynamicCapabilityName(Host.SERVICE_DESCRIPTOR.getName(), "default-server", "default-host"));
    }
}
