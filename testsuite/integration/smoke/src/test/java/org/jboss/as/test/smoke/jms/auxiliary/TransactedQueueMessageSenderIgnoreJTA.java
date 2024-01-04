/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jms.auxiliary;

import org.jboss.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.enterprise.context.RequestScoped;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConnectionFactoryDefinition;
import jakarta.jms.JMSConnectionFactoryDefinitions;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Queue;

@JMSDestinationDefinition(
        name = "java:/app/jms/nonXAQueue",
        interfaceName = "jakarta.jms.Queue"
)

@JMSConnectionFactoryDefinitions(
        value = {
                @JMSConnectionFactoryDefinition(
                        name = "java:/jms/nonXAcf",
                        transactional = false,
                        properties = {
                                "connectors=in-vm",}
                ),

                @JMSConnectionFactoryDefinition(
                        name = "java:/jms/XAcf",
                        properties = {
                                "connectors=in-vm",}
                )
        }
)

/**
 * Auxiliary class for Jakarta Messaging smoke tests - sends messages to a queue from within a transaction with different value in JMSConnectionFactoryDefinition annotation's transactional attribute
 * Test of fix for WFLY-9762
 *
 * @author <a href="jondruse@redhat.com">Jiri Ondrusek</a>
 */
@Stateful
@RequestScoped
public class TransactedQueueMessageSenderIgnoreJTA {

    private static final Logger logger = Logger.getLogger(TransactedQueueMessageSenderIgnoreJTA.class);

    @Resource(lookup = "java:/app/jms/nonXAQueue")
    private Queue queue;

    @Resource(lookup = "java:/ConnectionFactory")
    private ConnectionFactory factory;

    @Resource(lookup = "java:/jms/XAcf")
    private ConnectionFactory factoryXA;

    @Resource(lookup = "java:/jms/nonXAcf")
    private ConnectionFactory factoryNonXA;

    @Resource
    private SessionContext ctx;

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void send(boolean ignoreJTA, boolean rollback) throws Exception {
        try (JMSContext context = getFactory(ignoreJTA).createContext()) {
            context.createProducer().send(queue, "test");
            if (rollback) {
                ctx.setRollbackOnly();
            }
        }
    }

    private ConnectionFactory getFactory(boolean ignoreJTA) {
        return ignoreJTA ? factoryNonXA : factoryXA;
    }
}
