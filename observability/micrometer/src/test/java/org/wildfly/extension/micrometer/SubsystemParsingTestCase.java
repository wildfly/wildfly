/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer;

import java.util.EnumSet;

import org.jboss.as.controller.Feature;
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
        // TODO WFCORE-7416 Eventually simplify by using this constructor AbstractSubsystemSchemaTest(String, Extension, S, Set<S>)
        super(MicrometerConfigurationConstants.NAME, new MicrometerExtension(), schema, Feature.map(MicrometerSubsystemSchema.CURRENT).get(schema.getStability()));
    }
}
