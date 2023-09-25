/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.subsystem;

import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Test;
import org.wildfly.extension.picketlink.federation.FederationExtension;

/**
 * @author Pedro Igor
 */
public class RoleGeneratorDeclarationUnitTestCase extends AbstractSubsystemBaseTest {

    public RoleGeneratorDeclarationUnitTestCase() {
        super(FederationExtension.SUBSYSTEM_NAME, new FederationExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("federation-subsystem-invalid-role-generator.xml");
    }

    @Test
    public void testSubsystem() throws Exception {
        System.setProperty("jboss.server.data.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.home.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.server.server.dir", System.getProperty("java.io.tmpdir"));

        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(getSubsystemXml());

        KernelServices mainServices = builder.build();

        assertFalse(mainServices.isSuccessfulBoot());
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.ADMIN_ONLY_HC;
    }
}
