/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.smoke.embedded.demos.client.jms;

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;
import static org.jboss.as.protocol.StreamUtils.safeClose;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.container.MBeanServerConnectionProvider;
import org.jboss.as.arquillian.container.TunneledMBeanServerConnection;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.shared.util.fakejndi.FakeJndi;
import org.jboss.as.test.smoke.modular.utils.PollingUtils;
import org.jboss.as.test.smoke.modular.utils.ShrinkWrapUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Demo using the AS management API to create and destroy a JMS queue.
 *
 * @author Emanuel Muckenhuber
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JmsClientTestCase {

    private static final String QUEUE_NAME = "createdTestQueue";

    @Deployment
    public static Archive<?> createDeployment(){
        //TODO Don't do this FakeJndi stuff once we have remote JNDI working
        return ShrinkWrapUtils.createJavaArchive("demos/fakejndi.sar", FakeJndi.class.getPackage());
    }

    @Test
    public void testMessagingClient() throws Exception {
        QueueConnection conn = null;
        QueueSession session = null;
        ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("127.0.0.1"), 9999, getCallbackHandler());

        boolean actionsApplied = false;
        try {

            // Create the queue using the management API
            ModelNode op = new ModelNode();
            op.get("operation").set("add");
            op.get("address").add("subsystem", "messaging");
            op.get("address").add("hornetq-server", "default");
            op.get("address").add("jms-queue", QUEUE_NAME);
            op.get("entries").add(QUEUE_NAME);
            applyUpdate(op, client);
            actionsApplied = true;

            QueueConnectionFactory qcf = lookup(client, "RemoteConnectionFactory", QueueConnectionFactory.class);
            Queue queue = lookup(client, QUEUE_NAME, Queue.class);

            conn = qcf.createQueueConnection();
            conn.start();
            session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);

            final CountDownLatch latch = new CountDownLatch(10);
            final List<String> result = new ArrayList<String>();

            // Set the async listener
            QueueReceiver recv = session.createReceiver(queue);
            recv.setMessageListener(new MessageListener() {

                @Override
                public void onMessage(Message message) {
                    TextMessage msg = (TextMessage)message;
                    try {
                        result.add(msg.getText());
                        latch.countDown();
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });

            QueueSender sender = session.createSender(queue);
            for (int i = 0 ; i < 10 ; i++) {
                String s = "Test" + i;
                TextMessage msg = session.createTextMessage(s);
                sender.send(msg);
            }

            Assert.assertTrue(latch.await(3, TimeUnit.SECONDS));
            Assert.assertEquals(10, result.size());
            for (int i = 0 ; i < result.size() ; i++) {
                Assert.assertEquals("Test" + i, result.get(i));
            }

        } finally {
            try {
                conn.stop();
            } catch (Exception ignore) {
            }
            try {
                session.close();
            } catch (Exception ignore) {
            }
            try {
                conn.close();
            } catch (Exception ignore) {
            }

            if(actionsApplied) {
                // Remove the queue using the management API
                ModelNode op = new ModelNode();
                op.get("operation").set("remove");
                op.get("address").add("subsystem", "messaging");
                op.get("address").add("hornetq-server", "default");
                op.get("address").add("jms-queue", QUEUE_NAME);
                applyUpdate(op, client);
            }
            safeClose(client);
        }
    }

    static void applyUpdate(ModelNode update, final ModelControllerClient client) throws IOException {
        ModelNode result = client.execute(new OperationBuilder(update).build());
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            if (result.hasDefined("result")) {
                System.out.println(result.get("result"));
            }
        }
        else if (result.hasDefined("failure-description")){
            throw new RuntimeException(result.get("failure-description").toString());
        }
        else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }

    private <T> T lookup(ModelControllerClient client, String name, Class<T> expected) throws Exception {
        //TODO Don't do this FakeJndi stuff once we have remote JNDI working
        MBeanServerConnection mbeanServer = new TunneledMBeanServerConnection(client);
        ObjectName objectName = new ObjectName("jboss:name=test,type=fakejndi");
        PollingUtils.retryWithTimeout(10000, new PollingUtils.WaitForMBeanTask(mbeanServer, objectName));
        Object o = mbeanServer.invoke(objectName, "lookup", new Object[] {name}, new String[] {"java.lang.String"});
        return expected.cast(o);
    }
}
