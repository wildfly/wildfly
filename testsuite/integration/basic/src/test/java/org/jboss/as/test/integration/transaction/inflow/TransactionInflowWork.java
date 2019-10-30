/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.integration.transaction.inflow;

import javax.jms.MessageListener;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import org.jboss.logging.Logger;

/**
 * JCA work executing onMessage method with text message payload.
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
public class TransactionInflowWork implements Work {
    private static final Logger log = Logger.getLogger(TransactionInflowWork.class);

    private MessageEndpointFactory messageFactory;
    private String msg;

    TransactionInflowWork(MessageEndpointFactory messageFactory, String msg) {
        this.messageFactory = messageFactory;
        this.msg = msg;
    }

    public void run() {
        MessageEndpoint messageEndpoint;
        try {
            messageEndpoint = messageFactory.createEndpoint(null);
            if (messageEndpoint instanceof MessageListener){
                log.tracef("Calling on message on endpoint '%s' with data message '%s'", messageEndpoint, msg);
                TransactionInflowTextMessage textMessage = new TransactionInflowTextMessage(msg);
                ((MessageListener) messageEndpoint).onMessage(textMessage);
            } else {
                throw new IllegalStateException("Not supported message endpoint: " + messageEndpoint);
            }
        } catch (UnavailableException ue) {
            String msg = String.format("Not capable to create message factory '%s' endpoint", messageFactory);
            throw new IllegalStateException(msg, ue);
        }
    }

    public void release() {
        // nothing to release
    }
}
