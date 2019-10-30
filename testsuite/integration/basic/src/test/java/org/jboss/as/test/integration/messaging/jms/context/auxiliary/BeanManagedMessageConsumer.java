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

package org.jboss.as.test.integration.messaging.jms.context.auxiliary;

import static javax.ejb.TransactionManagementType.BEAN;
import static org.jboss.as.test.integration.messaging.jms.context.ScopedInjectedJMSContextTestCase.QUEUE_NAME_FOR_REQUEST_SCOPE;
import static org.jboss.as.test.integration.messaging.jms.context.ScopedInjectedJMSContextTestCase.QUEUE_NAME_FOR_TRANSACTION_SCOPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSDestinationDefinitions;
import javax.jms.JMSRuntimeException;
import javax.transaction.UserTransaction;

import org.junit.Assert;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@JMSDestinationDefinitions(
        value =  {
                @JMSDestinationDefinition(
                        name = QUEUE_NAME_FOR_TRANSACTION_SCOPE,
                        interfaceName = "javax.jms.Queue",
                        destinationName = "ScopedInjectedJMSContextTestCaseQueue-transactionScope"),
                @JMSDestinationDefinition(
                        name = QUEUE_NAME_FOR_REQUEST_SCOPE,
                        interfaceName = "javax.jms.Queue",
                        destinationName = "ScopedInjectedJMSContextTestCaseQueue-requestScope")
        }
)
@TransactionManagement(BEAN)
@Stateless
public class BeanManagedMessageConsumer {

    @Inject
    private JMSContext context;

    @Resource
    UserTransaction transaction;

    public boolean receive(Destination destination, String expectedText) throws Exception {

        transaction.begin();

        JMSConsumer consumer = context.createConsumer(destination);
        String text = consumer.receiveBody(String.class, 1000);
        assertNotNull(text);
        assertEquals(expectedText, text);

        transaction.commit();

        try {
            consumer.receiveBody(String.class, 1000);
            Assert.fail("call must fail as the injected JMSContext is closed when the transaction is committed");
        } catch (JMSRuntimeException e) {
            // exception is expected
        } catch (Exception e) {
            throw e;
        }

        return true;
    }
}
