/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.keycloak.subsystem.adapter.extension;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Tests all management expects for subsystem, parsing, marshaling, model definition and other
 * Here is an example that allows you a fine grained controller over what is tested and how. So it can give you ideas what can be done and tested.
 * If you have no need for advanced testing of subsystem you look at {@link SubsystemBaseParsingTestCase} that testes same stuff but most of the code
 * is hidden inside of test harness
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Tomaz Cerar
 * @author <a href="marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class SubsystemParsingTestCase extends AbstractSubsystemBaseTest {

    public SubsystemParsingTestCase() {
        super(KeycloakExtension.SUBSYSTEM_NAME, new KeycloakExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("keycloak-1.2.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-keycloak_1_2.xsd";
    }

    /**
     * Checks if the subsystem is still capable of reading a configuration that uses version 1.1 of the schema.
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    public void testSubsystem1_1() throws Exception {
        KernelServices servicesA = super.createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(readResource("keycloak-1.1.xml")).build();
        Assert.assertTrue("Subsystem boot failed!", servicesA.isSuccessfulBoot());
        ModelNode modelA = servicesA.readWholeModel();
        super.validateModel(modelA);
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.ADMIN_ONLY_HC;
    }
}
