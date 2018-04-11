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
package org.jboss.as.ejb3.component.stateful;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.concurrency.AccessTimeoutDetails;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulSessionSynchronizationInterceptorTestCase {
    private static AccessTimeoutDetails defaultAccessTimeout() {
        return new AccessTimeoutDetails(5, TimeUnit.MINUTES);
    }

    private static Interceptor noop() {
        return new Interceptor() {
            @Override
            public Object processInvocation(InterceptorContext context) throws Exception {
                return null;
            }
        };
    }


    /**
     * After the bean is accessed within a tx and the tx has committed, the
     * association should be gone (and thus it is ready for another tx).
     */
    @Test
    @Ignore("WFLY-10176: Relies on obsolete details of component implementation")
    public void testDifferentTx() throws Exception {
        final Interceptor interceptor = new StatefulSessionSynchronizationInterceptor(true);
        final InterceptorContext context = new InterceptorContext();
        context.setInterceptors(Arrays.asList(noop()));
        final StatefulSessionComponent component = mock(StatefulSessionComponent.class);
        context.putPrivateData(Component.class, component);
        when(component.getAccessTimeout(null)).thenReturn(defaultAccessTimeout());
        Cache<SessionID, StatefulSessionComponentInstance> cache = mock(Cache.class);
        when(component.getCache()).thenReturn(cache);
        final TransactionSynchronizationRegistry transactionSynchronizationRegistry = mock(TransactionSynchronizationRegistry.class);
        when(component.getTransactionSynchronizationRegistry()).thenReturn(transactionSynchronizationRegistry);
        when(transactionSynchronizationRegistry.getTransactionKey()).thenReturn("TX1");
        final List<Synchronization> synchronizations = new LinkedList<Synchronization>();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Synchronization synchronization = (Synchronization) invocation.getArguments()[0];
                synchronizations.add(synchronization);
                return null;
            }
        }).when(transactionSynchronizationRegistry).registerInterposedSynchronization((Synchronization) any());
        final StatefulSessionComponentInstance instance = new StatefulSessionComponentInstance(component, org.jboss.invocation.Interceptors.getTerminalInterceptor(), Collections.EMPTY_MAP, Collections.emptyMap());
        context.putPrivateData(ComponentInstance.class, instance);

        interceptor.processInvocation(context);

        // commit
        for (Synchronization synchronization : synchronizations) {
            synchronization.beforeCompletion();
        }
        for (Synchronization synchronization : synchronizations) {
            synchronization.afterCompletion(Status.STATUS_COMMITTED);
        }
        synchronizations.clear();

        when(transactionSynchronizationRegistry.getTransactionKey()).thenReturn("TX2");

        interceptor.processInvocation(context);
    }
}
