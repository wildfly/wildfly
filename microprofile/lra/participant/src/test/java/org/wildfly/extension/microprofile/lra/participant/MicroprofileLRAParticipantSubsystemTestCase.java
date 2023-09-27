/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.participant;

import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
}
