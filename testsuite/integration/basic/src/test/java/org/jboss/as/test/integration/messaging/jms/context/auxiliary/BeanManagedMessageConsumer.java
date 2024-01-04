/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.jms.context.auxiliary;

import static jakarta.ejb.TransactionManagementType.BEAN;
import static org.jboss.as.test.integration.messaging.jms.context.ScopedInjectedJMSContextTestCase.QUEUE_NAME_FOR_REQUEST_SCOPE;
import static org.jboss.as.test.integration.messaging.jms.context.ScopedInjectedJMSContextTestCase.QUEUE_NAME_FOR_TRANSACTION_SCOPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.inject.Inject;
import jakarta.jms.Destination;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.JMSDestinationDefinitions;
import jakarta.jms.JMSRuntimeException;
import jakarta.transaction.UserTransaction;

import org.jboss.as.test.shared.TimeoutUtil;
import org.junit.Assert;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@JMSDestinationDefinitions(
        value =  {
                @JMSDestinationDefinition(
                        name = QUEUE_NAME_FOR_TRANSACTION_SCOPE,
                        interfaceName = "jakarta.jms.Queue",
                        destinationName = "ScopedInjectedJMSContextTestCaseQueue-transactionScope"),
                @JMSDestinationDefinition(
                        name = QUEUE_NAME_FOR_REQUEST_SCOPE,
                        interfaceName = "jakarta.jms.Queue",
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
        String text = consumer.receiveBody(String.class, TimeoutUtil.adjust(1000));
        assertNotNull(text);
        assertEquals(expectedText, text);

        transaction.commit();

        try {
            consumer.receiveBody(String.class, TimeoutUtil.adjust(1000));
            Assert.fail("call must fail as the injected JMSContext is closed when the transaction is committed");
        } catch (JMSRuntimeException e) {
            // exception is expected
        } catch (Exception e) {
            throw e;
        }

        return true;
    }
}
