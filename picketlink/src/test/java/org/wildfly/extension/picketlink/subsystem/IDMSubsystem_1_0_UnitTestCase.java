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

import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.wildfly.extension.picketlink.idm.IDMExtension;

/**
 * @author Pedro Igor
 */
public class IDMSubsystem_1_0_UnitTestCase extends AbstractSubsystemTest {

    public IDMSubsystem_1_0_UnitTestCase() {
        super(IDMExtension.SUBSYSTEM_NAME, new IDMExtension());
    }

    @Test
    public void testParseAndMarshalModel() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml = readResource("identity-management-subsystem-1.0.xml");

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
}
