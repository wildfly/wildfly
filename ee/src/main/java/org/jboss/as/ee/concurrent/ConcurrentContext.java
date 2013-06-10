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

package org.jboss.as.ee.concurrent;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ee.concurrent.interceptors.ConcurrentContextInterceptor;
import org.jboss.as.naming.util.ThreadLocalStack;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.SimpleInterceptorFactoryContext;

import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.ManagedThreadFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * The concurrent context allows the retrieval of the managed objects related with the component in context, and is also responsible for capturing the invocation context, through {@link InterceptorContext}s.
 *
 * @author Eduardo Martins
 */
public class ConcurrentContext {

    private final ConcurrentContextInterceptor concurrentContextInterceptor;
    private final InterceptorContext currentInterceptorContext;

    public ConcurrentContext(ConcurrentContextInterceptor concurrentContextInterceptor, InterceptorContext currentInterceptorContext) {
        this.concurrentContextInterceptor = concurrentContextInterceptor;
        this.currentInterceptorContext = currentInterceptorContext;
    }

    public ContextService getDefaultContextService() {
        return concurrentContextInterceptor.getDefaultContextService().getValue();
    }

    public ManagedExecutorService getDefaultManagedExecutorService() {
        return concurrentContextInterceptor.getDefaultManagedExecutorService().getValue();
    }

    public ManagedScheduledExecutorService getDefaultManagedScheduledExecutorService() {
        return concurrentContextInterceptor.getDefaultManagedScheduledExecutorService().getValue();
    }

    public ManagedThreadFactory getDefaultManagedThreadFactory() {
        return concurrentContextInterceptor.getDefaultManagedThreadFactory().getValue();
    }

    public InterceptorContext getDefaultInterceptorContext() {
        final InterceptorContext interceptorContext = currentInterceptorContext.clone();
        interceptorContext.putPrivateData(InvocationType.class, InvocationType.CONCURRENT_CONTEXT);
        final SimpleInterceptorFactoryContext interceptorFactoryContext = new SimpleInterceptorFactoryContext();
        // required by component interceptor factories
        interceptorFactoryContext.getContextData().put(Component.class, interceptorContext.getPrivateData(Component.class));
        final List<Interceptor> interceptors = new ArrayList<>();
        for (InterceptorFactory interceptorFactory : concurrentContextInterceptor.getComponentConfiguration().getDefaultConcurrentContextInterceptors()) {
            interceptors.add(interceptorFactory.create(interceptorFactoryContext));
        }
        interceptorContext.setInterceptors(interceptors);
        return interceptorContext;
    }

    public InterceptorContext getDefaultManagedThreadFactoryInterceptorContext() {
        // TODO by the spec the thread factory should have a predefined interceptor context which for instance does have a specific security context, yet I'm still not convinced that is really needed or wanted...
        return getDefaultInterceptorContext();
    }

    // current thread concurrent context management

    /**
     * a thread local stack with the contexts pushed
     */
    private static ThreadLocalStack<ConcurrentContext> current = new ThreadLocalStack<ConcurrentContext>();

    /**
     * Sets the specified context as the current one, in the current thread.
     *
     * @param context The current context
     */
    public static void pushCurrent(final ConcurrentContext context) {
        current.push(context);
    }

    /**
     * Pops the current context in the current thread.
     *
     * @return
     */
    public static ConcurrentContext popCurrent() {
        return current.pop();
    }

    /**
     * Retrieves the current context in the current thread.
     *
     * @return
     */
    public static ConcurrentContext current() {
        return current.peek();
    }


}
