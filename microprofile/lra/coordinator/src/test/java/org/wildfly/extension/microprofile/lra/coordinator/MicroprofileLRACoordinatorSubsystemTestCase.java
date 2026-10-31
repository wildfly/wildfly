/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.coordinator;

import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.Server;

import java.util.EnumSet;

@RunWith(Parameterized.class)
public class MicroprofileLRACoordinatorSubsystemTestCase extends AbstractSubsystemSchemaTest<MicroProfileLRACoordinatorSubsystemSchema> {

    @Parameterized.Parameters
    public static Iterable<MicroProfileLRACoordinatorSubsystemSchema> parameters() {
        return EnumSet.allOf(MicroProfileLRACoordinatorSubsystemSchema.class);
    }

    public MicroprofileLRACoordinatorSubsystemTestCase(MicroProfileLRACoordinatorSubsystemSchema schema) {
        super(MicroProfileLRACoordinatorExtension.SUBSYSTEM_NAME, new MicroProfileLRACoordinatorExtension(), schema, MicroProfileLRACoordinatorExtension.CURRENT_SCHEMA);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        // The 'server' and 'host' attributes are capability references to the Undertow subsystem.
        // Register the referenced Undertow 'server' and 'host' capabilities as present so the
        // capability references in the parsed configuration resolve. The dynamic names must match
        // the 'server'/'host' values used in the test configuration.
        return AdditionalInitialization.withCapabilities(
                RuntimeCapability.buildDynamicCapabilityName(Server.SERVICE_DESCRIPTOR.getName(), "default-server"),
                RuntimeCapability.buildDynamicCapabilityName(Host.SERVICE_DESCRIPTOR.getName(), "default-server", "default-host"));
    }

}