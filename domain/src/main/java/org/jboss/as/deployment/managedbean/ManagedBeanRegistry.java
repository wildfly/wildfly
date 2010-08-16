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

package org.jboss.as.deployment.managedbean;

import org.jboss.msc.service.ServiceName;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Static registry used to manged registration and retrieval of managed bean services.
 * 
 * @author John E. Bailey
 */
public class ManagedBeanRegistry {
    private static final ConcurrentMap<ServiceName, ManagedBeanService<?>> registry = new ConcurrentHashMap<ServiceName, ManagedBeanService<?>>();

    /**
     * Register a managed bean service.
     *
     * @param name The service name
     * @param managedBeanService The service
     * @throws DuplicateMangedBeanException if multiple instances are registered with the same name
     */
    public static void register(final ServiceName name, final ManagedBeanService<?> managedBeanService) throws DuplicateMangedBeanException {
        if(registry.putIfAbsent(name, managedBeanService) != null) {
            throw new DuplicateMangedBeanException("ManagedBean bound to '%s' already exists in registry.", name);
        }
    }

    /**
     * Get a managed bean service.
     *
     * @param name The managed bean service name
     * @return The managed bean service if it exists in the registry, null if no.
     */
    public static ManagedBeanService<?> get(final ServiceName name) {
        return registry.get(name);
    }

    /**
     * Remove a managed bean service from the registry.
     *
     * @param name The service name
     * @param managedBeanService The service
     */
    public static void unregister(final ServiceName name, final ManagedBeanService<?> managedBeanService) {
        registry.remove(name, managedBeanService);
    }

    public static class DuplicateMangedBeanException extends Exception {
        public DuplicateMangedBeanException(final String messageFormat, final Object... params) {
            super(String.format(messageFormat, params));
        }
    }
}
