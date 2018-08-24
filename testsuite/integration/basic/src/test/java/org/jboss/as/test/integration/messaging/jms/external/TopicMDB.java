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
package org.jboss.as.test.integration.messaging.jms.external;

import static org.jboss.as.test.integration.messaging.jms.external.ExternalMessagingDeploymentTestCase.REMOTE_PCF;
import static org.jboss.as.test.integration.messaging.jms.external.ExternalMessagingDeploymentTestCase.TOPIC_LOOKUP;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSPasswordCredential;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import org.jboss.ejb3.annotation.ResourceAdapter;

/**
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
@MessageDriven(
        activationConfig = {
            @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
            @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = TOPIC_LOOKUP),
            @ActivationConfigProperty(propertyName="user", propertyValue="guest"),
            @ActivationConfigProperty(propertyName="password", propertyValue="guest")
        }
)
@ResourceAdapter(REMOTE_PCF) // name of the pooled-connection-factory resource
public class TopicMDB implements MessageListener {

    @Inject
    @JMSPasswordCredential(userName = "guest", password = "guest")
    private JMSContext context;

    @Override
    public void onMessage(final Message m) {
        try {
            TextMessage message = (TextMessage) m;
            Destination replyTo = m.getJMSReplyTo();

            context.createProducer()
                    .setJMSCorrelationID(message.getJMSMessageID())
                    .send(replyTo, message.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
