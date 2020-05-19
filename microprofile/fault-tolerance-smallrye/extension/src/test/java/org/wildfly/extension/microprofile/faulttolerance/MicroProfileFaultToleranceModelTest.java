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

package org.wildfly.extension.microprofile.faulttolerance;

import static org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Locale;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Radoslav Husar
 */
@RunWith(value = Parameterized.class)
public class MicroProfileFaultToleranceModelTest extends AbstractSubsystemBaseTest {

    @Parameters
    public static Iterable<MicroProfileFaultToleranceSchema> parameters() {
        return EnumSet.allOf(MicroProfileFaultToleranceSchema.class);
    }

    private MicroProfileFaultToleranceSchema testSchema;

    public MicroProfileFaultToleranceModelTest(MicroProfileFaultToleranceSchema testSchema) {
        super(MicroProfileFaultToleranceExtension.SUBSYSTEM_NAME, new MicroProfileFaultToleranceExtension());
        this.testSchema = testSchema;
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource(String.format(Locale.ROOT, "subsystem_%d_%d.xml", this.testSchema.major(), this.testSchema.minor()));
    }

    @Override
    protected String getSubsystemXsdPath() {
        return String.format(Locale.ROOT, "schema/wildfly-microprofile-fault-tolerance-smallrye_%d_%d.xsd", this.testSchema.major(), this.testSchema.minor());
    }
}