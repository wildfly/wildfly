/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.tx2.impl;

import org.jboss.as.ejb3.tx.ApplicationExceptionDetails;
import org.jboss.as.ejb3.tx.StatefulBMTInterceptor;
import org.jboss.as.ejb3.tx.TransactionalInvocationContext;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.ejb.EJBException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulBMTInterceptorTestCase {
    private static final Logger logger = Logger.getLogger(StatefulBMTInterceptorTestCase.class);

    private TransactionManager transactionManager;

    @Before
    public void beforeTest() {
        this.transactionManager = new MockTransactionManager();
    }

    @Test
    public void test1() throws Exception {
        final String componentName = "Test";

        StatefulBMTInterceptor interceptor = new StatefulBMTInterceptor() {
            @Override
            protected String getComponentName() {
                return componentName;
            }

            @Override
            protected TransactionManager getTransactionManager() {
                return transactionManager;
            }
        };

        TransactionalInvocationContext invocation = mock(TransactionalInvocationContext.class);
        when(invocation.proceed()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                transactionManager.begin();
                return transactionManager.getTransaction();
            }
        });
        Transaction expected = (Transaction) interceptor.invoke(invocation);

        assertNotNull(expected);
        assertSame(expected, interceptor.getTransaction());
    }

    @Test
    public void testApplicationException() throws Exception {
        ApplicationExceptionDetails ae = new ApplicationExceptionDetails("", false, false);

        StatefulBMTInterceptor statefulBMTInterceptor = new StatefulBMTInterceptor() {
            @Override
            protected String getComponentName() {
                return "appexception-test";
            }

            @Override
            protected TransactionManager getTransactionManager() {
                return transactionManager;
            }
        };
        TransactionalInvocationContext invocation = mock(TransactionalInvocationContext.class);
        when(invocation.getApplicationException(SimpleApplicationException.class)).thenReturn(ae);
        when(invocation.proceed()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // throw the application exception
                throw new SimpleApplicationException();
            }
        });

        try {
            // invoke on the interceptor
            statefulBMTInterceptor.invoke(invocation);
        } catch (SimpleApplicationException sae) {
            // expected
            logger.debug("Got the expected " + SimpleApplicationException.class.getName() + " exception: ", sae);
        }

    }

    @Test
    public void testSystemException() throws Exception {
        StatefulBMTInterceptor statefulBMTInterceptor = new StatefulBMTInterceptor() {
            @Override
            protected String getComponentName() {
                return "appexception-test";
            }

            @Override
            protected TransactionManager getTransactionManager() {
                return transactionManager;
            }
        };
        TransactionalInvocationContext invocation = mock(TransactionalInvocationContext.class);
        when(invocation.proceed()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // throw the system exception
                throw new SimpleSystemException();
            }
        });

        try {
            // invoke on the interceptor
            statefulBMTInterceptor.invoke(invocation);
        } catch (EJBException ejbe) {
            // expected
            logger.debug("Got the expected " + EJBException.class.getName() + " exception: ", ejbe);
            Assert.assertNotNull("No cause found in EJBException", ejbe.getCause());
            Assert.assertEquals("Unexpected cause in EJBException", SimpleSystemException.class, ejbe.getCause().getClass());
        }

    }

    private class SimpleApplicationException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private class SimpleSystemException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
