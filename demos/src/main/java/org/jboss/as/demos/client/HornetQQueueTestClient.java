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

package org.jboss.as.demos.client;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import org.jboss.as.messaging.QueueAdd;
import org.jboss.as.messaging.QueueRemove;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.ServerSubsystemUpdate;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.standalone.client.api.StandaloneClient;
import org.jboss.as.standalone.client.api.StandaloneUpdateResult;

/**
 * @author Emanuel Muckenhuber
 */
public class HornetQQueueTestClient {

    public static void main(String[] args) throws Exception {

        final String queueName = "queue.standalone";

        final ClientSessionFactory sf = createClientSessionFactory("localhost", 5445);
        final StandaloneClient client = StandaloneClient.Factory.create(InetAddress.getByName("localhost"), 9999);

        // Check that the queue does not exists
        if(queueExists(queueName, sf)) {
            throw new IllegalStateException();
        }

        // Create a new core queue using the standalone client
        final QueueAdd add = new QueueAdd(queueName);
        add.setAddress(queueName);
        applyUpdates(Collections.<AbstractServerModelUpdate<?>>singletonList(ServerSubsystemUpdate.create(add)), client);
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

        final QueueRemove remove = new QueueRemove(queueName);
        applyUpdates(Collections.<AbstractServerModelUpdate<?>>singletonList(ServerSubsystemUpdate.create(remove)), client);

        // Check that the queue does not exists
        if(queueExists(queueName, sf)) {
            throw new IllegalStateException();
        }
    }

    static void applyUpdates(final List<AbstractServerModelUpdate<?>> updates, final StandaloneClient client) throws UpdateFailedException {
        for(StandaloneUpdateResult<?> result : client.applyUpdates(updates)) {
            if(! result.isSuccess()) {
                throw result.getFailure();
            }
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

    static ClientSessionFactory createClientSessionFactory(String host, int port) {
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("host", host);
        properties.put("port", port);
        final TransportConfiguration configuration = new TransportConfiguration(NettyConnectorFactory.class.getName(), properties);
        return HornetQClient.createClientSessionFactory(configuration);
    }

}
