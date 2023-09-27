/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.ejb2x;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.jms.Message;
import jakarta.jms.Queue;
import javax.naming.InitialContext;
import java.util.PropertyPermission;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

/**
 * Tests EJB2.1 MDB deployments.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({MDB21TestCase.JmsQueueSetup.class})
public class MDB21TestCase extends AbstractMDB2xTestCase {

    private Queue queue;
    private Queue replyQueue;

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsAdminOperations.createJmsQueue("ejb2x/queue", "java:jboss/ejb2x/queue");
            jmsAdminOperations.createJmsQueue("ejb2x/replyQueue", "java:jboss/ejb2x/replyQueue");
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue("ejb2x/queue");
                jmsAdminOperations.removeJmsQueue("ejb2x/replyQueue");
                jmsAdminOperations.close();
            }
        }
    }

    @Deployment
    public static Archive getDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "mdb.jar");
        ejbJar.addClasses(EJB2xMDB.class, AbstractMDB2xTestCase.class);
        ejbJar.addPackage(JMSOperations.class.getPackage());
        ejbJar.addClasses(JmsQueueSetup.class, TimeoutUtil.class);
        ejbJar.addAsManifestResource(MDB21TestCase.class.getPackage(), "ejb-jar-21.xml", "ejb-jar.xml");
        ejbJar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        ejbJar.addAsManifestResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read")), "jboss-permissions.xml");
        return ejbJar;
    }

    @Before
    public void initQueues() {
        try {
            final InitialContext ic = new InitialContext();

            queue = (Queue) ic.lookup("java:jboss/ejb2x/queue");
            replyQueue = (Queue) ic.lookup("java:jboss/ejb2x/replyQueue");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test a simple 2.1 MDB.
     */
    @Test
    public void testSimple21MDB() {
        sendTextMessage("Say hello to " + EJB2xMDB.class.getName(), queue, replyQueue);
        final Message reply = receiveMessage(replyQueue, TimeoutUtil.adjust(5000));
        Assert.assertNotNull("Reply message was null on reply queue: " + replyQueue, reply);
    }
}
