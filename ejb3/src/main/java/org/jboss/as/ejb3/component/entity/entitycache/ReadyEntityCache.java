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
package org.jboss.as.ejb3.component.entity.entitycache;

import javax.ejb.NoSuchEntityException;

import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;

/**
 * A cache for entity beans that are in the ready state.
 *
 * @author Stuart Douglas
 */
public interface ReadyEntityCache {


    /**
     * Called after an entity bean has been created and associated with a new identity.
     *
     * This corresponds to an ejbCreate call on the entity bean.
     *
     * The newly created object will be marked as in use, and must be released in the normal manner
     *
     * @param instance The new instance
     */
    void create(EntityBeanComponentInstance instance);

    /**
     * Gets an entity bean instance for the given primary key. This may return a cached instance,
     * or may associate the given ket with a pooled entity.
     *
     * The returned entity will be referenced, and will have its reference count increased by one. It must be de-referenced
     * via {@link #release(org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance, boolean)}
     *
     *
     * Implementors of this method must ensure that repeated calls to get within the same transaction
     * return the same instance for a given primary key. This must also take into account entity beans
     * created in the same transaction using the {@link #create(org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance)}
     * method.
     *
     * Implementations are free to use a 1 to 1 instance -> pk mapping, or create multiple instances per
     * primary key.
     *
     * @param key the identifier of the object
     * @return the object
     * @throws javax.ejb.NoSuchEntityException if the object identity association failed
     */
    EntityBeanComponentInstance get(Object key) throws NoSuchEntityException;


    /**
     * Release the object from use. This will be called at transaction commit time.
     *
     * If the entity bean has been removed it should be released back into the pool.
     *
     * This method is called before the lock on the instance has been released.
     *
     * @param instance The entity
     * @param transactionSuccess True if the transaction succeeded
     */
    void release(EntityBeanComponentInstance instance, boolean transactionSuccess);

    /**
     * Discard the object, called when an exception occurs
     *
     * @param instance The instance to discard
     */
    void discard(EntityBeanComponentInstance instance);

    /**
     * Start the cache.
     */
    void start();

    /**
     * Stop the cache.
     */
    void stop();
}
