/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.messagelistener.nomethodinterface;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;

import org.jboss.ejb3.annotation.ResourceAdapter;
import org.jboss.logging.Logger;

/**
 * @author Jan Martiska
 */
@MessageDriven(
        messageListenerInterface = NoMethodMessageListener.class,
        activationConfig = @ActivationConfigProperty(propertyName = "methodName", propertyValue = "handleMessage")
)
@ResourceAdapter("no-method-message-listener-test.ear#resource-adapter.rar")
public class SimpleMessageDrivenBean implements NoMethodMessageListener {

    @EJB
    private ReceivedMessageTracker tracker;

    private Logger logger = Logger.getLogger(SimpleMessageDrivenBean.class);

    public void handleMessage(String message) {
        logger.trace("SimpleMessageDriven bean received message: " + message);
        tracker.getReceivedLatch().countDown();
    }

}
