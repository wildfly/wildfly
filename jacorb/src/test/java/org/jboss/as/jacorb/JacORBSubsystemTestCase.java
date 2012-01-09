/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jacorb;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import junit.framework.Assert;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * <ṕ>
 * JacORB subsystem tests.
 * </ṕ>
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 */
public class JacORBSubsystemTestCase extends AbstractSubsystemBaseTest {

    public JacORBSubsystemTestCase() {
        super(JacORBExtension.SUBSYSTEM_NAME, new JacORBExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        // for the standard subsystem test we use a complete configuration XML.
        return
        "<subsystem xmlns=\"urn:jboss:domain:jacorb:1.1\">" +
        "    <orb name=\"JBoss\" print-version=\"off\" use-imr=\"off\" use-bom=\"off\"  cache-typecodes=\"off\" " +
        "        cache-poa-names=\"off\" giop-minor-version =\"2\">" +
        "        <connection retries=\"5\" retry-interval=\"500\" client-timeout=\"0\" server-timeout=\"0\" " +
        "            max-server-connections=\"500\" max-managed-buf-size=\"24\" outbuf-size=\"2048\" " +
        "            outbuf-cache-timeout=\"-1\"/>" +
        "        <initializers security=\"on\" transactions=\"on\"/>" +
        "    </orb>" +
        "    <poa monitoring=\"off\" queue-wait=\"on\" queue-min=\"10\" queue-max=\"100\">" +
        "        <request-processors pool-size=\"10\" max-threads=\"32\"/>" +
        "    </poa>" +
        "    <naming root-context=\"JBoss/Naming/root\" export-corbaloc=\"on\"/>" +
        "    <interop sun=\"on\" comet=\"off\" iona=\"off\" chunk-custom-rmi-valuetypes=\"on\" " +
        "        lax-boolean-encoding=\"off\" indirection-encoding-disable=\"off\" strict-check-on-tc-creation=\"off\"/>" +
        "    <security support-ssl=\"off\" add-component-via-interceptor=\"on\" client-supports=\"60\"" +
        "        client-requires=\"0\" server-supports=\"60\" server-requires=\"0\" use-domain-socket-factory=\"off\"" +
        "        use-domain-server-socket-factory=\"off\"/>" +
        "    <properties>" +
        "        <property name=\"some_property\" value=\"some_value\"/>" +
        "    </properties>" +
        "</subsystem>";
    }

    @Test
    public void testParseEmptySubsystem() throws Exception {
        // parse the subsystem xml into operations.
        String subsystemXml =
                "<subsystem xmlns=\"" + JacORBSubsystemParser.Namespace.CURRENT.getUriString() + "\">" +
                "</subsystem>";
        List<ModelNode> operations = super.parse(subsystemXml);

        // check that we have the expected number of operations.
        Assert.assertEquals(1, operations.size());

        // check that each operation has the correct content.
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(JacORBExtension.SUBSYSTEM_NAME, element.getValue());
    }

    @Test
    public void testParseSubsystemWithBadChild() throws Exception {
        // try parsing a XML with an invalid element.
        String subsystemXml =
                "<subsystem xmlns=\"" + JacORBSubsystemParser.Namespace.CURRENT.getUriString() + "\">" +
                "   <invalid/>" +
                "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed bad child");
        } catch (XMLStreamException expected) {
        }

        // now try parsing a valid element in an invalid position.
        subsystemXml =
                "<subsystem xmlns=\"urn:jboss:domain:jacorb:1.0\">" +
                "    <orb name=\"JBoss\" print-version=\"off\">" +
                "        <poa/>" +
                "    </orb>" +
                "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed bad child");
        } catch (XMLStreamException expected) {
        }

    }

    @Test
    public void testParseSubsystemWithBadAttribute() throws Exception {
        String subsystemXml =
                "<subsystem xmlns=\"" + JacORBSubsystemParser.Namespace.CURRENT.getUriString() + "\" bad=\"very_bad\">" +
                "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed bad attribute");
        } catch (XMLStreamException expected) {
        }
    }

   /* @Test
    public void testDescribeHandler() throws Exception {
        // parse the subsystem xml and install into the first controller.
        String subsystemXml =
                "<subsystem xmlns=\"" + JacORBSubsystemParser.Namespace.CURRENT.getUriString() + "\">" +
                "</subsystem>";

        AdditionalInitialization additionalInit = new AdditionalInitialization(){
            @Override
            protected void setupController(ControllerInitializer controllerInitializer) {
                controllerInitializer.addSocketBinding("jacorb", 3528);
                controllerInitializer.addSocketBinding("jacorb-ssl", 3529);
            }
        };

        KernelServices servicesA = super.installInController(additionalInit, subsystemXml);
        // get the model and the describe operations from the first controller.
        ModelNode modelA = servicesA.readWholeModel();
        ModelNode describeOp = new ModelNode();
        describeOp.get(OP).set(DESCRIBE);
        describeOp.get(OP_ADDR).set(
                PathAddress.pathAddress(
                        PathElement.pathElement(SUBSYSTEM, JacORBExtension.SUBSYSTEM_NAME)).toModelNode());
        List<ModelNode> operations = super.checkResultAndGetContents(servicesA.executeOperation(describeOp)).asList();
        servicesA.shutdown();

        Assert.assertEquals(1, operations.size());

        // install the describe options from the first controller into a second controller.
        KernelServices servicesB = super.installInController(additionalInit, operations);
        ModelNode modelB = servicesB.readWholeModel();
        servicesB.shutdown();

        // make sure the models from the two controllers are identical.
        super.compare(modelA, modelB);

    }*/

    // Tests for the version 1.0 of the JacORB subsystem configuration.

    @Test
    public void testParseSubsystem_1_0() throws Exception {
        String subsystemXml = "<subsystem xmlns=\"urn:jboss:domain:jacorb:1.0\">" +
        "    <orb name=\"JBoss\" print-version=\"off\">" +
        "        <connection retries=\"5\" retry-interval=\"500\" client-timeout=\"0\" server-timeout=\"0\"/>" +
        "        <naming root-context=\"JBoss/Naming/root\" export-corbaloc=\"on\"/>" +
        "    </orb>" +
        "    <poa monitoring=\"off\" queue-wait=\"on\" queue-min=\"10\" queue-max=\"100\">" +
        "        <request-processors pool-size=\"10\" max-threads=\"32\"/>" +
        "    </poa>" +
        "    <interop sun=\"on\" comet=\"off\" chunk-custom-rmi-valuetypes=\"on\"/>" +
        "    <security support-ssl=\"off\" use-domain-socket-factory=\"off\" use-domain-server-socket-factory=\"off\"/>" +
        "    <property key=\"a\" value=\"va\"/>" +
        "    <property key=\"b\" value=\"vb\"/>" +
        "    <initializers>security,transactions</initializers>" +
        "</subsystem>";

        List<ModelNode> operations = super.parse(subsystemXml);

        // check that we have the expected number of operations.
        Assert.assertEquals(1, operations.size());
        // check that each operation has the correct content.
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(JacORBExtension.SUBSYSTEM_NAME, element.getValue());
    }

    @Test
    public void testParseEmptySubsystem_1_0() throws Exception {
        // parse the subsystem xml into operations.
        String subsystemXml =
                "<subsystem xmlns=\"" + JacORBSubsystemParser.Namespace.JacORB_1_0.getUriString() + "\">" +
                "</subsystem>";
        List<ModelNode> operations = super.parse(subsystemXml);

        // check that we have the expected number of operations.
        Assert.assertEquals(1, operations.size());

        // check that each operation has the correct content.
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(JacORBExtension.SUBSYSTEM_NAME, element.getValue());
    }

    @Test
    public void testParseSubsystemWithBadInitializer_1_0() throws Exception {
        // try parsing a XML with an invalid element.
        String subsystemXml =
                "<subsystem xmlns=\"" + JacORBSubsystemParser.Namespace.CURRENT.getUriString() + "\">" +
                "   <initializers>invalid</initializers>" +
                "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed bad initializer");
        } catch (XMLStreamException expected) {
        }
    }
}
