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

import junit.framework.Assert;
import org.jboss.as.controller.OperationContext.Type;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * Test the {@link ConfigAdminParser}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 10-Jan-2012
 */
public class ConfigAdminParserTestCase extends AbstractSubsystemBaseTest {

    private static final String SUBSYSTEM_XML_1_0 =
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
        return SUBSYSTEM_XML_1_0;
    }

    @Test
    public void testParseEmptySubsystem() throws Exception {
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "</subsystem>";
        List<ModelNode> operations = parse(subsystemXml);

        // Check that we have the expected number of operations
        Assert.assertEquals(1, operations.size());

        // Check that each operation has the correct content
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ModelDescriptionConstants.ADD, addSubsystem.get(ModelDescriptionConstants.OP).asString());
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(ModelDescriptionConstants.OP_ADDR));
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(ModelDescriptionConstants.SUBSYSTEM, element.getKey());
        Assert.assertEquals(ConfigAdminExtension.SUBSYSTEM_NAME, element.getValue());
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
        KernelServices servicesA = installInController(new AdditionalInitialization() {
            @Override
            protected Type getType() {
                return Type.MANAGEMENT;
            }
        }, SUBSYSTEM_XML_1_0);
        ModelNode modelA = servicesA.readWholeModel();
        ModelNode describeOp = new ModelNode();
        describeOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.DESCRIBE);
        describeOp.get(ModelDescriptionConstants.OP_ADDR).set(
                PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, ConfigAdminExtension.SUBSYSTEM_NAME)).toModelNode());
        List<ModelNode> operations = checkResultAndGetContents(servicesA.executeOperation(describeOp)).asList();

        KernelServices servicesB = installInController(new AdditionalInitialization() {
            @Override
            public Type getType() {
                return Type.MANAGEMENT;
            }
        }, operations);
        ModelNode modelB = servicesB.readWholeModel();

        compare(modelA, modelB);
    }

    private void assertOSGiSubsystemAddress(ModelNode address) {
        PathAddress addr = PathAddress.pathAddress(address);
        PathElement element = addr.getElement(0);
        Assert.assertEquals(ModelDescriptionConstants.SUBSYSTEM, element.getKey());
        Assert.assertEquals(ConfigAdminExtension.SUBSYSTEM_NAME, element.getValue());
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization(){
            @Override
            protected Type getType() {
                return Type.MANAGEMENT;
            }
        };
    }
}
