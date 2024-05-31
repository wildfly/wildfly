/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.delivery;

import static org.jboss.as.test.integration.ejb.mdb.delivery.ReplyUtil.reply;

import jakarta.annotation.Resource;
import jakarta.ejb.MessageDriven;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

/**
 * A MDB deployed with deliveryActive set to false in the deployment description.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@MessageDriven
public class MDBWithDeliveryActiveDeploymentDescriptor implements MessageListener {
    @Resource(lookup = "java:/JmsXA")
    private ConnectionFactory factory;

    @Override
    public void onMessage(Message message) {
        reply(factory, message);
    }
}
