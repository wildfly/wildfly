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

package org.jboss.as.demos.client.jms.runner;

import static org.jboss.as.protocol.StreamUtils.safeClose;

import java.io.IOException;
import java.net.InetAddress;

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

import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.demos.DeploymentUtils;
import org.jboss.as.demos.fakejndi.FakeJndi;
import org.jboss.dmr.ModelNode;

/**
 * Demo using the AS management API to create and destroy a JMS queue.
 *
 * @author Emanuel Muckenhuber
 */
public class ExampleRunner {

    private static final String QUEUE_NAME = "createdTestQueue";

    public static void main(String[] args) throws Exception {
        QueueConnection conn = null;
        QueueSession session = null;
        ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
        //TODO Don't do this FakeJndi stuff once we have remote JNDI working
        DeploymentUtils utils = null;
        boolean actionsApplied = false;
        try {
            utils = new DeploymentUtils("fakejndi.sar", FakeJndi.class.getPackage());
            utils.deploy();

            ModelNode op = new ModelNode();
            op.get("operation").set("add");
            op.get("address").add("subsystem", "jms");
            op.get("address").add("queue", QUEUE_NAME);
            op.get("entries").add(QUEUE_NAME);
            applyUpdate(op, client);
            actionsApplied = true;

            QueueConnectionFactory qcf = lookup(utils, "RemoteConnectionFactory", QueueConnectionFactory.class);
            Queue queue = lookup(utils, QUEUE_NAME, Queue.class);

            conn = qcf.createQueueConnection();
            conn.start();
            session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);

            // Set the async listener
            QueueReceiver recv = session.createReceiver(queue);
            recv.setMessageListener(new MessageListener() {

                @Override
                public void onMessage(Message message) {
                    TextMessage msg = (TextMessage)message;
                    try {
                        System.out.println("---->Received: " + msg.getText());
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });

            QueueSender sender = session.createSender(queue);
            for (int i = 0 ; i < 10 ; i++) {
                String s = "Test" + i;
                System.out.println("----> Sending: " +s );
                TextMessage msg = session.createTextMessage(s);
                sender.send(msg);
            }

            Thread.sleep(1000);

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

            if(utils != null) {
                utils.undeploy();
            }
            safeClose(utils);
            if(actionsApplied) {
                // Remove the queue using the management API
                ModelNode op = new ModelNode();
                op.get("operation").set("remove");
                op.get("address").add("subsystem", "jms");
                op.get("address").add("queue", QUEUE_NAME);
                applyUpdate(op, client);
            }
            safeClose(client);
        }
    }

    static void applyUpdate(ModelNode update, final ModelControllerClient client) throws IOException {
        ModelNode result = client.execute(OperationBuilder.Factory.create(update).build());
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

    private static <T> T lookup(DeploymentUtils utils, String name, Class<T> expected) throws Exception {
        MBeanServerConnection mbeanServer = utils.getConnection();
        ObjectName objectName = new ObjectName("jboss:name=test,type=fakejndi");
        Object o = mbeanServer.invoke(objectName, "lookup", new Object[] {name}, new String[] {"java.lang.String"});
        return expected.cast(o);
    }
}
