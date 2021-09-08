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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.picketlink.idm.IDMExtension;

/**
 * @author Pedro Igor
 */
public class IDMSubsystem_2_0_UnitTestCase extends AbstractSubsystemBaseTest {

    public IDMSubsystem_2_0_UnitTestCase() {
        super(IDMExtension.SUBSYSTEM_NAME, new IDMExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("identity-management-subsystem-2.0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-picketlink-idm_2_0.xsd";
    }

    @Test
    public void testExpressions() throws Exception {
        standardSubsystemTest("identity-management-subsystem-expressions-2.0.xml");
    }

    @Test
    public void testValidation() throws Exception {

        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(getSubsystemXml());

        KernelServices mainServices = builder.build();

        assertTrue(mainServices.isSuccessfulBoot());

        // LDAP identity store must have a mapping
        ModelNode op = Util.createRemoveOperation(getAddress("partition-manager=ldap.based.partition.manager/identity-configuration=ldap.config/ldap-store=ldap-store/mapping=Agent"));
        mainServices.executeForResult(op); // removing 1 is ok
        op =  Util.createEmptyOperation("composite", PathAddress.EMPTY_ADDRESS);
        ModelNode steps = op.get("steps");
        steps.add(Util.createRemoveOperation(getAddress("partition-manager=ldap.based.partition.manager/identity-configuration=ldap.config/ldap-store=ldap-store/mapping=User")));
        steps.add(Util.createRemoveOperation(getAddress("partition-manager=ldap.based.partition.manager/identity-configuration=ldap.config/ldap-store=ldap-store/mapping=Role")));
        steps.add(Util.createRemoveOperation(getAddress("partition-manager=ldap.based.partition.manager/identity-configuration=ldap.config/ldap-store=ldap-store/mapping=Group")));
        steps.add(Util.createRemoveOperation(getAddress("partition-manager=ldap.based.partition.manager/identity-configuration=ldap.config/ldap-store=ldap-store/mapping=Grant")));
        executeForFailure(mainServices, op, "WFLYPL0057"); // removing all is not ok

        // identity store must have a supported type (removal)
        op = Util.createRemoveOperation(getAddress("partition-manager=ldap.based.partition.manager/identity-configuration=ldap.config/ldap-store=ldap-store/supported-types=supported-types/supported-type=IdentityType"));
        mainServices.executeForResult(op); // removing 1 is ok
        op = Util.createRemoveOperation(getAddress("partition-manager=ldap.based.partition.manager/identity-configuration=ldap.config/ldap-store=ldap-store/supported-types=supported-types/supported-type=Relationship"));
        executeForFailure(mainServices, op, "WFLYPL0056"); // removing all is not ok

        // identity store must have a supported type (not supports all)
        op = Util.getWriteAttributeOperation(getAddress("partition-manager=file.based.partition.manager/identity-configuration=file.config/file-store=file-store/supported-types=supported-types"), "supports-all", "false");
        executeForFailure(mainServices, op, "WFLYPL0056");

        // PM must have an identity config
        op = Util.createRemoveOperation(getAddress("partition-manager=file.based.partition.manager/identity-configuration=file.config"));
        executeForFailure(mainServices, op, "WFLYPL0054");

        // complete PM removal works
        op = Util.createRemoveOperation(getAddress("partition-manager=ldap.based.partition.manager"));
        mainServices.executeForResult(op);
        op = Util.getReadResourceOperation(getAddress("partition-manager=ldap.based.partition.manager"));
        executeForFailure(mainServices, op, "WFLYCTL0216"); // confirm it's gone
    }

    @Override
    protected void assertRemoveSubsystemResources(KernelServices kernelServices, Set<PathAddress> ignoredChildAddresses) {
        // we can not remove resources and leave subsystem in invalid state
    }

    private static PathAddress getAddress(String cliStyleAddress) {
        return PathAddress.parseCLIStyleAddress("/subsystem=picketlink-identity-management/" + cliStyleAddress);
    }

    private static void executeForFailure(KernelServices services, ModelNode op, String expectedFailureCode) {
        ModelNode resp = services.executeOperation(op);
        Assert.assertEquals(resp.toString(), "failed", resp.get("outcome").asString());
        String failDesc = resp.get("failure-description").asString();
        Assert.assertTrue(resp.toString(), failDesc.contains(expectedFailureCode));
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.ADMIN_ONLY_HC;
    }
}
