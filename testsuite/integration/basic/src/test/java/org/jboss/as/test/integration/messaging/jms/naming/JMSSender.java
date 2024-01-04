/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.messaging.jms.naming;


import static jakarta.ejb.TransactionAttributeType.NOT_SUPPORTED;

import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConnectionFactoryDefinition;
import jakarta.jms.JMSConnectionFactoryDefinitions;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;

@JMSDestinationDefinition(
    name = "java:app/jms/queue",
    interfaceName = "jakarta.jms.Queue"
)
@JMSConnectionFactoryDefinitions(
    value = {
        @JMSConnectionFactoryDefinition(
            name = "java:app/jms/nonXAconnectionFactory",
            transactional = false,
            properties = {
                "connectors=${org.jboss.messaging.default-connector:in-vm}",}
        )
    }
)
@Singleton
public class JMSSender {
    @Resource(lookup = "java:app/jms/nonXAconnectionFactory")
    private ConnectionFactory connectionFactory;

    @Resource(lookup = "java:app/jms/queue")
    private Queue queue;

    @TransactionAttribute(NOT_SUPPORTED)
    public void sendMessage(String payload) {
        try (JMSContext context = connectionFactory.createContext()) {
            JMSProducer producer = context.createProducer();
            producer.send(queue, payload);
        }
    }
}
