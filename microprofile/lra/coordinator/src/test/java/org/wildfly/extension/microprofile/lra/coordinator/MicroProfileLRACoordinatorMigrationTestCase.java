/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.coordinator;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.Server;

/**
 * Verifies that a configuration migrated from the {@code 1.0} schema, which omits the {@code server} and
 * {@code host} attributes, retains the default values ({@code default-server}/{@code default-host}) that the
 * {@code 1.0} schema declared for them. Version {@code 2.0} models these as capability references, which
 * cannot declare default values, so the schema repopulates the previous defaults when the attributes are
 * omitted from a {@code 1.0} configuration.
 */
public class MicroProfileLRACoordinatorMigrationTestCase extends AbstractSubsystemTest {

    public MicroProfileLRACoordinatorMigrationTestCase() {
        super(MicroProfileLRACoordinatorExtension.SUBSYSTEM_NAME, new MicroProfileLRACoordinatorExtension());
    }

    @Test
    public void testLegacyDefaultsPreserved() throws Exception {
        AdditionalInitialization additionalInitialization = AdditionalInitialization.withCapabilities(
                RuntimeCapability.resolveCapabilityName(Server.SERVICE_DESCRIPTOR, "default-server"),
                RuntimeCapability.resolveCapabilityName(Host.SERVICE_DESCRIPTOR, "default-server", "default-host"));

        KernelServices services = super.createKernelServicesBuilder(additionalInitialization)
                .setSubsystemXml(readResource("microprofile-lra-coordinator-1.0.xml"))
                .build();
        Assert.assertTrue(services.isSuccessfulBoot());

        ModelNode subsystem = services.readWholeModel().get(SUBSYSTEM, MicroProfileLRACoordinatorExtension.SUBSYSTEM_NAME);
        Assert.assertEquals("default-server", subsystem.get(MicroProfileLRACoordinatorSubsystemDefinition.SERVER.getName()).asString());
        Assert.assertEquals("default-host", subsystem.get(MicroProfileLRACoordinatorSubsystemDefinition.HOST.getName()).asString());
    }
}
