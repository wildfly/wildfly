/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.mdb;

import org.jboss.logging.Logger;

import javax.ejb.EJB;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * User: jpai
 */
public class DDBasedMDB implements MessageListener {

    @EJB
    private JMSMessagingUtil jmsMessagingUtil;

    private static final Logger logger = Logger.getLogger(DDBasedMDB.class);

    @Override
    public void onMessage(Message message) {
        logger.info("Received message " + message + " in MDB " + this.getClass().getName());
        try {
            final Destination replyTo = message.getJMSReplyTo();
            if (replyTo == null) {
                return;
            }
            logger.info("Sending a reply to destination " + replyTo);
            jmsMessagingUtil.reply(message);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
