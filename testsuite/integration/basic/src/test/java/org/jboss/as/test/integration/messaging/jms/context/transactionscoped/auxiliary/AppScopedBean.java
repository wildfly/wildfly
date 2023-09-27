/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.jms.context.transactionscoped.auxiliary;


import static org.jboss.as.test.integration.messaging.jms.context.transactionscoped.TransactionScopedJMSContextTestCase.QUEUE_NAME;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;
import jakarta.transaction.Transactional;


@ApplicationScoped
@Transactional
public class AppScopedBean {

    @Inject
    private JMSContext context;

    @Resource(lookup = QUEUE_NAME)
    private Queue queue;

    public void sendMessage() {
        JMSProducer producer = context.createProducer();
        producer.send(queue, "a message");
    }

}
