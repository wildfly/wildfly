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

package org.jboss.as.test.smoke.jms.auxiliary;

import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.jms.*;

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
        // TODO use JMS 2.0 context when HornetQ supports it
        Connection connection = cf.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(destination);

        Message message = session.createTextMessage(text);
        message.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
        producer.send(message);

        connection.close();
    }
}
