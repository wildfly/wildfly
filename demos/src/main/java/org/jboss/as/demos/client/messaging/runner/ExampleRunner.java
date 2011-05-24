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

package org.jboss.as.demos.client.messaging.runner;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.resource.spi.IllegalStateException;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSession.QueueQuery;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;

/**
 * Demo using the AS management API to create and destroy a HornetQ core queue.
 *
 * @author Emanuel Muckenhuber
 */
public class ExampleRunner {

    public static void main(String[] args) throws Exception {

        final String queueName = "queue.standalone";

        final ClientSessionFactory sf = createClientSessionFactory("localhost", 5445);
        final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);

        try {
            // Check that the queue does not exist
            if(queueExists(queueName, sf)) {
                throw new IllegalStateException();
            }

            // Create a new core queue using the standalone client


            ModelNode op = new ModelNode();
            op.get("operation").set("add");
            op.get("address").add("subsystem", "messaging");
            op.get("address").add("queue", queueName);
            op.get("queue-address").set(queueName);
            applyUpdate(op, client);

            // Check if the queue exists
            if(! queueExists(queueName, sf)) {
                throw new IllegalStateException();
            }

            ClientSession session = null;
            try {
               session = sf.createSession();
               ClientProducer producer = session.createProducer(queueName);
               ClientMessage message = session.createMessage(false);

               final String propName = "myprop";
               message.putStringProperty(propName, "Hello sent at " + new Date());
               System.out.println("Sending the message.");

               producer.send(message);

               ClientConsumer messageConsumer = session.createConsumer(queueName);
               session.start();

               ClientMessage messageReceived = messageConsumer.receive(1000);
               System.out.println("Received TextMessage:" + messageReceived.getStringProperty(propName));
            } finally {
               if (session != null) {
                  session.close();
               }
            }

            op = new ModelNode();
            op.get("operation").set("remove");
            op.get("address").add("subsystem", "messaging");
            op.get("address").add("queue", queueName);
            applyUpdate(op, client);

            // Check that the queue does not exist
            if(queueExists(queueName, sf)) {
                throw new IllegalStateException();
            }
        } finally {
            client.close();
        }
    }

    static void applyUpdate(ModelNode update, final ModelControllerClient client) throws OperationFailedException, IOException {
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

    static boolean queueExists(final String queueName, final ClientSessionFactory sf) throws HornetQException {
        final ClientSession session = sf.createSession(false, false, false);
        try {
            final QueueQuery query = session.queueQuery(new SimpleString(queueName));
            return query.isExists();
        } finally {
            session.close();
        }
    }

    static ClientSessionFactory createClientSessionFactory(String host, int port) throws Exception {
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("host", host);
        properties.put("port", port);
        final TransportConfiguration configuration = new TransportConfiguration(NettyConnectorFactory.class.getName(), properties);
        return HornetQClient.createServerLocatorWithoutHA(configuration).createSessionFactory();
    }

}
