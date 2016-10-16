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

package org.jboss.as.test.integration.ee.injection.resource.substitution;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.logging.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MapMessage;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the Resource injection with substitution works as expected
 *
 * @author wangchao
 */
@RunWith(Arquillian.class)
@ServerSetup({ ResourceInjectionSubstitutionTestCase.SystemPropertySetup.class })
public class ResourceInjectionSubstitutionTestCase {

    private static final Logger logger = Logger.getLogger(ResourceInjectionSubstitutionTestCase.class.getName());

    @ArquillianResource
    InitialContext ctx;

    private SimpleSLSB slsb;
    private SimpleSFSB sfsb;

    static class SystemPropertySetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {

            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient);
            jmsAdminOperations.createJmsQueue("queue/testQueue", "java:jboss/queue/testQueue");

            final ModelNode enableSubstitutionOp = new ModelNode();
            enableSubstitutionOp.get(OP_ADDR).set(SUBSYSTEM, "ee");
            enableSubstitutionOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            enableSubstitutionOp.get(NAME).set("annotation-property-replacement");
            enableSubstitutionOp.get(VALUE).set(true);

            // @Resource(name="${resource.name}")
            final ModelNode setResourceNameOp = new ModelNode();
            setResourceNameOp.get(ClientConstants.OP).set(ClientConstants.ADD);
            setResourceNameOp.get(ClientConstants.OP_ADDR).add("system-property", "resource.name");
            setResourceNameOp.get("value").set("simpleString");

            // @Resource(lookup = "${resource.lookup}")
            final ModelNode setResourceLookupOp = new ModelNode();
            setResourceLookupOp.get(ClientConstants.OP).set(ClientConstants.ADD);
            setResourceLookupOp.get(ClientConstants.OP_ADDR).add("system-property", "resource.lookup");
            setResourceLookupOp.get("value").set("java:comp/env/ResourceFromWebXml");

            // @Resource(mappedName="${resource.mappedname}")
            final ModelNode setResourceMappedNameOp = new ModelNode();
            setResourceMappedNameOp.get(ClientConstants.OP).set(ClientConstants.ADD);
            setResourceMappedNameOp.get(ClientConstants.OP_ADDR).add("system-property", "resource.mappedname");
            setResourceMappedNameOp.get("value").set("java:comp/env/ResourceFromWebXml");

            // @Resource(mappedName = "${resource.mappedname.connectionfactory}")
            final ModelNode setResourceMappedNameConnectionFactoryOp = new ModelNode();
            setResourceMappedNameConnectionFactoryOp.get(ClientConstants.OP).set(ClientConstants.ADD);
            setResourceMappedNameConnectionFactoryOp.get(ClientConstants.OP_ADDR).add("system-property",
                    "resource.mappedname.connectionfactory");
            setResourceMappedNameConnectionFactoryOp.get("value").set("java:/ConnectionFactory");

            try {
                applyUpdate(managementClient, enableSubstitutionOp);
                applyUpdate(managementClient, setResourceNameOp);
                applyUpdate(managementClient, setResourceLookupOp);
                applyUpdate(managementClient, setResourceMappedNameOp);
                applyUpdate(managementClient, setResourceMappedNameConnectionFactoryOp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {

            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue("queue/testQueue");
                jmsAdminOperations.close();
            }

            // @Resource(name="${resource.name}")
            final ModelNode removeResourceNameOp = new ModelNode();
            removeResourceNameOp.get(ClientConstants.OP).set("remove");
            removeResourceNameOp.get(ClientConstants.OP_ADDR).add("system-property", "resource.name");

            // @Resource(lookup = "${resource.lookup}")
            final ModelNode removeResourceLookupOp = new ModelNode();
            removeResourceLookupOp.get(ClientConstants.OP).set("remove");
            removeResourceLookupOp.get(ClientConstants.OP_ADDR).add("system-property", "resource.lookup");

            // @Resource(mappedName="${resource.mappedname}")
            final ModelNode removeResourceMappedNameOp = new ModelNode();
            removeResourceMappedNameOp.get(ClientConstants.OP).set("remove");
            removeResourceMappedNameOp.get(ClientConstants.OP_ADDR).add("system-property", "resource.mappedname");

            // @Resource(mappedName = "${resource.mappedname.conncetionfactory}")
            final ModelNode removeResourceMappedNameConnectionFactoryOp = new ModelNode();
            removeResourceMappedNameConnectionFactoryOp.get(ClientConstants.OP).set("remove");
            removeResourceMappedNameConnectionFactoryOp.get(ClientConstants.OP_ADDR).add("system-property", "resource.mappedname.conncetionfactory");

            final ModelNode disableSubstitutionOp = new ModelNode();
            disableSubstitutionOp.get(OP_ADDR).set(SUBSYSTEM, "ee");
            disableSubstitutionOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            disableSubstitutionOp.get(NAME).set("annotation-property-replacement");
            disableSubstitutionOp.get(VALUE).set(false);

            try {
                applyUpdate(managementClient, removeResourceNameOp);
                applyUpdate(managementClient, removeResourceLookupOp);
                applyUpdate(managementClient, removeResourceMappedNameOp);
                applyUpdate(managementClient, removeResourceMappedNameConnectionFactoryOp);
                applyUpdate(managementClient, disableSubstitutionOp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void applyUpdate(final ManagementClient managementClient, final ModelNode update) throws Exception {
            ModelNode result = managementClient.getControllerClient().execute(update);
            if (result.hasDefined(ClientConstants.OUTCOME)
                    && ClientConstants.SUCCESS.equals(result.get(ClientConstants.OUTCOME).asString())) {
            } else if (result.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
                final String failureDesc = result.get(ClientConstants.FAILURE_DESCRIPTION).toString();
                throw new RuntimeException(failureDesc);
            } else {
                throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
            }
        }
    }

    @Before
    public void beforeTest() throws Exception {
        Context ctx = new InitialContext();
        slsb = (SimpleSLSB) ctx.lookup("java:module/" + SimpleSLSB.class.getSimpleName() + "!" + SimpleSLSB.class.getName());
        sfsb = (SimpleSFSB) ctx.lookup("java:module/" + SimpleSFSB.class.getSimpleName() + "!" + SimpleSFSB.class.getName());
    }

    @Deployment
    public static WebArchive createWebDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "resource-injection-substitution-test.war");
        war.addPackage(SimpleSLSB.class.getPackage()).addPackage(JMSOperations.class.getPackage());
        war.addAsWebInfResource(ResourceInjectionSubstitutionTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }

    /**
     * Test resource injection with SLSB
     */
    @Test
    public void testResourceInjectionSubstitutionSlsb() {
        Assert.assertTrue("@Resource with name wasn't injected in SLSB", slsb.isResourceWithNameInjected());
        Assert.assertTrue("@Resource with lookup wasn't injected in SLSB", slsb.isResourceWithLookupNameInjected());
        Assert.assertTrue("@Resource with mappedName wasn't injected in SLSB", slsb.isResourceWithMappedNameInjected());
    }

    /**
     * Test resource injection with SFSB
     */
    @Test
    public void testResourceInjectionSubstitutionSfsb() {
        Assert.assertTrue("@Resource with name wasn't injected in SFSB", sfsb.isResourceWithNameInjected());
        Assert.assertTrue("@Resource with lookup wasn't injected in SFSB", sfsb.isResourceWithLookupNameInjected());
        Assert.assertTrue("@Resource with mappedName wasn't injected in SFSB", sfsb.isResourceWithMappedNameInjected());
    }

    /**
     * Test resource injection with MDB
     */
    @Test
    public void testResourceInjectionSubstitutionMdb() throws Exception {
        // ConnectionFactory and Reply message are injected in SimpleMDB
        ConnectionFactory factory = (ConnectionFactory) ctx.lookup("ConnectionFactory");
        Connection con = factory.createConnection();
        try {
            Destination dest = (Destination) ctx.lookup("java:jboss/queue/testQueue");

            Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(dest);

            Queue replyQueue = session.createTemporaryQueue();
            MessageConsumer consumer = session.createConsumer(replyQueue);

            con.start();

            TextMessage msg = session.createTextMessage();
            msg.setJMSReplyTo(replyQueue);
            msg.setText("This is message one");
            producer.send(msg);

            MapMessage replyMsg = (MapMessage) consumer.receive(5000);
            Assert.assertNotNull(replyMsg);
            Assert.assertEquals("It's Friday!!!", replyMsg.getString("replyMsg"));
        } finally {
            con.close();
        }
    }
}
