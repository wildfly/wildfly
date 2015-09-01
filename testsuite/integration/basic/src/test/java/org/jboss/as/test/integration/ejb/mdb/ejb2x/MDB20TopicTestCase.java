/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.InitialContext;

/**
 * Tests EJB2.0 MDBs listening on a topic.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({MDB20TopicTestCase.JmsQueueSetup.class})
public class MDB20TopicTestCase extends AbstractMDB2xTestCase {

    private Topic topic;
    private Queue replyQueueA;
    private Queue replyQueueB;

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsAdminOperations.createJmsTopic("ejb2x-topic", "java:jboss/ejb2x/topic");
            jmsAdminOperations.createJmsQueue("ejb2x-replyQueueA", "java:jboss/ejb2x/replyQueueA");
            jmsAdminOperations.createJmsQueue("ejb2x-replyQueueB", "java:jboss/ejb2x/replyQueueB");
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsTopic("ejb2x-topic");
                jmsAdminOperations.removeJmsQueue("ejb2x-replyQueueA");
                jmsAdminOperations.removeJmsQueue("ejb2x-replyQueueB");
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
        ejbJar.addAsManifestResource(MDB20TopicTestCase.class.getPackage(), "ejb-jar-20-topic.xml", "ejb-jar.xml");
        ejbJar.addAsManifestResource(MDB20TopicTestCase.class.getPackage(), "jboss-ejb3-topic.xml", "jboss-ejb3.xml");
        ejbJar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        logger.info(ejbJar.toString(true));
        return ejbJar;
    }

    @Before
    public void initTopics() {
        try {
            final InitialContext ic = new InitialContext();

            topic = (Topic) ic.lookup("java:jboss/ejb2x/topic");
            replyQueueA = (Queue) ic.lookup("java:jboss/ejb2x/replyQueueA");
            replyQueueB = (Queue) ic.lookup("java:jboss/ejb2x/replyQueueB");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Tests 2 MDBs both listening on a topic.
     */
    @Test
    public void testEjb20TopicMDBs() {
        sendTextMessage("Say hello to the topic", topic);
        final Message replyA = receiveMessage(replyQueueA, TimeoutUtil.adjust(5000));
        Assert.assertNotNull("Reply message was null on reply queue: " + replyQueueA, replyA);
        final Message replyB = receiveMessage(replyQueueB, TimeoutUtil.adjust(5000));
        Assert.assertNotNull("Reply message was null on reply queue: " + replyQueueB, replyB);
    }
}
