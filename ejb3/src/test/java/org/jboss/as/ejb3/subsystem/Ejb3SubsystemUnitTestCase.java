/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;


import java.io.IOException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.singleton.SingletonDefaultRequirement;

/**
 * Test case for testing the integrity of the EJB3 subsystem.
 *
 * This checks the following features:
 * - basic subsystem testing (i.e. current model version boots successfully)
 * - registered transformers transform model and operations correctly between different API model versions
 * - expressions appearing in XML configurations are correctly rejected if so required
 * - bad attribute values are correctly rejected
 *
 * @author Emanuel Muckenhuber
 */

public class Ejb3SubsystemUnitTestCase extends AbstractSubsystemBaseTest {

    private static final AdditionalInitialization ADDITIONAL_INITIALIZATION = AdditionalInitialization.withCapabilities(
            SingletonDefaultRequirement.POLICY.getName());

    public Ejb3SubsystemUnitTestCase() {
        super(EJB3Extension.SUBSYSTEM_NAME, new EJB3Extension());
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return ADDITIONAL_INITIALIZATION;
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-ejb3_5_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/ejb3.xml"
        };
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
    }

    @Test
    public void test15() throws Exception {
        standardSubsystemTest("subsystem15.xml", false);
    }

    /** WFLY-7797 */
    @Test
    public void testPoolSizeAlternatives() throws Exception {
        // Parse the subsystem xml and install into the first controller
        final String subsystemXml = getSubsystemXml();
        final KernelServices ks = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT).setSubsystemXml(subsystemXml).build();
        Assert.assertTrue("Subsystem boot failed!", ks.isSuccessfulBoot());

        PathAddress pa = PathAddress.pathAddress("subsystem", "ejb3").append("strict-max-bean-instance-pool", "slsb-strict-max-pool");

        ModelNode composite = Util.createEmptyOperation("composite", PathAddress.EMPTY_ADDRESS);
        ModelNode steps = composite.get("steps");
        ModelNode writeMax = Util.getWriteAttributeOperation(pa, "max-pool-size", 5);
        ModelNode writeDerive = Util.getWriteAttributeOperation(pa, "derive-size", "none");

        steps.add(writeMax);
        steps.add(writeDerive);

        // none works in combo with max-pool-size
        ModelNode response = ks.executeOperation(composite);
        Assert.assertEquals(response.toString(), "success", response.get("outcome").asString());

        validatePoolConfig(ks, pa);

        steps.setEmptyList();

        // Other values fail in combo with max-pool-size
        writeMax.get("value").set(10);
        writeDerive.get("value").set("from-cpu-count");

        steps.add(writeMax);
        steps.add(writeDerive);

        ks.executeForFailure(composite);

        validatePoolConfig(ks, pa);

    }

    private void validatePoolConfig(KernelServices ks, PathAddress pa) {
        ModelNode ra = Util.createEmptyOperation("read-attribute", pa);
        ra.get("name").set("max-pool-size");
        ModelNode response = ks.executeOperation(ra);
        Assert.assertEquals(response.toString(), 5, response.get("result").asInt());
        ra.get("name").set("derive-size");
        response = ks.executeOperation(ra);
        Assert.assertFalse(response.toString(), response.hasDefined("result"));
    }

}
