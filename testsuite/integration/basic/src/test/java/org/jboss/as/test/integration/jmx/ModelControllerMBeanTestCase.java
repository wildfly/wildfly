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
package org.jboss.as.test.integration.jmx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.jmx.model.Constants;
import org.jboss.as.test.integration.jmx.sar.TestMBean;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@Ignore("AS7-2741")
public class ModelControllerMBeanTestCase {

    static final String HOST = "localhost";
    static final int PORT = 1090;

    static final ObjectName MODEL_FILTER = createObjectName(Constants.DOMAIN  + ":*");
    static final ObjectName ROOT_MODEL_NAME = Constants.ROOT_MODEL_NAME;

    static JMXConnector connector;
    static MBeanServerConnection connection;
    static ModelControllerClient client;

    @BeforeClass
    public static void initialize() throws Exception {
        client = ModelControllerClient.Factory.create("localhost", 9999, getCallbackHandler());
        enableJMXConnector(client);
        connection = setupAndGetConnection();
    }

    private static void enableJMXConnector(ModelControllerClient client) throws IOException {
        final ModelNode connector = new ModelNode();
        connector.get(OP).set(ADD);
        connector.get(OP_ADDR).add(SUBSYSTEM, "jmx").add("connector", "jmx");
        connector.get("server-binding").set("jmx-connector-server");
        connector.get("registry-binding").set("jmx-connector-registry");
        ModelNode result = client.execute(connector);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    }

    @AfterClass
    public static void closeConnection() throws Exception {
        IoUtils.safeClose(connector);
        disableJMXConnector(client);
        IoUtils.safeClose(client);
    }

    private static void disableJMXConnector(ModelControllerClient client) throws IOException {
        ModelNode op = new ModelNode();
        op.get(OP).set("remove");
        op.get(OP_ADDR).add(SUBSYSTEM, "jmx").add("connector", "jmx");
        ModelNode result = client.execute(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    }

    /**
     * Test that all the MBean infos can be read properly
     */
    @Test
    public void testAllMBeanInfos() throws Exception {
        Set<ObjectName> names = connection.queryNames(MODEL_FILTER, null);
        Map<ObjectName, Exception> failedInfos = new HashMap<ObjectName, Exception>();

        for (ObjectName name : names) {
            try {
                Assert.assertNotNull(connection.getMBeanInfo(name));
            } catch (Exception e) {
                System.out.println("Error getting info for " + name);
                failedInfos.put(name, e);
            }
        }
        Assert.assertTrue(failedInfos.toString(), failedInfos.isEmpty());
    }

    @Test
    public void testSystemProperties() throws Exception {
        String[] initialNames = getSystemPropertyNames();

        ObjectName testName = new ObjectName(Constants.DOMAIN + ":system-property=mbeantest");
        assertNoMBean(testName);

        connection.invoke(ROOT_MODEL_NAME, "addSystemProperty", new Object[] {"mbeantest", "800"}, new String[] {String.class.getName(), String.class.getName()});
        try {
            String[] newNames = getSystemPropertyNames();
            Assert.assertEquals(initialNames.length + 1, newNames.length);
            boolean found = false;
            for (String s : newNames) {
                if (s.equals("mbeantest")) {
                    found = true;
                    break;
                }
            };
            Assert.assertTrue(found);
            Assert.assertNotNull(connection.getMBeanInfo(new ObjectName(Constants.DOMAIN + ":system-property=mbeantest")));
        } finally {
            connection.invoke(new ObjectName(Constants.DOMAIN + ":system-property=mbeantest"), "remove", new Object[0], new String[0]);
        }

        assertNoMBean(testName);

        Assert.assertEquals(initialNames.length, getSystemPropertyNames().length);
    }

    @Test
    public void testDeploymentViaJmx() throws Exception {
        ObjectName testSarMBeanName = new ObjectName("jboss:name=test,type=jmx-sar");
        ObjectName testDeploymentModelName = new ObjectName("jboss.model:deployment=test-jmx-sar.sar");

        assertNoMBean(testSarMBeanName);
        assertNoMBean(testDeploymentModelName);

        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "test-jmx-sar.sar");
        sar.addClasses(org.jboss.as.test.integration.jmx.sar.Test.class, TestMBean.class);
        sar.addAsManifestResource("jmx-sar/jboss-service.xml", "jboss-service.xml");

        InputStream in = sar.as(ZipExporter.class).exportAsInputStream();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int i = in.read();
        while (i != -1) {
            bout.write(i);
            i = in.read();
        }

        byte[] bytes = bout.toByteArray();

        //Upload the content
        byte[] hash = (byte[])connection.invoke(ROOT_MODEL_NAME, "uploadDeploymentBytes", new Object[] {bytes}, new String[] {byte.class.getName()});


        //Do all this to create the composite type
        CompositeType contentType = null;
        MBeanInfo info = connection.getMBeanInfo(ROOT_MODEL_NAME);
        for (MBeanOperationInfo op : info.getOperations()) {
            if (op.getName().equals("addDeployment")) {
                contentType = (CompositeType)((ArrayType<CompositeType>)((OpenMBeanParameterInfo)op.getSignature()[2]).getOpenType()).getElementOpenType();
                break;
            }
        }
        Map<String, Object> values = new HashMap<String, Object>();
        for (String key : contentType.keySet()) {
            values.put(key, null);
        }
        values.put("hash", hash);
        CompositeData contents = new CompositeDataSupport(contentType, values);

        //Deploy it
        connection.invoke(ROOT_MODEL_NAME,
                "addDeployment",
                new Object[] {"test-jmx-sar.sar", "test-jmx-sar.sar", new CompositeData[] {contents}, Boolean.TRUE},
                new String[] {String.class.getName(), String.class.getName(), CompositeData.class.getName(), Boolean.class.getName()});

        //Make sure the test deployment mbean and the management model mbean for the deployment are there
        Assert.assertNotNull(connection.getMBeanInfo(testSarMBeanName));
        Assert.assertTrue((Boolean)connection.getAttribute(testDeploymentModelName, "enabled"));

        //Undeploy
        connection.invoke(testDeploymentModelName, "undeploy", new Object[0], new String[0]);

        //Check the app was undeployed
        assertNoMBean(testSarMBeanName);
        Assert.assertFalse((Boolean)connection.getAttribute(testDeploymentModelName, "enabled"));

        //Remove
        connection.invoke(testDeploymentModelName, "remove", new Object[0], new String[0]);
        assertNoMBean(testDeploymentModelName);
    }

    private void assertNoMBean(ObjectName name) throws Exception {
        try {
            connection.getMBeanInfo(name);
            Assert.fail("Should not have found mbean with nane " + name);
        } catch (InstanceNotFoundException expected) {
        }
    }

    private String[] getSystemPropertyNames() throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
        op.get(OP_ADDR).setEmptyList();
        op.get(CHILD_TYPE).set("system-property");

        ModelNode result = client.execute(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        List<ModelNode> propertyNames = result.get(RESULT).asList();
        String[] names = new String[propertyNames.size()];
        int i = 0;
        for (ModelNode node : propertyNames) {
            names[i++] = node.asString();
        }
        return names;
    }



    private static ObjectName createObjectName(String name) {
        try {
            return ObjectName.getInstance(name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static MBeanServerConnection setupAndGetConnection() throws Exception {
        // Make sure that we can connect to the MBean server
        String urlString = System
                .getProperty("jmx.service.url", "service:jmx:remoting-jmx://" + HOST + ":" + PORT);
        JMXServiceURL serviceURL = new JMXServiceURL(urlString);
        connector = JMXConnectorFactory.connect(serviceURL, null);
        return connector.getMBeanServerConnection();
    }

}
