/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer;

import java.util.EnumSet;

import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author <a href="jasondlee@redhat.com">Jason Lee</a>
 */
@RunWith(Parameterized.class)
public class SubsystemParsingTestCase extends AbstractSubsystemSchemaTest<MicrometerSubsystemSchema> {
    @Parameters
    public static Iterable<MicrometerSubsystemSchema> parameters() {
        return EnumSet.allOf(MicrometerSubsystemSchema.class);
    }

    public SubsystemParsingTestCase(MicrometerSubsystemSchema schema) {
        super(MicrometerConfigurationConstants.NAME, new MicrometerExtension(), schema,
            MicrometerSubsystemSchema.VERSION_2_0);
    }
}
