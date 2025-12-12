/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.openapi;

import java.util.EnumSet;

import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Unit test for MicroProfile OpenAPI subsystem.
 * @author Paul Ferraro
 */
@RunWith(Parameterized.class)
public class MicroProfileOpenAPISubsystemTestCase extends AbstractSubsystemSchemaTest<MicroProfileOpenAPISubsystemSchema> {

    @Parameters
    public static Iterable<MicroProfileOpenAPISubsystemSchema> parameters() {
        return EnumSet.allOf(MicroProfileOpenAPISubsystemSchema.class);
    }

    public MicroProfileOpenAPISubsystemTestCase(MicroProfileOpenAPISubsystemSchema schema) {
        super(MicroProfileOpenAPISubsystemRegistrar.REGISTRATION.getName(), new MicroProfileOpenAPIExtension(), schema, MicroProfileOpenAPISubsystemSchema.CURRENT);
    }
}