/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.cmt.timeout;

import javax.naming.InitialContext;

import jakarta.transaction.TransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.wildfly.transaction.client.ContextTransactionManager;

/**
 */
@ExtendWith(ArquillianExtension.class)
public class TransactionTimeoutTestCase {

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test-ejb-cmt-timeout.jar");
        jar.addClass(BeanWithTimeoutValue.class);
        jar.addClass(TimeoutRemoteView.class);
        jar.addClass(TimeoutLocalView.class);
        jar.addAsManifestResource(TransactionTimeoutTestCase.class.getPackage(), "jboss-ejb3.xml",
                "jboss-ejb3.xml");
        jar.addAsManifestResource(TransactionTimeoutTestCase.class.getPackage(), "jboss.properties",
                "jboss.properties");
        return jar;
    }

    /**
     */
    @Test
    public void testBeanTimeouts() throws Exception {
        TimeoutRemoteView remoteView = (TimeoutRemoteView) (new InitialContext()
                .lookup("java:module/BeanWithTimeoutValue!org.jboss.as.test.integration.ejb.transaction.cmt.timeout.TimeoutRemoteView"));
        TimeoutLocalView localView = (TimeoutLocalView) (new InitialContext()
                .lookup("java:module/BeanWithTimeoutValue!org.jboss.as.test.integration.ejb.transaction.cmt.timeout.TimeoutLocalView"));

        long timeoutValue = -1;
        timeoutValue = (long) remoteView.getBeanTimeout();
        Assertions.assertEquals(5L, timeoutValue, "Bean-level timeout failed");
        timeoutValue = (long) remoteView.getBeanMethodTimeout();
        Assertions.assertEquals(6L, timeoutValue, "Bean-method timeout failed");
        timeoutValue = (long) remoteView.getRemoteMethodTimeout();
        Assertions.assertEquals(7L, timeoutValue, "Remote-method timeout failed");
        timeoutValue = (long) localView.getLocalViewTimeout();
        Assertions.assertEquals(5L, timeoutValue, "Local-view timeout failed");
    }

    @Test
    public void testDescriptor() throws Exception {
        final TimeoutLocalView localView = (TimeoutLocalView) new InitialContext()
                .lookup("java:module/DDBeanWithTimeoutValue!" + TimeoutLocalView.class.getName());
        assertEquals(10, localView.getLocalViewTimeout());
    }

    @Test
    public void testDescriptorWithNestedExpressions() throws Exception {
        final TimeoutLocalView localView = (TimeoutLocalView) new InitialContext()
                .lookup("java:module/DDBeanWithTimeoutValueUsingNestedExpression!" + TimeoutLocalView.class
                        .getName());
        assertEquals(90, localView.getLocalViewTimeout());
    }

    @Test
    public void threadStoringTimeout() throws Exception {
        TimeoutLocalView localView = (TimeoutLocalView) (new InitialContext()
            .lookup("java:module/BeanWithTimeoutValue!org.jboss.as.test.integration.ejb.transaction.cmt.timeout.TimeoutLocalView"));
        TransactionManager tm = (TransactionManager) new InitialContext().lookup("java:/TransactionManager");

        int transactionTimeoutToSet = 42;
        tm.setTransactionTimeout(transactionTimeoutToSet);
        Assertions.assertEquals(transactionTimeoutToSet,
                getTransactionTimeout(tm), "Expecting transaction timeout has to be the same as it was written by setter");

        localView.getLocalViewTimeout();

        Assertions.assertEquals(transactionTimeoutToSet,
            getTransactionTimeout(tm), "The transaction timeout has to be the same as before CMT call");
    }

    private int getTransactionTimeout(TransactionManager tmTimeout) {
        if (tmTimeout instanceof ContextTransactionManager) {
            return ((ContextTransactionManager) tmTimeout).getTransactionTimeout();
        }
        throw new IllegalStateException("Cannot get transaction timeout");
    }
}
