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

import java.io.IOException;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.wildfly.extension.picketlink.idm.IDMExtension;

/**
 * @author Pedro Igor
 */
public class IDMSubsystem_1_0_UnitTestCase extends AbstractSubsystemBaseTest {

    public IDMSubsystem_1_0_UnitTestCase() {
        super(IDMExtension.SUBSYSTEM_NAME, new IDMExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("identity-management-subsystem-1.0.xml");
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        //do not compare it
    }

    @Override
    protected void assertRemoveSubsystemResources(KernelServices kernelServices, Set<PathAddress> ignoredChildAddresses) {
        // we can not remove resources and leave subsystem in invalid state
    }
    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.ADMIN_ONLY_HC;
    }
}
