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
package org.jboss.as.ejb3.component.pool;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.ejb3.pool.Pool;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.junit.Test;

import static org.jboss.invocation.Interceptors.getChainedInterceptor;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class PooledInstanceInterceptorTestCase {
    private static Interceptor chain(Interceptor... interceptors) {
        return getChainedInterceptor(interceptors);
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
     * EJB 3.1 FR 4.7.1 release an instance back to the pool after each invocation.
     */
    @Test
    public void testRelease() throws Exception {
        final PooledInstanceInterceptor interceptor = PooledInstanceInterceptor.INSTANCE;
        final InterceptorContext context = new InterceptorContext();
        final PooledComponent<ComponentInstance> component = mock(PooledComponent.class);
        final Pool<ComponentInstance> pool = mock(Pool.class);
        when(component.getPool()).thenReturn(pool);
        context.putPrivateData(Component.class, component);
        chain(interceptor, noop()).processInvocation(context);
        verify(pool).get();
        verify(pool).release((ComponentInstance) any());
        verifyNoMoreInteractions(pool);
    }
}
