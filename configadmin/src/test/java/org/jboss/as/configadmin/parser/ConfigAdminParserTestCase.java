/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.configadmin.parser;

import java.io.IOException;
import java.util.List;

import junit.framework.Assert;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * Test the {@link ConfigAdminParser}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 10-Jan-2012
 */
public class ConfigAdminParserTestCase extends AbstractSubsystemBaseTest {

    private static final String SUBSYSTEM_XML_1_0_1 =
        "<subsystem xmlns='urn:jboss:domain:configadmin:1.0'>" +
        "  <!-- Some Comment -->" +
        "  <configuration pid='Pid1'>" +
        "    <property name='org.acme.key1' value='val 1'/>" +
        "  </configuration>" +
        "  <configuration pid='Pid2'>" +
        "    <property name='propname' value='propval'/>" +
        "  </configuration>" +
        "</subsystem>";

    public ConfigAdminParserTestCase() {
        super(ConfigAdminExtension.SUBSYSTEM_NAME, new ConfigAdminExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return SUBSYSTEM_XML_1_0_1;
    }

    @Test
    public void testParseEmptySubsystem() throws Exception {
        standardSubsystemTest(null);

    }

    @Test
    public void testReadWriteEmptySubsystem() throws Exception {
        String subsystemXml =
            "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
            "</subsystem>";

        ModelNode testModel = new ModelNode();
        testModel.get(ModelDescriptionConstants.SUBSYSTEM, ConfigAdminExtension.SUBSYSTEM_NAME).setEmptyObject();
        String triggered = outputModel(testModel);
        Assert.assertEquals(normalizeXML(subsystemXml), normalizeXML(triggered));
    }

    @Test
    public void testDescribeHandler() throws Exception {
        KernelServices servicesA = installInController(AdditionalInitialization.MANAGEMENT, SUBSYSTEM_XML_1_0_1);
        ModelNode modelA = servicesA.readWholeModel();
        ModelNode describeOp = new ModelNode();
        describeOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.DESCRIBE);
        describeOp.get(ModelDescriptionConstants.OP_ADDR).set(
                PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, ConfigAdminExtension.SUBSYSTEM_NAME)).toModelNode());
        List<ModelNode> operations = checkResultAndGetContents(servicesA.executeOperation(describeOp)).asList();

        KernelServices servicesB = installInController(AdditionalInitialization.MANAGEMENT, operations);
        ModelNode modelB = servicesB.readWholeModel();

        compare(modelA, modelB);
    }


    @Test
    public void testTransformers1_0_0() throws Exception {
        ModelVersion oldVersion = ModelVersion.create(1, 0, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(null)
                .setSubsystemXml(SUBSYSTEM_XML_1_0_1);
        builder.createLegacyKernelServicesBuilder(null, oldVersion)
                .setExtensionClassName(ConfigAdminExtension.class.getName())
                .addMavenResourceURL("org.jboss.as:jboss-as-configadmin:7.1.2.Final");
        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(oldVersion);
        Assert.assertNotNull(legacyServices);

        //Should be fine the actual model did not change between 1.0.0 and 1.0.1
        checkSubsystemModelTransformation(mainServices, oldVersion);


        ModelNode op = Util.getEmptyOperation(ModelConstants.UPDATE, new ModelNode().add(ModelDescriptionConstants.SUBSYSTEM, ConfigAdminExtension.SUBSYSTEM_NAME).add(ModelConstants.CONFIGURATION, "Pid1"));
        op.get(ModelConstants.ENTRIES).get("test123").set("testing123");
        op.get(ModelConstants.ENTRIES).get("test456").set("testingabc");
        mainServices.executeForResult(op);

        TransformedOperation transformedOp  = mainServices.transformOperation(oldVersion, op);
        checkResultAndGetContents(mainServices.executeOperation(oldVersion, transformedOp));

        checkSubsystemModelTransformation(mainServices, oldVersion);
        ModelNode pid1 = legacyServices.readWholeModel().get(ModelDescriptionConstants.SUBSYSTEM, ConfigAdminExtension.SUBSYSTEM_NAME, ModelConstants.CONFIGURATION, "Pid1");
        Assert.assertTrue(pid1.hasDefined(ModelConstants.ENTRIES));
        ModelNode entries = pid1.get(ModelConstants.ENTRIES);
        Assert.assertEquals(2, entries.keys().size());
        Assert.assertEquals("testing123", entries.get("test123").asString());
        Assert.assertEquals("testingabc", entries.get("test456").asString());
    }

    private void assertOSGiSubsystemAddress(ModelNode address) {
        PathAddress addr = PathAddress.pathAddress(address);
        PathElement element = addr.getElement(0);
        Assert.assertEquals(ModelDescriptionConstants.SUBSYSTEM, element.getKey());
        Assert.assertEquals(ConfigAdminExtension.SUBSYSTEM_NAME, element.getValue());
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }
}
