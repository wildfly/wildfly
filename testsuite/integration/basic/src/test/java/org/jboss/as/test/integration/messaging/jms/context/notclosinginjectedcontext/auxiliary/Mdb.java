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


package org.jboss.as.test.integration.messaging.jms.context.notclosinginjectedcontext.auxiliary;

import org.jboss.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.JMSDestinationDefinitions;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;


/**
 * Message driven bean receives message from wfly10531_in queue and sends them into wfly10531_verify queue.
 *
 * @author Gunter Zeilinger <gunterze@gmail.com>, Jiri Ondrusek <jondruse@redhat.com>
 * @since Sep 2018
 */
@JMSDestinationDefinitions(
        value= {
                @JMSDestinationDefinition(
                        name = Mdb.JNDI_NAME,
                        interfaceName = "jakarta.jms.Queue",
                        destinationName = "wfly10531_in"
                ),
                @JMSDestinationDefinition(
                        name = Mdb.JNDI_VERIFY_NAME,
                        interfaceName = "jakarta.jms.Queue",
                        destinationName = "wfly10531_verify"
                )
        }
)
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = Mdb.JNDI_NAME),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")})
public class Mdb implements MessageListener {
    private static final Logger LOGGER = Logger.getLogger(Mdb.class);

    public static final String JNDI_NAME = "java:app/queue/wfly10531_in";
    public static final String JNDI_VERIFY_NAME = "java:app/queue/wfly10531_verify";


    @Inject
    private JMSContext context;

    @Resource(mappedName = JNDI_VERIFY_NAME)
    private Queue queue;

    public void onMessage(Message message) {
        try {
            String text = ((TextMessage) message).getText();
            LOGGER.info("Received " + text);
            context.createProducer()
                    .send(queue, text);

        } catch (Throwable e) {
            LOGGER.error("Failed to receive or send message", e);
        }
    }
}
