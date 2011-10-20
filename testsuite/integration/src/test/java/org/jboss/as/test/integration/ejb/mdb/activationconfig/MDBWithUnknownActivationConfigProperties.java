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

package org.jboss.as.test.integration.ejb.mdb.activationconfig;

import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * User: jpai
 */
@MessageDriven (activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = MDBWithUnknownActivationConfigProperties.QUEUE_JNDI_NAME),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "foo", propertyValue = "bar") // activation config properties which aren't supported by the resource adapter
})
public class MDBWithUnknownActivationConfigProperties implements MessageListener {


    private static final Logger logger = Logger.getLogger(MDBWithUnknownActivationConfigProperties.class);

    public static final String QUEUE_JNDI_NAME = "java:jboss/jms/mdbtest/unknown-activationconfig-props";

    @EJB
    private JMSMessagingUtil jmsMessagingUtil;

    @Override
    public void onMessage(Message message) {
        logger.info("Received message: " + message);
        try {
            if (message.getJMSReplyTo() != null) {
                logger.info("Replying to " + message.getJMSReplyTo());
                this.jmsMessagingUtil.reply(message);
            }
        } catch (JMSException jmse) {
            throw new RuntimeException(jmse);
        }
    }
}
