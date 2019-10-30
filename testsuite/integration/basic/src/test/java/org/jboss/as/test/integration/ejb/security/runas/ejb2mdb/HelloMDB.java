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
import javax.annotation.security.RunAs;
import javax.annotation.security.PermitAll;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

import org.jboss.logging.Logger;

/**
 * MDB with RunAs annotation. Takes data from in queue and replies to reply queue.
 *
 * @author Ondrej Chaloupka
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/exported/queue/TestQueue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1")})
@RunAs("INTERNAL_ROLE")
@PermitAll // needed for access not being denied, see WFLY-8560
public class HelloMDB implements MessageListener {
    private static final Logger log = Logger.getLogger(HowdyBean.class);

    @EJB
    Howdy howdy;

    @Resource(mappedName = "java:/ConnectionFactory")
    private QueueConnectionFactory qFactory;

    @Resource(lookup = "java:global/runasmdbejb-ejb2/GoodBye!org.jboss.as.test.integration.ejb.security.runas.ejb2mdb.GoodByeLocalHome")
    GoodByeLocalHome goodByeHome;

    public void onMessage(Message message) {
        try {
            GoodByeLocal goodBye = goodByeHome.create();
            String messageToReply = String.format("%s! %s.", howdy.sayHowdy(), goodBye.sayGoodBye());

            sendReply(messageToReply, (Queue) message.getJMSReplyTo(), message.getJMSMessageID());
        } catch (Exception e) {
            log.errorf(e, "Can't process message '%s'", message);
        }
    }

    private void sendReply(String msg, Queue destination, String messageID) throws JMSException {
        QueueConnection conn = qFactory.createQueueConnection("guest", "guest");
        try {
            QueueSession session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            conn.start();
            QueueSender sender = session.createSender(destination);
            TextMessage message = session.createTextMessage(msg);
            message.setJMSCorrelationID(messageID);
            sender.send(message, DeliveryMode.NON_PERSISTENT, 4, 500);
        } finally {
            conn.close();
        }
    }
}
