/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.transaction.messaging;

import org.jboss.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSDestinationDefinitions;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;


@JMSDestinationDefinitions(
        value= {
                @JMSDestinationDefinition(
                        name = MDB.JNDI_NAME,
                        interfaceName = "javax.jms.Queue",
                        destinationName = "WFLY_10293"
                )
        }
)
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = MDB.JNDI_NAME),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue")})
@TransactionManagement(TransactionManagementType.CONTAINER)
public class MDB implements MessageListener {

    private static final Logger logger = Logger.getLogger(MDB.class);

    public static final String JNDI_NAME = "java:app/queue/WFLY_10293";
    public static final String TXT1 = "method";
    public static final String TXT2 = "methodWithTransaction";

    @Inject
    JMSContext context;

    @Inject
    CDI cdi;

    /**
     * @see MessageListener#onMessage(Message)
     */
    public void onMessage(Message message) {

        try {
            String txt = ((TextMessage)message).getText();

            if(TXT1.equals(txt)) {
                cdi.dummyMethod();
            } else {
                cdi.dummyMethodWithTransaction();
            }

            final Destination replyTo = message.getJMSReplyTo();
            // ignore messages that need no reply
            if (replyTo == null) {
                return;
            }
            //send message to out queue, as a result of successful call
            context.createProducer()
                    .setJMSCorrelationID(message.getJMSMessageID())
                    .send(replyTo, ((TextMessage) message).getText());
        } catch (Exception e) {
            logger.error(e);
        }
    }

}
