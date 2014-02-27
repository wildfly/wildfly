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
package org.jboss.as.testsuite.integration.secman;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.testsuite.integration.secman.mdb.ReadSystemPropertyMDB;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.NamingException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * A common class for PropertyPermissions tests on MDB.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public abstract class AbstractMDBPermissionPropertyReadTestCase {

    private static final String EJBAPP_BASE_NAME = "mdb-read-props";
    private static final String EJBAPP_SFX_GRANT = "-grant";
    private static final String EJBAPP_SFX_LIMITED = "-limited";
    private static final String EJBAPP_SFX_DENY = "-deny";

    private static final Logger logger = Logger.getLogger(AbstractMDBPermissionPropertyReadTestCase.class);

    @Resource(mappedName = "java:/JmsXA")
    private QueueConnectionFactory factory;

    @Resource (mappedName = "queue/propertyTestQueue")
    private Queue queue;

    /**
     * Creates archive with a tested application.
     *
     * @return {@link org.jboss.shrinkwrap.api.spec.JavaArchive} instance
     */
    protected static JavaArchive grantDeployment() {
        return ejbDeployment(EJBAPP_SFX_GRANT);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link org.jboss.shrinkwrap.api.spec.JavaArchive} instance
     */
    public static JavaArchive limitedDeployment() {
        return ejbDeployment(EJBAPP_SFX_LIMITED);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link org.jboss.shrinkwrap.api.spec.JavaArchive} instance
     */
    public static JavaArchive denyDeployment() {
        return ejbDeployment(EJBAPP_SFX_DENY);
    }

    private static JavaArchive ejbDeployment(final String suffix) {
        final JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, EJBAPP_BASE_NAME + suffix + ".jar");
        ejb.addPackage(ReadSystemPropertyMDB.class.getPackage());
        ejb.addClass(AbstractMDBPermissionPropertyReadTestCase.class);
        logger.debug(ejb.toString(true));
        return ejb;
    }

    protected void sendMessage(final String propertyName, final boolean failureExpected) throws JMSException, NamingException {
        final QueueConnection connection = factory.createQueueConnection();
        connection.start();
        final QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        final Queue replyDestination = session.createTemporaryQueue();
        final QueueReceiver receiver = session.createReceiver(replyDestination);
        final Message message = session.createTextMessage(propertyName);
        message.setJMSReplyTo(replyDestination);
        final MessageProducer producer = session.createProducer(queue);
        producer.send(message);
        producer.close();

        final Message reply = receiver.receive(1000);
        assertNotNull(reply);
        final String result = ((TextMessage) reply).getText();
        logger.info("result: " + result);

        assertEquals(failureExpected ? "failed" : "got it!", result);
    }

    static class JmsQueueSetup implements ServerSetupTask {
        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient);
            jmsAdminOperations.createJmsQueue("propertyTestQueue", "queue/propertyTestQueue");
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue("propertyTestQueue");
                jmsAdminOperations.close();
            }
        }
    }

}
