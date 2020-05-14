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
package org.wildfly.extension.microprofile.openapi;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Locale;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Unit test for MicroProfile OpenAPI subsystem.
 * @author Paul Ferraro
 */
@RunWith(value = Parameterized.class)
public class MicroProfileOpenAPISubsystemTestCase extends AbstractSubsystemBaseTest {

    private final MicroProfileOpenAPISchema schema;

    @Parameters
    public static Iterable<MicroProfileOpenAPISchema> parameters() {
        return EnumSet.allOf(MicroProfileOpenAPISchema.class);
    }

    public MicroProfileOpenAPISubsystemTestCase(MicroProfileOpenAPISchema schema) {
        super(MicroProfileOpenAPIExtension.SUBSYSTEM_NAME, new MicroProfileOpenAPIExtension());
        this.schema = schema;
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return this.readResource(String.format(Locale.ROOT, "%s_%d_%d.xml", this.getMainSubsystemName(), this.schema.major(), this.schema.minor()));
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return String.format(Locale.ROOT, "schema/wildfly-%s_%d_%d.xsd", this.getMainSubsystemName(), this.schema.major(), this.schema.minor());
    }
}