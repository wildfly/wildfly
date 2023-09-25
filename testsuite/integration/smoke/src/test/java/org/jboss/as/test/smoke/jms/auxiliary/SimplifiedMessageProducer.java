/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jms.auxiliary;

import org.jboss.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateful;
import jakarta.enterprise.context.RequestScoped;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Destination;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;

/**
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 */
@Stateful
@RequestScoped
public class SimplifiedMessageProducer {

    private static final Logger logger = Logger.getLogger(SimplifiedMessageProducer.class);

    @Resource
    private ConnectionFactory defaultConnectionFactory;

    @Resource(lookup = "java:/ConnectionFactory")
    private ConnectionFactory regularConnectionFactory;

    public void sendWithDefaultJMSConnectionFactory(Destination destination, String text) throws Exception {
        send(defaultConnectionFactory, destination, text);
    }

    public void sendWithRegularConnectionFactory(Destination destination, String text) throws Exception {
        send(regularConnectionFactory, destination, text);
    }

    private void send(ConnectionFactory cf, Destination destination, String text) throws Exception {
        // TODO use Jakarta Messaging 2.0 context when HornetQ supports it
        Connection connection = cf.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(destination);

        Message message = session.createTextMessage(text);
        message.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
        producer.send(message);

        connection.close();
    }
}
