/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.messaging.jms.context.injectedToRequestScoped;

import javax.annotation.Resource;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.transaction.Transactional;

@RequestScoped
@Stateful
public class Alpha {

    @Inject
    JMSContext context;

    @Resource(mappedName = TestMessageListener.QUEUE_JNDI_NAME)
    private Queue queue;

    @Transactional
    public void ping() {
        Message message = context.createMessage();
        try {
            message.setJMSDestination(queue);
        } catch (JMSException e) {
            e.printStackTrace();
        }
        context.createProducer().send(queue, message);
    }

    public Message createConsumer(int timeout) {
        Message message = context.createConsumer(queue).receive(timeout);
        return message;
    }

}
