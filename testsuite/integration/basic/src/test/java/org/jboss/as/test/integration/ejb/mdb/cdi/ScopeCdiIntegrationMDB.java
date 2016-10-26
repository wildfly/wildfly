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

package org.jboss.as.test.integration.ejb.mdb.cdi;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.ejb3.annotation.ResourceAdapter;
import org.jboss.logging.Logger;

/**
 * User: jpai
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = MDBProxy.QUEUE_JNDI_NAME)
})
@ResourceAdapter(value = MDBRAScopeCdiIntegrationTestCase.subDeploymentName)
public class ScopeCdiIntegrationMDB implements MessageListener {

    private static final Logger logger = Logger.getLogger(ScopeCdiIntegrationMDB.class);


    public static final String REPLY = "Successful message delivery!";

    @EJB
    private JMSMessagingUtil jmsMessagingUtil;

    @Inject
    private RequestScopedCDIBean requestScopedCDIBean;

    @Override
    public void onMessage(Message message) {
        logger.trace("Received message " + message);
        try {
            if (message.getJMSReplyTo() != null) {
                logger.trace("Replying to " + message.getJMSReplyTo());
                // send a reply
                this.jmsMessagingUtil.sendTextMessage(requestScopedCDIBean.sayHello(), message.getJMSReplyTo(), null);
            }
        } catch (JMSException jmse) {
            throw new RuntimeException(jmse);
        }

    }
}
