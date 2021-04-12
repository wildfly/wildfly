/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.smoke.jms.auxiliary;

import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactoryDefinition;
import javax.jms.JMSConnectionFactoryDefinitions;
import javax.jms.JMSContext;
import javax.jms.JMSDestinationDefinition;
import javax.jms.Queue;

@JMSDestinationDefinition(
        name = "java:/app/jms/nonXAQueue",
        interfaceName = "javax.jms.Queue"
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
