/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.telemetry;

import java.util.EnumSet;

import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Unit test for MicroProfile Telemetry subsystem.
 * @author Jason Lee
 */
@RunWith(value = Parameterized.class)
public class MicroProfileTelemetrySubsystemTestCase extends AbstractSubsystemSchemaTest<MicroProfileTelemetrySubsystemSchema> {

    @Parameters
    public static Iterable<MicroProfileTelemetrySubsystemSchema> parameters() {
        return EnumSet.allOf(MicroProfileTelemetrySubsystemSchema.class);
    }

    public MicroProfileTelemetrySubsystemTestCase(MicroProfileTelemetrySubsystemSchema schema) {
        super(MicroProfileTelemetryExtension.SUBSYSTEM_NAME, new MicroProfileTelemetryExtension(), schema, MicroProfileTelemetrySubsystemSchema.CURRENT);
    }

    @Override
    protected String getSubsystemXmlPathPattern() {
        return "%s_%d_%d.xml";
    }
}
