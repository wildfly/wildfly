/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
import java.util.Properties;

import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Test;

/**
 * This is the barebone test example that tests subsystem
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class UndertowSubsystem10TestCase extends AbstractUndertowSubsystemTestCase {

    private final String virtualHostName = "default-server";
    private final int flag = 0;

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("undertow-1.0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-undertow_1_0.xsd";
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties properties = new Properties();
        properties.put("jboss.home.dir", System.getProperty("java.io.tmpdir"));
        properties.put("jboss.server.server.dir", System.getProperty("java.io.tmpdir"));
        properties.put("server.data.dir", System.getProperty("java.io.tmpdir"));
        return properties;
    }

    @Override
    protected KernelServices standardSubsystemTest(String configId, boolean compareXml) throws Exception {
        return super.standardSubsystemTest(configId, false);
    }

    @Test
    public void testRuntime() throws Exception {

        setProperty();
        KernelServicesBuilder builder = createKernelServicesBuilder(UndertowSubsystemTestCase.RUNTIME)
                .setSubsystemXml(getSubsystemXml());
        KernelServices mainServices = builder.build();
        testRuntime(mainServices, virtualHostName, flag);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return UndertowSubsystemTestCase.DEFAULT;
    }

}
