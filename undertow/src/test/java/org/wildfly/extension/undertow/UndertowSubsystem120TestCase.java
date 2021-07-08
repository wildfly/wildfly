/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.io.IOException;

import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Test;

/**
 * Test for UndertowSubsystem with subsystem schema version 12.0.
 *
 * @author Flavia Rainone
 */
public class UndertowSubsystem120TestCase extends AbstractUndertowSubsystemTestCase {

    private static final int SCHEMA_VERSION = 12;
    private final String virtualHostName = "some-server";

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("undertow-12.0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-undertow_12_0.xsd";
    }

    @Test
    public void testRuntime() throws Exception {
        setProperty();
        KernelServicesBuilder builder = createKernelServicesBuilder(RUNTIME).setSubsystemXml(getSubsystemXml());
        KernelServices mainServices = builder.build();
        testRuntime(mainServices, virtualHostName, SCHEMA_VERSION);
        testRuntimeOther(mainServices);
        testRuntimeLast(mainServices);
    }
}
