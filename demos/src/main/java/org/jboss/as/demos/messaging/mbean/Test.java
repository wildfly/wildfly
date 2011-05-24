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
package org.jboss.as.demos.messaging.mbean;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.jboss.logging.Logger;

/**
 * HornetQ example using the core API
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class Test implements TestMBean {

    private static final String QUEUE_EXAMPLE_QUEUE = "queue.exampleQueue";

    static final Logger log = Logger.getLogger(Test.class);

    private static final String BODY = "msg.body";

    private ClientSessionFactory sf;
    private ClientSession session;
    private ClientConsumer consumer;

    private final AtomicBoolean shutdown = new AtomicBoolean();

    private final List<String> receivedMessages = new ArrayList<String>();

    @Override
    public void start() throws Exception {
        //HornetQ needs the proper TCL
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        try {
            //HornetQService set up the config and starts the HornetQServer

            //Not using JNDI so we use the core services directly
            sf = HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName())).createSessionFactory();

            //Create a queue
            ClientSession coreSession = sf.createSession(false, true, true);
            coreSession.createQueue(QUEUE_EXAMPLE_QUEUE, QUEUE_EXAMPLE_QUEUE, true);
            coreSession.close();

            session = sf.createSession();

            consumer = session.createConsumer(QUEUE_EXAMPLE_QUEUE);
            session.start();

            new Thread(new Runnable() {

                @Override
                public void run() {
                    while (!shutdown.get()) {
                        try {
                            ClientMessage message = consumer.receive(500);
                            if (message == null) {
                                continue;
                            }
                            String s = message.getStringProperty(BODY);
                            log.info("-----> Received: " + s);
                            synchronized (receivedMessages) {
                                receivedMessages.add(s);
                            }
                        } catch (HornetQException e) {
                            log.error("Exception, closing receiver", e);
                        }
                    }
                }
            }).start();

            System.out.println("-----> Started queue and session");
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    @Override
    public void stop() throws Exception {
        shutdown.set(true);
        if (session != null)
            session.close();
        ClientSession coreSession = sf.createSession(false, false, false);
        coreSession.deleteQueue(QUEUE_EXAMPLE_QUEUE);
        coreSession.close();
    }

    @Override
    public void sendMessage(String txt) throws Exception {
        System.out.println("-----> Attempting to send message");
        ClientProducer producer = session.createProducer(QUEUE_EXAMPLE_QUEUE);
        ClientMessage message = session.createMessage(false);

        message.putStringProperty(BODY, "'" + txt + "' sent at " + new Date());
        System.out.println("-----> Sending message");
        producer.send(message);
    }

    @Override
    public List<String> readMessages(String txt) {
        synchronized (receivedMessages) {
            List<String> list = new ArrayList<String>(receivedMessages);
            receivedMessages.clear();
            return list;
        }
    }
}
