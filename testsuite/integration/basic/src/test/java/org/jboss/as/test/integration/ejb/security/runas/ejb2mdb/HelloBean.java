/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security.runas.ejb2mdb;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
// Security related imports
import javax.annotation.security.RolesAllowed;
import javax.jms.*;
import javax.naming.*;
import org.jboss.logging.Logger;

/**
 * Bean passes message to HelloMDB bean and checks the reply queue. The HellpMDB bean calls this one for getting hello greeting
 * for JBossAdmin role.
 * 
 * @author Ondrej Chaloupka
 */
@Stateless(name = "Hello")
@RolesAllowed({})
@Remote(Hello.class)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class HelloBean implements Hello {
    private static final Logger log = Logger.getLogger(HelloBean.class);

    @Resource
    private SessionContext context;

    @RolesAllowed("JBossAdmin")
    public String sayHello() throws Exception {
        log.debug("session context: " + context);
        log.debug("caller name: " + context.getCallerPrincipal().getName());

        if (context.isCallerInRole("JBossAdmin")) {
            throw new IllegalArgumentException("User is in role!!");
        }

        log.info("HelloBean - sending message");
        String msg = this.sendMessage();

        String name = getName();
        return "Hello " + name + "! " + msg;
    }

    private String getName() {
        return "Fred";
    }

    public String sendMessage() throws Exception {
        String destinationName = "java:jboss/exported/queue/TestQueue";
        Context ic = null;
        ConnectionFactory cf = null;
        Connection connection = null;

        try {
            ic = getInitialContext();
            cf = (ConnectionFactory) ic.lookup("java:/ConnectionFactory");
            Queue queue = (Queue) ic.lookup(destinationName);
            connection = cf.createConnection("guest", "guest");
            connection.start(); // we need to start connection for consumer
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer sender = session.createProducer(queue);
            TextMessage message = session.createTextMessage("hello goodbye");
            TemporaryQueue replyQueue = session.createTemporaryQueue();
            message.setJMSReplyTo(replyQueue);
            sender.send(message);

            MessageConsumer consumer = session.createConsumer(replyQueue);
            TextMessage replyMsg = (TextMessage) consumer.receive(5000);
            log.info("Message received:" + replyMsg);
            return replyMsg.getText();
        } finally {
            if (ic != null) {
                try {
                    ic.close();
                } catch (Exception ignore) {
                }
            }
            closeConnection(connection);
        }

    }

    public static InitialContext getInitialContext() throws NamingException {
        return new InitialContext();
    }

    private void closeConnection(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (JMSException jmse) {
            log.info("connection close failed: " + jmse);
        }
    }

}
