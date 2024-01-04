/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.jms.context.auxiliary;

import static jakarta.ejb.TransactionAttributeType.REQUIRED;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@Stateful(passivationCapable = false)
@RequestScoped
public class TransactedMessageProducer {
    @Inject
    private JMSContext context;

    @Resource
    private SessionContext sessionContext;

    @TransactionAttribute(value = REQUIRED)
    public void sendToDestination(Destination destination, String text, boolean rollback) {
        context.createProducer()
                .send(destination, text);
        if (rollback) {
            sessionContext.setRollbackOnly();
        }
    }
}
