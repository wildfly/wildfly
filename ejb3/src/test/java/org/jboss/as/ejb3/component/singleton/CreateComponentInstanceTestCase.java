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
package org.jboss.as.ejb3.component.singleton;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class CreateComponentInstanceTestCase {
    private Object get(Object obj, Class<?> cls, String fieldName) throws NoSuchFieldException {
        final Field field = cls.getDeclaredField(fieldName);
        field.setAccessible(true);
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetComponentInstance() throws Exception {
        final CountDownLatch done = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(1);
        final SingletonComponent component = mock(SingletonComponent.class);
        when(component.getComponentInstance()).thenCallRealMethod();
        when(component.instantiateComponentInstance(Matchers.<AtomicReference<ManagedReference>>any(), Matchers.<Interceptor>any(), Matchers.<Map<Method, Interceptor>>any())).thenCallRealMethod();
        when(component.createInstance()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ComponentInstance instance = component.instantiateComponentInstance(null, null, new HashMap<Method, Interceptor>());
                done.countDown();
                latch.await(10, SECONDS);
                return instance;
            }
        });
        final ExecutorService service = Executors.newSingleThreadScheduledExecutor();
        Future<ComponentInstance> future = service.submit(new Callable<ComponentInstance>() {
            @Override
            public ComponentInstance call() throws Exception {
                return component.getComponentInstance();
            }
        });
        done.await(10, SECONDS);
        // so far the instance in the component should be null
        assertNull(get(component, SingletonComponent.class, "singletonComponentInstance"));
        latch.countDown();
        assertNotNull(future.get(10, SECONDS));
    }
}
