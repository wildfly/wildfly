/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.delivery;

import static org.jboss.as.test.integration.ejb.mdb.delivery.ReplyUtil.reply;

import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import org.jboss.ejb3.annotation.DeliveryActive;

/**
 * A MDB deployed with deliveryActive set to false using annotations.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/deliveryactive/MDBWithAnnotationQueue"),
})
// Do not deliver messages to this MDB until start-delivery management operation is explicitly called on it.
@DeliveryActive(false)
public class MDBWithDeliveryActiveAnnotation implements MessageListener {

    @Resource(lookup = "java:/JmsXA")
    private ConnectionFactory factory;

    @Override
    public void onMessage(Message message) {
        reply(factory, message);
    }
}
