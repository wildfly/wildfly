/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
