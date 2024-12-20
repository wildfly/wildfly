/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.subsystem;

import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.wildfly.extension.picketlink.federation.FederationExtension;

/**
 * @author Pedro Igor
 */
public class FederationSubsystem_1_0_UnitTestCase extends AbstractSubsystemTest {

    public FederationSubsystem_1_0_UnitTestCase() {
        super(FederationExtension.SUBSYSTEM_NAME, new FederationExtension());
    }

    @Test
    public void testParseAndMarshalModel() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml = readResource("federation-subsystem-1.0.xml");

        KernelServices servicesA = createKernelServicesBuilder(AdditionalInitialization.ADMIN_ONLY_HC)
                .setSubsystemXml(subsystemXml)
                .build();
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = createKernelServicesBuilder(AdditionalInitialization.ADMIN_ONLY_HC)
                .setSubsystemXml(marshalled)
                .build();
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);

        assertRemoveSubsystemResources(servicesB);
    }
}
