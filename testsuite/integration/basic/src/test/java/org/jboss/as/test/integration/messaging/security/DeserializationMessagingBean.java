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

package org.jboss.as.test.integration.messaging.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactoryDefinition;
import javax.jms.JMSConnectionFactoryDefinitions;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@JMSDestinationDefinition(
        name="java:comp/env/myQueue",
        interfaceName="javax.jms.Queue"
)
@JMSConnectionFactoryDefinitions(
        value = {
                @JMSConnectionFactoryDefinition(
                        name = "java:comp/env/myBlackListCF",
                        interfaceName = "javax.jms.QueueConnectionFactory",
                        properties = {
                                "connectors=in-vm",
                                "deserialization-black-list=java.util.UUID"
                        }),
                @JMSConnectionFactoryDefinition(
                        name = "java:comp/env/myWhiteListCF",
                        interfaceName = "javax.jms.QueueConnectionFactory",
                        properties = {
                                "connectors=in-vm",
                                "deserialization-white-list=java.util.UUID"
                        }
                )
        })

@Stateless
public class DeserializationMessagingBean {

    @Resource(lookup = "java:comp/env/myQueue")
    private Queue queue;

    @Resource(lookup = "java:comp/env/myBlackListCF")
    private ConnectionFactory blackListCF;

    @Resource(lookup = "java:comp/env/myWhiteListCF")
    private ConnectionFactory whiteListCF;

    public void send(Serializable serializable) {
        assertNotNull(queue);
        ConnectionFactory cf = blackListCF;
        assertNotNull(cf);

        try (JMSContext context = cf.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
            context.createProducer().send(queue, serializable);
        }
    }

    public void receive(Serializable serializable, boolean useBlackList, boolean consumeMustFail) {
        assertNotNull(queue);
        ConnectionFactory cf = useBlackList ? blackListCF : whiteListCF;
        assertNotNull(cf);

        try (JMSContext context = cf.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
            JMSConsumer consumer = context.createConsumer(queue);
            try {
                Message response = consumer.receive(1000);
                assertNotNull(response);
                assertTrue(response instanceof ObjectMessage);
                Serializable o = ((ObjectMessage)response).getObject();
                if (consumeMustFail) {
                    fail(o + " must not be deserialized when message is consumed");
                } else {
                    assertEquals(serializable, o);
                }
            } catch (JMSException e) {
                if (consumeMustFail) {
                    // exception is expected
                } else {
                    System.out.println("WTF e.getMessage() = " + e.getMessage());
                    e.printStackTrace(System.out);
                    fail(serializable + " is allowed to be deserialized when message is consumed");
                }
            }
        }
    }
}
