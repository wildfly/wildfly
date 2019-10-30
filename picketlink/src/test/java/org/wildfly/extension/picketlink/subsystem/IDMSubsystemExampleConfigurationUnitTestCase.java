/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.subsystem;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Test;
import org.wildfly.extension.picketlink.idm.IDMExtension;

import java.io.IOException;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * @author Pedro Igor
 */
public class IDMSubsystemExampleConfigurationUnitTestCase extends AbstractSubsystemBaseTest {

    public IDMSubsystemExampleConfigurationUnitTestCase() {
        super(IDMExtension.SUBSYSTEM_NAME, new IDMExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("identity-management-subsystem-example-2.0.xml");
    }

    @Test
    public void testRuntime() throws Exception {
        System.setProperty("jboss.server.data.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.home.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.server.server.dir", System.getProperty("java.io.tmpdir"));

        KernelServicesBuilder builder = createKernelServicesBuilder(new AdditionalInitialization() {
            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.NORMAL;
            }

            @Override
            protected void setupController(ControllerInitializer controllerInitializer) {
                super.setupController(controllerInitializer);
                controllerInitializer.addPath("jboss.server.data.dir", System.getProperty("java.io.tmpdir"), null);
            }
        }).setSubsystemXml(getSubsystemXml());

        KernelServices mainServices = builder.build();

        assertTrue(mainServices.isSuccessfulBoot());
    }

    @Override
    protected void assertRemoveSubsystemResources(KernelServices kernelServices, Set<PathAddress> ignoredChildAddresses) {
        // we can not remove resources and leave subsystem in invalid state
    }
}
