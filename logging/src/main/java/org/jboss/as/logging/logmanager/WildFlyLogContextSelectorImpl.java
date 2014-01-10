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

package org.jboss.as.logging.logmanager;

import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logmanager.ClassLoaderLogContextSelector;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.ThreadLocalLogContextSelector;

/**
* @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
*/
class WildFlyLogContextSelectorImpl implements WildFlyLogContextSelector {

    private final ClassLoaderLogContextSelector contextSelector;

    private final ThreadLocalLogContextSelector threadLocalContextSelector;

    private final AtomicInteger counter;

    public WildFlyLogContextSelectorImpl() {
        counter = new AtomicInteger(0);
        contextSelector = new ClassLoaderLogContextSelector();
        threadLocalContextSelector = new ThreadLocalLogContextSelector(contextSelector);
    }

    @Override
    public LogContext getLogContext() {
        return threadLocalContextSelector.getLogContext();
    }

    @Override
    public LogContext getAndSet(final Object securityKey, final LogContext newValue) {
        return threadLocalContextSelector.getAndSet(securityKey, newValue);
    }

    @Override
    public void registerLogContext(final ClassLoader classLoader, final LogContext logContext) {
        contextSelector.registerLogContext(classLoader, logContext);
        counter.incrementAndGet();
    }

    @Override
    public boolean unregisterLogContext(final ClassLoader classLoader, final LogContext logContext) {
        if (contextSelector.unregisterLogContext(classLoader, logContext)) {
            counter.decrementAndGet();
            return true;
        }
        return false;
    }

    @Override
    public boolean addLogApiClassLoader(final ClassLoader apiClassLoader) {
        return contextSelector.addLogApiClassLoader(apiClassLoader);
    }

    @Override
    public boolean removeLogApiClassLoader(final ClassLoader apiClassLoader) {
        return contextSelector.removeLogApiClassLoader(apiClassLoader);
    }

    @Override
    public int registeredCount() {
        return counter.get();
    }
}
