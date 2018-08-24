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

package org.jboss.as.test.integration.messaging.mgmt;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.TemporaryQueue;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
@Stateful
@Remote(RemoteConnectionHolding.class)
public class ConnectionHoldingBean implements RemoteConnectionHolding {

    @Resource(lookup = "java:/JmsXA")
    ConnectionFactory factory;

    private JMSContext context;

    @Override
    public void createConnection() throws JMSException {
        // create a consumer on a temp queue to ensure the JMS
        // connection is actually created and started
        context = factory.createContext("guest", "guest");
        TemporaryQueue tempQueue = context.createTemporaryQueue();
        context.createConsumer(tempQueue);
    }

    @Override
    public void closeConnection() throws JMSException {
        context.close();
    }
}
