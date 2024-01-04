/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.faulttolerance;

import static org.junit.runners.Parameterized.Parameters;

import java.util.EnumSet;

import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Radoslav Husar
 */
@RunWith(value = Parameterized.class)
public class MicroProfileFaultToleranceModelTest extends AbstractSubsystemSchemaTest<MicroProfileFaultToleranceSchema> {

    @Parameters
    public static Iterable<MicroProfileFaultToleranceSchema> parameters() {
        return EnumSet.allOf(MicroProfileFaultToleranceSchema.class);
    }

    public MicroProfileFaultToleranceModelTest(MicroProfileFaultToleranceSchema testSchema) {
        super(MicroProfileFaultToleranceExtension.SUBSYSTEM_NAME, new MicroProfileFaultToleranceExtension(), testSchema, MicroProfileFaultToleranceSchema.CURRENT);
    }

    @Override
    protected String getSubsystemXmlPathPattern() {
        // Exclude subsystem name from pattern
        return "subsystem_%2$d_%3$d.xml";
    }
}