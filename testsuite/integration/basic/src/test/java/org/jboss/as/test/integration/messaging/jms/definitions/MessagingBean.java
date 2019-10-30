/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.messaging.jms.definitions;

import static javax.jms.JMSContext.AUTO_ACKNOWLEDGE;
import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactoryDefinition;
import javax.jms.JMSConnectionFactoryDefinitions;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSDestinationDefinitions;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;


/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@JMSDestinationDefinition(
        name="java:comp/env/myQueue4",
        interfaceName="javax.jms.Queue"
)
@JMSDestinationDefinitions(
        value =  {
                @JMSDestinationDefinition(
                        name="java:module/env/myQueue1",
                        interfaceName="javax.jms.Queue",
                        destinationName="myQueue1"
                ),
                @JMSDestinationDefinition(
                        name="java:module/env/myTopic1",
                        interfaceName="javax.jms.Topic",
                        destinationName="myTopic1"
                ),
                @JMSDestinationDefinition(
                        // explicitly mention a resourceAdapter corresponding to a pooled-connection-factory resource
                        resourceAdapter = "activemq-ra",
                        name="java:global/env/myQueue2",
                        interfaceName="javax.jms.Queue",
                        destinationName="myQueue2",
                        properties = {
                                "durable=false",
                                "selector=color = 'red'"
                        }
                )
        }
)
@JMSConnectionFactoryDefinitions(
        value = {
                @JMSConnectionFactoryDefinition(
                        name="java:module/myFactory1",
                        properties = {
                                "connector=http-connector",
                                "initial-connect-attempts=3"
                        },
                        user = "guest",
                        password = "guest",
                        clientId = "myClientID1",
                        maxPoolSize = 2,
                        minPoolSize = 1
                ),
                @JMSConnectionFactoryDefinition(
                        name="java:comp/env/myFactory2"
                ),
                @JMSConnectionFactoryDefinition(
                        name="java:comp/env/myFactory5",
                        interfaceName = "javax.jms.QueueConnectionFactory",
                        user = "${VAULT::messaging::userName::1}",
                        password = "${VAULT::messaging::password::1}"
                )
        }
)
@JMSConnectionFactoryDefinition(
        // explicitly mention a resourceAdapter corresponding to a pooled-connection-factory resource
        resourceAdapter = "activemq-ra",
        name="java:global/myFactory3",
        interfaceName = "javax.jms.QueueConnectionFactory",
        properties = {
                "connector=http-connector",
                "initial-connect-attempts=5"
        },
        user = "guest",
        password = "guest",
        maxPoolSize = 4,
        minPoolSize = 3
)

@Stateless
public class MessagingBean {

    // Use a @JMSDestinationDefinition inside a @JMSDestinationDefinitions
    @Resource(lookup = "java:module/env/myQueue1")
    private Queue queue1;

    // Use a @JMSDestinationDefinition
    @Resource(lookup = "java:global/env/myQueue2")
    private Queue queue2;

    // Use a jms-destination from the deployment descriptor
    @Resource(lookup = "java:app/env/myQueue3")
    private Queue queue3;

    // Use a @JMSDestinationDefinition inside the bean
    @Resource(lookup = "java:comp/env/myQueue4")
    private Queue queue4;

    // Use a @JMSDestinationDefinition inside a @JMSDestinationDefinitions
    @Resource(lookup = "java:module/env/myTopic1")
    private Topic topic1;

    // Use a jms-destination from the deployment descriptor
    @Resource(lookup = "java:app/env/myTopic2")
    private Topic topic2;

    // Use a @JMSConnectionFactoryDefinition inside a @JMSConnectionFactoryDefinitions
    @Resource(lookup = "java:module/myFactory1")
    private ConnectionFactory factory1;

    // Use a @JMSConnectionFactoryDefinition inside a @JMSConnectionFactoryDefinitions
    @Resource(lookup = "java:comp/env/myFactory2")
    private ConnectionFactory factory2;

    // Use a @JMSConnectionFactoryDefinition
    @Resource(lookup = "java:global/myFactory3")
    private QueueConnectionFactory factory3;

    // Use a jms-connection-factory from the deployment descriptor
    @Resource(lookup = "java:app/myFactory4")
    private TopicConnectionFactory factory4;

    // Use a @JMSConnectionFactoryDefinition inside a @JMSConnectionFactoryDefinitions
    // with vaulted attributes for the user credentials
    @Resource(lookup = "java:comp/env/myFactory5")
    private ConnectionFactory factory5;

    // Use a jms-connection-factory from the deployment descriptor
    // with vaulted attributes for the user credentials
    @Resource(lookup = "java:app/myFactory6")
    private TopicConnectionFactory factory6;

    public void checkInjectedResources() {
        assertNotNull(queue1);
        assertNotNull(queue2);
        assertNotNull(queue3);
        assertNotNull(queue4);
        assertNotNull(topic1);
        assertNotNull(topic2);
        assertNotNull(factory1);
        assertNotNull(factory2);
        assertNotNull(factory3);
        assertNotNull(factory4);
        assertNotNull(factory5);
        assertNotNull(factory6);

        JMSContext context = factory3.createContext("guest", "guest", AUTO_ACKNOWLEDGE);
        JMSConsumer consumer = context.createConsumer(queue4);
        assertNotNull(consumer);
        consumer.close();
    }

    public void checkAnnotationBasedDefinitionsWithVaultedAttributes() {
        assertNotNull(factory5);

        // verify authentication using vaulted attributes works fine
        JMSContext context = factory5.createContext();
        assertNotNull(context);
        context.close();
    }

    public void checkDeploymendDescriptorBasedDefinitionsWithVaultedAttributes() {
        assertNotNull(factory6);

        // verify authentication using vaulted attributes works fine
        JMSContext context = factory6.createContext();
        assertNotNull(context);
        context.close();
    }
}
