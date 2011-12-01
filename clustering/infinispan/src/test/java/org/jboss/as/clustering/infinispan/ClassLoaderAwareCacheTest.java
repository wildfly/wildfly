/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.net.URLClassLoader;

import org.infinispan.AdvancedCache;
import org.infinispan.commands.VisitableCommand;
import org.infinispan.commands.read.KeySetCommand;
import org.infinispan.container.DataContainer;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.Event;
import org.jboss.as.clustering.infinispan.ClassLoaderAwareCache.ClassLoaderAwareListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;


public class ClassLoaderAwareCacheTest {
    @SuppressWarnings("unchecked")
    private final AdvancedCache<Object, Object> mockCache = mock(AdvancedCache.class);
    final ClassLoader loader = new URLClassLoader(new URL[0]);
    private AdvancedCache<Object, Object> cache;
    
    @Before
    public void before() throws Throwable {
        ArgumentCaptor<CommandInterceptor> capturedInterceptor = ArgumentCaptor.forClass(CommandInterceptor.class);
        
        cache = new ClassLoaderAwareCache<Object, Object>(mockCache, this.loader);

        verify(this.mockCache).addInterceptor(capturedInterceptor.capture(), eq(0));

        assertNotSame(Thread.currentThread().getContextClassLoader(), this.cache.getClassLoader());
        
        Object result = new Object();
        CommandInterceptor interceptor = capturedInterceptor.getValue();
        TestInterceptor test = new TestInterceptor(this.loader, result);
        
        interceptor.setNext(test);
        
        // Validate that ClassLoaderAwareCommandInterceptor functions correctly
        assertSame(result, interceptor.visitKeySetCommand(mock(InvocationContext.class), new KeySetCommand(mock(DataContainer.class))));
        
        assertTrue(test.triggered);
    }
    
    public static class TestInterceptor extends CommandInterceptor {
        boolean triggered = false;
        final ClassLoader loader;
        final Object result;
        
        TestInterceptor(ClassLoader loader, Object result) {
            this.loader = loader;
            this.result = result;
        }
        
        @Override
        protected Object handleDefault(InvocationContext ctx, VisitableCommand command) throws Throwable {
            triggered = true;
            // Validate that TCCL uses expected classloader
            assertSame(this.loader, Thread.currentThread().getContextClassLoader());
            return this.result;
        }
    }
    
    @Test
    public void getClassLoader() {
        assertSame(this.loader, this.cache.getClassLoader());
    }
    
    @Test
    public void getAdvancedCache() {
        assertSame(this.cache, this.cache.getAdvancedCache());
    }
    
    @Test
    public void with() {
        assertSame(this.cache, this.cache.with(Thread.currentThread().getContextClassLoader()));
    }

    @Test
    public void withFlags() {
        AdvancedCache<Object, Object> flaggedCache = mock(AdvancedCache.class);
        
        when(this.mockCache.withFlags(Flag.CACHE_MODE_LOCAL)).thenReturn(flaggedCache);
        
        AdvancedCache<Object, Object> result = this.cache.withFlags(Flag.CACHE_MODE_LOCAL);
        
        assertNotSame(this.cache, result);
        
        result.clear();
        
        verify(flaggedCache).clear();
    }

    @Test
    public void addListener() throws Throwable {
        ArgumentCaptor<ClassLoaderAwareListener> capturedListener = ArgumentCaptor.forClass(ClassLoaderAwareListener.class);

        TestListener testListener = new TestListener();
        this.cache.addListener(testListener);
        
        verify(this.mockCache).addListener(capturedListener.capture());
        
        ClassLoaderAwareListener listener = capturedListener.getValue();
        
        assertNotNull(listener);
        
        @SuppressWarnings("unchecked")
        CacheEntryActivatedEvent<Object, Object> event = mock(CacheEntryActivatedEvent.class);
        when(event.getType()).thenReturn(Event.Type.CACHE_ENTRY_ACTIVATED);
        when(event.getCache()).thenReturn(this.cache);
        
        // Validate that triggering this event, will trigger the next one
        listener.event(event);
        
        // Verify that event was actually triggered
        assertTrue(testListener.triggered);
    }
    
    @Listener
    public static class TestListener {
        boolean triggered = false;
        @CacheEntryActivated
        public void activated(CacheEntryActivatedEvent<Object, Object> event) {
            triggered = true;
            // Verify that the TCCL is set to the class loader of the cache
            assertSame(event.getCache().getAdvancedCache().getClassLoader(), Thread.currentThread().getContextClassLoader());
        }
    }
}
