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
package org.jboss.as.jmx;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.xml.stream.XMLStreamException;

import junit.framework.Assert;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JMXSubsystemTestCase extends AbstractSubsystemTest {

    public JMXSubsystemTestCase() {
        super(JMXExtension.SUBSYSTEM_NAME, new JMXExtension());
    }

    @Test
    public void testParseEmptySubsystem() throws Exception {
        //Parse the subsystem xml into operations
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "</subsystem>";
        List<ModelNode> operations = super.parse(subsystemXml);

        ///Check that we have the expected number of operations
        Assert.assertEquals(1, operations.size());

        //Check that each operation has the correct content
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(JMXExtension.SUBSYSTEM_NAME, element.getValue());
    }

    @Test
    public void testParseSubsystemWithBadChild() throws Exception {
        //Parse the subsystem xml into operations
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "   <invalid/>" +
                "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed bad child");
        } catch (XMLStreamException expected) {
        }
    }

    @Test
    public void testParseSubsystemWithBadAttribute() throws Exception {
        //Parse the subsystem xml into operations
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\" bad=\"very_bad\">" +
                "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed bad attribute");
        } catch (XMLStreamException expected) {
        }
    }

    @Test
    public void testParseSubsystemWithConnector() throws Exception {
        //Parse the subsystem xml into operations
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "    <jmx-connector registry-binding=\"jmx-connector-registry\" server-binding=\"jmx-connector-server\" />" +
                "</subsystem>";
        List<ModelNode> operations = super.parse(subsystemXml);

        ///Check that we have the expected number of operations
        Assert.assertEquals(2, operations.size());

        //Check that each operation has the correct content
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        assertJmxSubsystemAddress(addSubsystem.get(OP_ADDR));

        ModelNode addConnector = operations.get(1);
        Assert.assertEquals(JMXConnectorAdd.OPERATION_NAME, addConnector.get(OP).asString());
        assertJmxSubsystemAddress(addConnector.get(OP_ADDR));
        Assert.assertEquals("jmx-connector-registry", addConnector.get(CommonAttributes.REGISTRY_BINDING).asString());
        Assert.assertEquals("jmx-connector-server", addConnector.get(CommonAttributes.SERVER_BINDING).asString());
    }

    @Test
    public void testParseSubsystemWithTwoConnectors() throws Exception {
        //Parse the subsystem xml into operations
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "    <jmx-connector registry-binding=\"jmx-connector-registry1\" server-binding=\"jmx-connector-server1\" />" +
                "    <jmx-connector registry-binding=\"jmx-connector-registry2\" server-binding=\"jmx-connector-server2\" />" +
                "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed second connector");
        } catch (XMLStreamException expected) {
        }
    }

    @Test
    public void testParseSubsystemWithBadConnectorAttribute() throws Exception {
        //Parse the subsystem xml into operations
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "    <jmx-connector registry-binding=\"jmx-connector-registry\" server-binding=\"jmx-connector-server\" bad=\"verybad\"/>" +
                "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed bad attribute");
        } catch (XMLStreamException expected) {
        }
    }

    @Test
    public void testParseSubsystem1_0WithBadPasswordFile() throws Exception {
        //Parse the subsystem xml into operations
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.JMX_1_0.getUriString() + "\">" +
                "    <jmx-connector registry-binding=\"registry1\" server-binding=\"server1\" password-file=\"passwords\" />" +
                "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed bad attribute");
        } catch (XMLStreamException expected) {
        }
    }

    @Test
    public void testInstallIntoController() throws Exception {
        //Parse the subsystem xml and install into the controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "    <jmx-connector registry-binding=\"registry\" server-binding=\"server\" />" +
                "</subsystem>";
        KernelServices services = super.installInController(
                new AdditionalInitialization() {

                    @Override
                    protected void setupController(ControllerInitializer controllerInitializer) {
                        controllerInitializer.addSocketBinding("registry", 12345);
                        controllerInitializer.addSocketBinding("server", 12346);
                    }

                },subsystemXml);

        //Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(JMXExtension.SUBSYSTEM_NAME));
        Assert.assertEquals("registry", model.get(SUBSYSTEM, JMXExtension.SUBSYSTEM_NAME).require(CommonAttributes.REGISTRY_BINDING).asString());
        Assert.assertEquals("server", model.get(SUBSYSTEM, JMXExtension.SUBSYSTEM_NAME).require(CommonAttributes.SERVER_BINDING).asString());

    }


    @Test
    public void testParseAndMarshalModel1_0() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.JMX_1_0.getUriString() + "\">" +
                "    <jmx-connector registry-binding=\"registry1\" server-binding=\"server1\" />" +
                "</subsystem>";

        AdditionalInitialization additionalInit = new AdditionalInitialization(){
            @Override
            protected void setupController(ControllerInitializer controllerInitializer) {
                controllerInitializer.addSocketBinding("registry1", 12345);
                controllerInitializer.addSocketBinding("server1", 12346);
            }
        };

        KernelServices servicesA = super.installInController(additionalInit, subsystemXml);
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();

        System.out.println(marshalled);

        Assert.assertEquals(normalizeXML(subsystemXml), normalizeXML(marshalled));

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = super.installInController(additionalInit, marshalled);
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    @Test
    public void testParseAndMarshalModel1_1() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.JMX_1_1.getUriString() + "\">" +
                "    <jmx-connector registry-binding=\"registry1\" server-binding=\"server1\" />" +
                "</subsystem>";

        AdditionalInitialization additionalInit = new AdditionalInitialization(){
            @Override
            protected void setupController(ControllerInitializer controllerInitializer) {
                controllerInitializer.addSocketBinding("registry1", 12345);
                controllerInitializer.addSocketBinding("server1", 12346);
            }
        };

        KernelServices servicesA = super.installInController(additionalInit, subsystemXml);
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();

        Assert.assertEquals(normalizeXML(subsystemXml), normalizeXML(marshalled));

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = super.installInController(additionalInit, marshalled);
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    @Test
    public void testParseAndMarshalModelWithSecurity() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "    <jmx-connector registry-binding=\"registry1\" server-binding=\"server1\" password-file=\"jmxremote.password\" access-file=\"jmxremote.access\" />" +
                "</subsystem>";


        AdditionalInitialization additionalInit = new AdditionalInitialization(){
            @Override
            protected void setupController(ControllerInitializer controllerInitializer) {
                controllerInitializer.addSocketBinding("registry1", 12345);
                controllerInitializer.addSocketBinding("server1", 12346);
            }
        };

        KernelServices servicesA = super.installInController(additionalInit, subsystemXml);
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();

        Assert.assertEquals(normalizeXML(subsystemXml), normalizeXML(marshalled));

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = super.installInController(additionalInit, marshalled);
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    @Test
    public void testParseAndMarshalModelWithEnvVars() throws Exception {
        File currentDir = new File(".");
        String path=currentDir.getAbsolutePath();
        System.setProperty("security.config.path", path); 
        System.setProperty("accessFile", "jmxremote.access");

        //Parse the subsystem xml and install into the first controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "    <jmx-connector registry-binding=\"registry1\" server-binding=\"server1\" password-file=\"${security.config.path}/jmxremote.password\" access-file=\"" + path + "/${accessFile}\" />" +
                "</subsystem>";


        AdditionalInitialization additionalInit = new AdditionalInitialization(){
            @Override
            protected void setupController(ControllerInitializer controllerInitializer) {
                controllerInitializer.addSocketBinding("registry1", 12345);
                controllerInitializer.addSocketBinding("server1", 12346);
            }
        };

        KernelServices servicesA = super.installInController(additionalInit, subsystemXml);
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();

        Assert.assertEquals(normalizeXML(subsystemXml), normalizeXML(marshalled));

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = super.installInController(additionalInit, marshalled);
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    @Test
    public void testDescribeHandler() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "    <jmx-connector registry-binding=\"registry1\" server-binding=\"server1\" />" +
                "</subsystem>";
        KernelServices servicesA = super.installInController(subsystemXml);
        //Get the model and the describe operations from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        ModelNode describeOp = new ModelNode();
        describeOp.get(OP).set(DESCRIBE);
        describeOp.get(OP_ADDR).set(
                PathAddress.pathAddress(
                        PathElement.pathElement(SUBSYSTEM, JMXExtension.SUBSYSTEM_NAME)).toModelNode());
        List<ModelNode> operations = super.checkResultAndGetContents(servicesA.executeOperation(describeOp)).asList();
        servicesA.shutdown();

        Assert.assertEquals(2, operations.size());


        //Install the describe options from the first controller into a second controller
        KernelServices servicesB = super.installInController(operations);
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);

    }


    @Test
    public void testConnectToUnsecuredMBeanServer() throws Exception {
        //Parse the subsystem xml and install into the controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "    <jmx-connector registry-binding=\"registry\" server-binding=\"server\" />" +
                "</subsystem>";
        KernelServices services = super.installInController(
                new AdditionalInitialization() {

                    @Override
                    protected void setupController(ControllerInitializer controllerInitializer) {
                        controllerInitializer.addSocketBinding("registry", 12345);
                        controllerInitializer.addSocketBinding("server", 12346);
                    }

                },subsystemXml);

        //Make sure that we can connect to the MBean server
        String host = "localhost";
        int port = 12345;
        String urlString = System.getProperty("jmx.service.url",
            "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi");
        JMXServiceURL serviceURL = new JMXServiceURL(urlString);

        
        // TODO this is horrible - for some reason after the first test the
        // second time we
        // start the JMX connector it takes time for it to appear
        long end = System.currentTimeMillis() + 10000;
        while (true) {
            try {
        	    JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, null);
	            MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();
	            Assert.assertTrue(connection.getMBeanCount() > 0);
	            jmxConnector.close();
	            return;
            } catch (Exception e) {
                if (System.currentTimeMillis() >= end) {
                    services.shutdown();
                    throw new RuntimeException(e);
                }
                Thread.sleep(50);
            }
        }
    }

    @Test
    public void testConnectUnsecurlyToSecuredMBeanServer() throws Exception {
        //Parse the subsystem xml and install into the controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "    <jmx-connector registry-binding=\"registry\" server-binding=\"server\" password-file=\"jmxremote.password\" access-file=\"jmxremote.access\" />" +
                "</subsystem>";
        KernelServices services = super.installInController(
                new AdditionalInitialization() {

                    @Override
                    protected void setupController(ControllerInitializer controllerInitializer) {
                        controllerInitializer.addSocketBinding("registry", 12345);
                        controllerInitializer.addSocketBinding("server", 12346);
                    }

                },subsystemXml);

        //Make sure that we can connect to the MBean server
        String host = "localhost";
        int port = 12345;
        String urlString = System.getProperty("jmx.service.url",
            "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi");
        JMXServiceURL serviceURL = new JMXServiceURL(urlString);

        // TODO this is horrible - for some reason after the first test the
        // second time we
        // start the JMX connector it takes time for it to appear
        long end = System.currentTimeMillis() + 10000;
        while (true) {
            try {
                JMXConnectorFactory.connect(serviceURL, null);
                Assert.fail("Should never get here!");
                return;
            } catch (SecurityException e) {
        	    //expected
        	    return;
            } catch (Exception e) {
                if (System.currentTimeMillis() >= end) {
                    services.shutdown();
                	throw new RuntimeException(e);
                }
                Thread.sleep(50);
            }
        }
    }

    @Test
    public void testConnectSecurlyToSecuredMBeanServer() throws Exception {
        //Parse the subsystem xml and install into the controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "    <jmx-connector registry-binding=\"registry\" server-binding=\"server\" password-file=\"jmxremote.password\" access-file=\"jmxremote.access\" />" +
                "</subsystem>";
        KernelServices services = super.installInController(
                new AdditionalInitialization() {

                    @Override
                    protected void setupController(ControllerInitializer controllerInitializer) {
                        controllerInitializer.addSocketBinding("registry", 12345);
                        controllerInitializer.addSocketBinding("server", 12346);
                    }

                },subsystemXml);

        //Make sure that we can connect to the MBean server
    	final String username = "controlRole";
    	final String password = "R&D";
        String host = "localhost";
        int port = 12345;
        String urlString = System.getProperty("jmx.service.url",
            "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi");
        JMXServiceURL serviceURL = new JMXServiceURL(urlString);

    	HashMap env = new HashMap(); 
        String[] credentials = new String[] { username , password }; 
        env.put("jmx.remote.credentials", credentials); 
        
        // TODO this is horrible - for some reason after the first test the
        // second time we
        // start the JMX connector it takes time for it to appear
        long end = System.currentTimeMillis() + 10000;
        while (true) {
            try {
                JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, env);
	            MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();
	            Assert.assertTrue(connection.getMBeanCount() > 0);
	            jmxConnector.close();
	            return;
            } catch (Exception e) {
                if (System.currentTimeMillis() >= end) {
                    services.shutdown();
                	throw new RuntimeException(e);
                }
                Thread.sleep(50);
            }
        }
    }

    @Test
    public void testUseEnvVariableInSecurityAttr() throws Exception {
        File currentDir = new File(".");
        String path=currentDir.getAbsolutePath();
        System.setProperty("security.config.path", path); 
        System.setProperty("accessFile", "jmxremote.access");

        //Parse the subsystem xml and install into the controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "    <jmx-connector registry-binding=\"registry\" server-binding=\"server\" password-file=\"${security.config.path}/jmxremote.password\" access-file=\"" + path + "/${accessFile}\" />" +
                "</subsystem>";
        KernelServices services = super.installInController(
                new AdditionalInitialization() {

                    @Override
                    protected void setupController(ControllerInitializer controllerInitializer) {
                        controllerInitializer.addSocketBinding("registry", 12345);
                        controllerInitializer.addSocketBinding("server", 12346);
                    }

                },subsystemXml);

        //Make sure that we can connect to the MBean server
    	final String username = "controlRole";
    	final String password = "R&D";
        String host = "localhost";
        int port = 12345;
        String urlString = System.getProperty("jmx.service.url",
            "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi");
        JMXServiceURL serviceURL = new JMXServiceURL(urlString);

    	HashMap env = new HashMap(); 
        String[] credentials = new String[] { username , password }; 
        env.put("jmx.remote.credentials", credentials); 
        
        // TODO this is horrible - for some reason after the first test the
        // second time we
        // start the JMX connector it takes time for it to appear
        long end = System.currentTimeMillis() + 10000;
        while (true) {
            try {
                JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, env);
	            MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();
	            Assert.assertTrue(connection.getMBeanCount() > 0);
	            jmxConnector.close();
	            return;
            } catch (Exception e) {
                if (System.currentTimeMillis() >= end) {
                    services.shutdown();
                	throw new RuntimeException(e);
                }
                Thread.sleep(50);
            }
        }
    }

    private void assertJmxSubsystemAddress(ModelNode address) {
        PathAddress addr = PathAddress.pathAddress(address);
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(JMXExtension.SUBSYSTEM_NAME, element.getValue());
    }
}
