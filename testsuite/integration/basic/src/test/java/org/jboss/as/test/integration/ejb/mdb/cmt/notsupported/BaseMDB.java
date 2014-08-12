/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.mdb.cmt.notsupported;

import static org.jboss.as.test.integration.ejb.mdb.cmt.notsupported.ContainerManagedTransactionNotSupportedTestCase.EXCEPTION_PROP_NAME;

import javax.annotation.Resource;
import javax.ejb.MessageDrivenContext;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.jboss.logging.Logger;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public abstract class BaseMDB implements MessageListener {

    private static final Logger logger = Logger.getLogger(BaseMDB.class);

    @Resource(lookup="java:/ConnectionFactory")
    private ConnectionFactory cf;

    @Resource
    protected MessageDrivenContext messageDrivenContext;

    @Override
    public void onMessage(Message message) {
        logger.info("Received message: " + message);

        boolean setRollbackOnlyThrowsIllegalStateException;

        try {
            // this method in a Container-managed MDB with transaction NOT_SUPPORTED must throw an exception
            messageDrivenContext.setRollbackOnly();
            setRollbackOnlyThrowsIllegalStateException = false;
        } catch (IllegalStateException e) {
            setRollbackOnlyThrowsIllegalStateException = true;
        }
        Connection con = null;
        Session session = null;
        try {
            final Destination replyTo = message.getJMSReplyTo();
            if (replyTo != null) {
                logger.info("Replying to " + replyTo);

                con = cf.createConnection();

                session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
                MessageProducer producer = session.createProducer(replyTo);
                TextMessage msg = session.createTextMessage("");
                msg.setJMSCorrelationID(message.getJMSMessageID());
                msg.setBooleanProperty(EXCEPTION_PROP_NAME, setRollbackOnlyThrowsIllegalStateException);
                producer.send(replyTo, msg);
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
