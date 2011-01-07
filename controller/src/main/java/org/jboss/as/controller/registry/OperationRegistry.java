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

package org.jboss.as.controller.registry;

import java.util.Collections;
import java.util.ListIterator;
import java.util.Map;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;

/**
 * A registry of model operations.  This registry is thread-safe.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class OperationRegistry {

    private final String valueString;
    private final Subregistry parent;

    OperationRegistry(final String valueString, final Subregistry parent) {
        this.valueString = valueString;
        this.parent = parent;
    }

    /**
     * Create a new empty registry.
     *
     * @return the new registry
     */
    public static OperationRegistry create() {
        return new ConcreteOperationRegistry(null, null);
    }

    /**
     * Get a handler at a specific address.
     *
     * @param pathAddress the address
     * @param operationName the operation name
     * @return the operation handler, or {@code null} if none match
     */
    public final OperationHandler getHandler(PathAddress pathAddress, String operationName) {
        return getHandler(pathAddress.iterator(), operationName);
    }

    abstract OperationHandler getHandler(ListIterator<PathElement> iterator, String operationName);

    /**
     * Get all the handlers at a specific address.
     *
     * @param address the address
     * @return the handlers
     */
    public Map<String, OperationHandler> getHandlers(PathAddress address) {
        // might be a direct view, might be a copy - so just be safe
        return Collections.unmodifiableMap(getHandlers(address.iterator()));
    }

    abstract Map<String, OperationHandler> getHandlers(ListIterator<PathElement> iterator);

    /**
     * Register an operation handler.
     *
     * @param pathAddress the applicable path
     * @param operationName the operation name
     * @param handler the handler for the operation
     * @throws IllegalArgumentException if a handler is already registered at that location
     */
    public void register(PathAddress pathAddress, String operationName, OperationHandler handler) {
        register(pathAddress.iterator(), operationName, handler);
    }

    abstract void register(ListIterator<PathElement> iterator, String operationName, OperationHandler handler);

    /**
     * Register a proxy handler that will handle all requests at or below the given address.
     *
     * @param pathAddress the address to proxy
     * @param handler the handler to proxy to
     */
    public void registerProxyHandler(PathAddress pathAddress, OperationHandler handler) {
        registerProxyHandler(pathAddress.iterator(), handler);
    }

    abstract void registerProxyHandler(ListIterator<PathElement> iterator, OperationHandler handler);

    final String getLocationString() {
        if (parent == null) {
            return "";
        } else {
            return parent.getLocationString() + valueString + ")";
        }
    }

}
