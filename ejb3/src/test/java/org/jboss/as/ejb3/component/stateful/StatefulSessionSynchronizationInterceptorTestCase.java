/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.stateful;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBean;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCache;
import org.jboss.as.ejb3.concurrency.AccessTimeoutDetails;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
    public void testDifferentTx() throws Exception {
        final Interceptor interceptor = new StatefulSessionSynchronizationInterceptor(true);
        final InterceptorContext context = new InterceptorContext();
        context.setInterceptors(Arrays.asList(noop()));
        final StatefulSessionComponent component = mock(StatefulSessionComponent.class);
        context.putPrivateData(Component.class, component);
        when(component.getAccessTimeout(null)).thenReturn(defaultAccessTimeout());
        StatefulSessionBeanCache<SessionID, StatefulSessionComponentInstance> cache = mock(StatefulSessionBeanCache.class);
        when(component.getCache()).thenReturn(cache);
        Supplier<SessionID> identifierFactory = mock(Supplier.class);
        when(cache.getIdentifierFactory()).thenReturn(identifierFactory);
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

        StatefulSessionBean<SessionID, StatefulSessionComponentInstance> bean = mock(StatefulSessionBean.class);
        when(bean.getInstance()).thenReturn(instance);
        context.putPrivateData(StatefulSessionBean.class, bean);

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
