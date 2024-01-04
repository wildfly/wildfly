/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.mgmt;

import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.TemporaryQueue;

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
        // create a consumer on a temp queue to ensure the Jakarta Messaging
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
