/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.subsystem;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Test;
import org.wildfly.extension.picketlink.idm.IDMExtension;

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

        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(getSubsystemXml());

        KernelServices mainServices = builder.build();

        assertTrue(mainServices.isSuccessfulBoot());
    }

    @Override
    protected void assertRemoveSubsystemResources(KernelServices kernelServices, Set<PathAddress> ignoredChildAddresses) {
        // we can not remove resources and leave subsystem in invalid state
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.ADMIN_ONLY_HC;
    }
}
