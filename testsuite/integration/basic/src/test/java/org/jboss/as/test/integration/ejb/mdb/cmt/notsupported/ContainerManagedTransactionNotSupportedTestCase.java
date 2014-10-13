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

package org.jboss.as.test.integration.ejb.mdb.cmt.notsupported;

import static org.jboss.as.test.shared.TimeoutUtil.adjust;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.PropertyPermission;
import java.util.UUID;

import javax.annotation.Resource;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;

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
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@RunWith(Arquillian.class)
@ServerSetup({ContainerManagedTransactionNotSupportedTestCase.JmsQueueSetup.class})
public class ContainerManagedTransactionNotSupportedTestCase {

    public static final String QUEUE_JNDI_NAME_FOR_ANNOTATION = "java:jboss/queue/cmt-not-supported-annotation";
    public static final String QUEUE_JNDI_NAME_FOR_DD = "java:jboss/queue/cmt-not-supported-dd";

    public static final String EXCEPTION_PROP_NAME = "setRollbackOnlyThrowsIllegalStateException";

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient);
            jmsAdminOperations.createJmsQueue("ContainerManagedTransactionNotSupportedTestCaseQueueWithAnnotation", QUEUE_JNDI_NAME_FOR_ANNOTATION);
            jmsAdminOperations.createJmsQueue("ContainerManagedTransactionNotSupportedTestCaseQueueWithDD", QUEUE_JNDI_NAME_FOR_DD);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue("ContainerManagedTransactionNotSupportedTestCaseQueueWithAnnotation");
                jmsAdminOperations.removeJmsQueue("ContainerManagedTransactionNotSupportedTestCaseQueueWithDD");
                jmsAdminOperations.close();
            }
        }
    }

    @Resource(mappedName = QUEUE_JNDI_NAME_FOR_ANNOTATION)
    private Queue queueForAnnotation;

    @Resource(mappedName = QUEUE_JNDI_NAME_FOR_DD)
    private Queue queueForDD;

    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory cf;

    @Deployment
    public static Archive createDeployment() {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ContainerManagedTransactionNotSupportedTestCase.jar");
        jar.addPackage(JMSOperations.class.getPackage());
        jar.addClasses(TimeoutUtil.class);
        jar.addPackage(BaseMDB.class.getPackage());
        jar.addAsManifestResource(ContainerManagedTransactionNotSupportedMDBWithDD.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        // grant necessary permissions
        jar.addAsResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read")), "META-INF/jboss-permissions.xml");

        return jar;
    }

    /**
     * Test that the {@link javax.ejb.MessageDrivenContext#setRollbackOnly()} throws an IllegalStateException
     * when a CMT MDB with NOT_SUPPORTED transaction attribute is invoked.
     *
     * @throws Exception
     */
    @Test
    public void testSetRollbackOnlyInContainerManagedTransactionNotSupportedMDBThrowsIllegalStateExceptionWithAnnotation() throws Exception {
        doSetRollbackOnlyInContainerManagedTransactionNotSupportedMDBThrowsIllegalStateException(queueForAnnotation);
    }

    @Test
    public void testSetRollbackOnlyInContainerManagedTransactionNotSupportedMDBThrowsIllegalStateExceptionWithDD() throws Exception {
        doSetRollbackOnlyInContainerManagedTransactionNotSupportedMDBThrowsIllegalStateException(queueForDD);
    }

    private void doSetRollbackOnlyInContainerManagedTransactionNotSupportedMDBThrowsIllegalStateException(Destination destination) throws Exception {
        try (
                JMSContext context = cf.createContext()
        ) {
            TemporaryQueue replyTo = context.createTemporaryQueue();

            String text = UUID.randomUUID().toString();

            TextMessage message = context.createTextMessage(text);
            message.setJMSReplyTo(replyTo);

            context.createProducer()
                    .send(destination, message);

            Message reply = context.createConsumer(replyTo)
                    .receive(adjust(2000));
            assertNotNull(reply);
            assertEquals(message.getJMSMessageID(), reply.getJMSCorrelationID());
            assertTrue("messageDrivenContext.setRollbackOnly() did not throw the expected IllegalStateException",
                    reply.getBooleanProperty(EXCEPTION_PROP_NAME));
        }
    }
}
