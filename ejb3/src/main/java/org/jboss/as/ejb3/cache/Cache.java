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
package org.jboss.as.ejb3.cache;

import javax.transaction.TransactionManager;

import org.jboss.ejb.client.SessionID;

/**
 * Cache a stateful object and make sure any life cycle callbacks are
 * called at the appropriate time.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public interface Cache<T extends Identifiable> {
    /**
     * Creates and caches a new instance of <code>T</code>.
     *
     * @return a new <code>T</code>
     */
    T create();

    /**
     * Discard the specified object from cache.
     *
     * @param key the identifier of the object
     */
    void discard(SessionID key);

    /**
     * Get the specified object from cache. This will mark
     * the object as being in use.
     *
     * @param key the identifier of the object
     * @return the object, or null if it does not exist
     */
    T get(SessionID key);

    /**
     * Peek at an object which might be in use.
     *
     * @param key    the identifier of the object
     * @return the object
     * @throws javax.ejb.NoSuchEJBException    if the object does not exist
     */
    //T peek(Serializable key) throws NoSuchEJBException;

    /**
     * Release the object from use.
     *
     * @param obj the object
     */
    void release(T obj);

    /**
     * Remove the specified object from cache.
     *
     * @param key the identifier of the object
     */
    void remove(final TransactionManager transactionManager, SessionID key);

    /**
     * Associate the cache with a stateful object factory.
     *
     * @param factory the factory this cache should use.
     */
    void setStatefulObjectFactory(StatefulObjectFactory<T> factory);

    /**
     * Start the cache.
     */
    void start();

    /**
     * Stop the cache.
     */
    void stop();
}
