/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.deployment.managedbean.container;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Static registry used to manged registration and retrieval of managed bean containers.
 *
 * @author John E. Bailey
 */
public class ManagedBeanRegistry {
    private static final ConcurrentMap<String, ManagedBeanContainer<?>> registry = new ConcurrentHashMap<String, ManagedBeanContainer<?>>();

    /**
     * Register a managed bean service.
     *
     * @param name The service name
     * @param managedBeanContainer The container
     * @throws DuplicateMangedBeanException if multiple instances are registered with the same name
     */
    public static void register(final String name, final ManagedBeanContainer<?> managedBeanContainer) throws DuplicateMangedBeanException {
        if(registry.putIfAbsent(name, managedBeanContainer) != null) {
            throw new DuplicateMangedBeanException("ManagedBean bound to '%s' already exists in registry.", name);
        }
    }

    /**
     * Get a managed bean service.
     *
     * @param name The managed bean service name
     * @return The managed bean service if it exists in the registry, null if no.
     */
    public static ManagedBeanContainer<?> get(final String name) {
        return registry.get(name);
    }

    /**
     * Remove a managed bean service from the registry.
     *
     * @param name The service name
     * @param managedBeanContainer The service
     */
    public static void unregister(final String name, final ManagedBeanContainer<?> managedBeanContainer) {
        registry.remove(name, managedBeanContainer);
    }

    public static class DuplicateMangedBeanException extends Exception {
        private static final long serialVersionUID = -2999486400675276839L;
        public DuplicateMangedBeanException(final String messageFormat, final Object... params) {
            super(String.format(messageFormat, params));
        }
    }
}
