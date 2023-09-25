/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.streams.operators;

import static org.wildfly.extension.microprofile.reactive.streams.operators.MicroProfileReactiveStreamsOperatorsExtension.WELD_CAPABILITY_NAME;

import java.util.EnumSet;

import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Parameterized.class)
public class MicroprofileReactiveStreamsOperatorsSubsystemTestCase extends AbstractSubsystemSchemaTest<MicroProfileReactiveStreamsOperatorsSubsystemSchema> {
    @Parameters
    public static Iterable<MicroProfileReactiveStreamsOperatorsSubsystemSchema> parameters() {
        return EnumSet.allOf(MicroProfileReactiveStreamsOperatorsSubsystemSchema.class);
    }

    public MicroprofileReactiveStreamsOperatorsSubsystemTestCase(MicroProfileReactiveStreamsOperatorsSubsystemSchema schema) {
        super(MicroProfileReactiveStreamsOperatorsExtension.SUBSYSTEM_NAME, new MicroProfileReactiveStreamsOperatorsExtension(), schema, MicroProfileReactiveStreamsOperatorsSubsystemSchema.CURRENT);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(WELD_CAPABILITY_NAME);
    }
}
