/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.coordinator;

import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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

}