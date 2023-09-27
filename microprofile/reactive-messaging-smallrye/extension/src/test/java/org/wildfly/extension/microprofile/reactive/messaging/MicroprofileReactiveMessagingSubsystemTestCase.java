/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.messaging;

import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.CONFIG_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.REACTIVE_STREAMS_OPERATORS_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.WELD_CAPABILITY_NAME;

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
public class MicroprofileReactiveMessagingSubsystemTestCase extends AbstractSubsystemSchemaTest<MicroProfileReactiveMessagingSubsystemSchema> {
    @Parameters
    public static Iterable<MicroProfileReactiveMessagingSubsystemSchema> parameters() {
        return EnumSet.allOf(MicroProfileReactiveMessagingSubsystemSchema.class);
    }

    public MicroprofileReactiveMessagingSubsystemTestCase(MicroProfileReactiveMessagingSubsystemSchema schema) {
        super(MicroProfileReactiveMessagingExtension.SUBSYSTEM_NAME, new MicroProfileReactiveMessagingExtension(), schema, MicroProfileReactiveMessagingSubsystemSchema.CURRENT);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(CONFIG_CAPABILITY_NAME, REACTIVE_STREAMS_OPERATORS_CAPABILITY_NAME, WELD_CAPABILITY_NAME);
    }
}
