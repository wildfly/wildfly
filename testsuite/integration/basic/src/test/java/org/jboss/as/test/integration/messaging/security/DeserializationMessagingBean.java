/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.security;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConnectionFactoryDefinition;
import jakarta.jms.JMSConnectionFactoryDefinitions;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@JMSDestinationDefinition(
        name="java:comp/env/myQueue",
        interfaceName="jakarta.jms.Queue"
)
@JMSConnectionFactoryDefinitions(
        value = {
                @JMSConnectionFactoryDefinition(
                        name = "java:comp/env/myBlockListCF",
                        interfaceName = "jakarta.jms.QueueConnectionFactory",
                        properties = {
                                "connectors=${org.jboss.messaging.default-connector:in-vm}",
                                "deserialization-black-list=java.util.UUID"
                        }),
                @JMSConnectionFactoryDefinition(
                        name = "java:comp/env/myAllowListCF",
                        interfaceName = "jakarta.jms.QueueConnectionFactory",
                        properties = {
                                "connectors=${org.jboss.messaging.default-connector:in-vm}",
                                "deserialization-white-list=java.util.UUID"
                        }
                )
        })

@Stateless
public class DeserializationMessagingBean {

    public static final String BLACK_LIST_CF_LOOKUP = "java:comp/env/myBlockListCF";
    public static final String WHITE_LIST_CF_LOOKUP = "java:comp/env/myAllowListCF";
    public static final String BLACK_LIST_REGULAR_CF_LOOKUP = "java:/jms/myBlockListCF";
    static final Logger log = Logger.getLogger(DeserializationMessagingBean.class);

    @Resource(lookup = "java:comp/env/myQueue")
    private Queue queue;

    public void send(Serializable serializable) throws NamingException {
        assertNotNull(queue);

        Context namingContext = new InitialContext();
        ConnectionFactory cf = (ConnectionFactory) namingContext.lookup(BLACK_LIST_CF_LOOKUP);
        assertNotNull(cf);

        try (JMSContext context = cf.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
            context.createProducer().send(queue, serializable);
        }
    }

    public void receive(Serializable serializable, String cfLookup, boolean consumeMustFail) throws NamingException {
        assertNotNull(queue);

        Context namingContext = new InitialContext();
        ConnectionFactory cf = (ConnectionFactory) namingContext.lookup(cfLookup);

        try (
                JMSContext context = cf.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
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
                    log.trace("Expected JMSException", e);
                } else {
                    log.error("Unexpected exception", e);
                    fail(serializable + " is allowed to be deserialized when message is consumed");
                }
            }
        }
    }
}
