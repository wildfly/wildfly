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

import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.LogContextSelector;

/**
 * The log context selector to use for the WildFly logging extension.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface WildFlyLogContextSelector extends LogContextSelector {

    /**
     * Get and set the log context.
     *
     * @param securityKey the security key to check (ignored if none was set on construction)
     * @param newValue    the new log context value, or {@code null} to clear
     *
     * @return the previous log context value, or {@code null} if none was set
     *
     * @see org.jboss.logmanager.ThreadLocalLogContextSelector#getAndSet(Object, org.jboss.logmanager.LogContext)
     */
    LogContext getAndSet(Object securityKey, LogContext newValue);

    /**
     * Register a class loader with a log context.
     *
     * @param classLoader the class loader
     * @param logContext  the log context
     *
     * @throws IllegalArgumentException if the class loader is already associated with a log context
     * @see org.jboss.logmanager.ClassLoaderLogContextSelector#registerLogContext(ClassLoader,
     * org.jboss.logmanager.LogContext)
     */
    void registerLogContext(ClassLoader classLoader, LogContext logContext);

    /**
     * Unregister a class loader/log context association.
     *
     * @param classLoader the class loader
     * @param logContext  the log context
     *
     * @return {@code true} if the association exists and was removed, {@code false} otherwise
     *
     * @see org.jboss.logmanager.ClassLoaderLogContextSelector#unregisterLogContext(ClassLoader,
     * org.jboss.logmanager.LogContext)
     */
    boolean unregisterLogContext(ClassLoader classLoader, LogContext logContext);

    /**
     * Register a class loader which is a known log API, and thus should be skipped over when searching for the
     * log context to use for the caller class.
     *
     * @param apiClassLoader the API class loader
     *
     * @return {@code true} if this class loader was previously unknown, or {@code false} if it was already
     * registered
     *
     * @see org.jboss.logmanager.ClassLoaderLogContextSelector#addLogApiClassLoader(ClassLoader)
     */
    boolean addLogApiClassLoader(ClassLoader apiClassLoader);

    /**
     * Remove a class loader from the known log APIs set.
     *
     * @param apiClassLoader the API class loader
     *
     * @return {@code true} if the class loader was removed, or {@code false} if it was not known to this selector
     *
     * @see org.jboss.logmanager.ClassLoaderLogContextSelector#removeLogApiClassLoader(ClassLoader)
     */
    boolean removeLogApiClassLoader(ClassLoader apiClassLoader);

    /**
     * Returns the number of registered {@link org.jboss.logmanager.LogContext log contexts}.
     *
     * @return the number of registered log contexts
     */
    int registeredCount();

    class Factory {
        public static WildFlyLogContextSelector create() {
            return new WildFlyLogContextSelectorImpl();
        }
    }

}
