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

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;

import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.junit.Assert;

/**
 * @author baranowb
 *
 */
@Stateless()
public class MDBProxyBean implements MDBProxy{

    @EJB(mappedName = "java:module/JMSMessagingUtil")
    private JMSMessagingUtil jmsUtil;

    @Resource(mappedName = REPLY_QUEUE_JNDI_NAME)
    private Queue replyQueue;

    @Resource(mappedName = QUEUE_JNDI_NAME)
    private Queue queue;

    @Override
    public void trigger() throws Exception {
        final String goodMorning = "Good morning";
        // send as ObjectMessage
        // this.jmsUtil = (JMSMessagingUtil) getInitialContext().lookup(getEJBJNDIBinding());
        this.jmsUtil.sendTextMessage(goodMorning, this.queue, this.replyQueue);
        // wait for an reply
        final Message reply = this.jmsUtil.receiveMessage(replyQueue, 5000);
        // test the reply
        final TextMessage textMessage = (TextMessage) reply;
        Assert.assertEquals("Unexpected reply message on reply queue: " + this.replyQueue, CdiIntegrationMDB.REPLY,
                textMessage.getText());
    }
}
