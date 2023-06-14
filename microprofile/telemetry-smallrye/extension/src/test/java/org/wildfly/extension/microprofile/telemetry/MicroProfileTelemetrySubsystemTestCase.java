/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
