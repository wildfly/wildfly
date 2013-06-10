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

package org.jboss.as.ee.concurrent.interceptors;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.concurrent.ConcurrentContext;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.value.InjectedValue;

import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.ManagedThreadFactory;

/**
 * The interceptor responsible for managing the current {@link org.jboss.as.ee.concurrent.ConcurrentContext}.
 *
 * @author Eduardo Martins
 */
public class ConcurrentContextInterceptor implements Interceptor {

    private final InjectedValue<ContextService> defaultContextService;
    private final InjectedValue<ManagedExecutorService> defaultManagedExecutorService;
    private final InjectedValue<ManagedScheduledExecutorService> defaultManagedScheduledExecutorService;
    private final InjectedValue<ManagedThreadFactory> defaultManagedThreadFactory;
    private final ComponentConfiguration componentConfiguration;

    public ConcurrentContextInterceptor(ComponentConfiguration componentConfiguration) {
        this.componentConfiguration = componentConfiguration;
        defaultContextService = new InjectedValue<>();
        defaultManagedExecutorService = new InjectedValue<>();
        defaultManagedScheduledExecutorService = new InjectedValue<>();
        defaultManagedThreadFactory = new InjectedValue<>();
    }

    public InjectedValue<ContextService> getDefaultContextService() {
        return defaultContextService;
    }

    public InjectedValue<ManagedExecutorService> getDefaultManagedExecutorService() {
        return defaultManagedExecutorService;
    }

    public InjectedValue<ManagedScheduledExecutorService> getDefaultManagedScheduledExecutorService() {
        return defaultManagedScheduledExecutorService;
    }

    public InjectedValue<ManagedThreadFactory> getDefaultManagedThreadFactory() {
        return defaultManagedThreadFactory;
    }

    public ComponentConfiguration getComponentConfiguration() {
        return componentConfiguration;
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final ConcurrentContext concurrentContext = new ConcurrentContext(this, context);
        ConcurrentContext.pushCurrent(concurrentContext);
        try {
            return context.proceed();
        } finally {
            ConcurrentContext.popCurrent();
        }
    }
}