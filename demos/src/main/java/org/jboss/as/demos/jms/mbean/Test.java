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
package org.jboss.as.demos.jms.mbean;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.jboss.logging.Logger;

/**
 * JMS Example (waiting for JMS to be implemented)
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class Test implements TestMBean {

    static final Logger log = Logger.getLogger(Test.class);

    private QueueConnection conn;
    private Queue queue;
    private QueueSession session;

    private final List<String> receivedMessages = new ArrayList<String>();

    public void start() throws Exception {

        InitialContext ctx = new InitialContext();

        QueueConnectionFactory qcf = (QueueConnectionFactory)ctx.lookup("java:/ConnectionFactory");
        conn = qcf.createQueueConnection();
        conn.start();
        queue = (Queue)ctx.lookup("queue/test");
        session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);

        // Set the async listener
        QueueReceiver recv = session.createReceiver(queue);
        recv.setMessageListener(new ExampeMessageListener());
    }

    public void stop() throws Exception {
        if (conn != null) {
            conn.stop();
        }
        if (session != null) {
            session.close();
        }
        if (conn != null) {
            conn.close();
        }
    }

    public void sendMessage(String txt) throws Exception {
        QueueSender send = null;
        try {
            // Send a text msg
            send = session.createSender(queue);
            TextMessage tm = session.createTextMessage(txt);
            send.send(tm);
            log.info("-----> sent text=" + tm.getText());
        } finally {
            send.close();
        }
    }

    public List<String> readMessages() {
        synchronized (receivedMessages) {
            List<String> list = new ArrayList<String>(receivedMessages);
            receivedMessages.clear();
            return list;
        }
    }

    private class ExampeMessageListener implements MessageListener {

        @Override
        public void onMessage(Message message) {
            TextMessage msg = (TextMessage)message;
            try {
                log.info("-----> on message: " + msg.getText());
                synchronized (receivedMessages) {
                    receivedMessages.add(msg.getText());
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
