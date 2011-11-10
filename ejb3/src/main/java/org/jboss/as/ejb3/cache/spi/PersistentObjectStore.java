/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.cache.spi;

import org.jboss.as.ejb3.cache.Identifiable;

/**
 * Stores an indentifiable object in a persistent store. Note that the object store does NOT call any callbacks.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @author Paul Ferraro
 */
public interface PersistentObjectStore<K, V extends Identifiable<K>> {
    /**
     * Load the object from storage.
     *
     * @param key the object identifier. It is assumed the key represents something meaningful to the object store.
     * @return the object or null if not found
     */
    V load(K key);

    /**
     * Store the object into storage.
     *
     * @param obj the object
     */
    void store(V obj);

    /**
     * Perform any initialization work.
     */
    void start();

    /**
     * Perform any shutdown work.
     */
    void stop();
}
