/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.security;

import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

public class SecurityDomainModelv1UnitTestCase extends AbstractSubsystemTest {

    public SecurityDomainModelv1UnitTestCase() {
        super(SecurityExtension.SUBSYSTEM_NAME, new SecurityExtension());
    }

    @Test
    public void testParseAndMarshalModel() throws Exception {

        //Parse the subsystem xml and install into the first controller
        String subsystemXml = readResource("securitysubsystemv1.xml");

        KernelServices servicesA = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml)
                .build();
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(marshalled)
                .build();
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    @Test
    public void testParseAndMarshalModelWithJASPI() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml = readResource("securitysubsystemJASPIv1.xml");

        KernelServices servicesA = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml)
                .build();
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(marshalled)
                .build();
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);

        assertRemoveSubsystemResources(servicesB);
    }

    @Override
    protected KernelServicesBuilder createKernelServicesBuilder(AdditionalInitialization additionalInit) {
        return super.createKernelServicesBuilder(AdditionalInitialization.ADMIN_ONLY_HC);
    }

}