/*
 * Copyright 2018 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.messaging.jms.naming;


import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;

import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactoryDefinition;
import javax.jms.JMSConnectionFactoryDefinitions;
import javax.jms.JMSContext;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSProducer;
import javax.jms.Queue;

@JMSDestinationDefinition(
    name = "java:app/jms/queue",
    interfaceName = "javax.jms.Queue"
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
