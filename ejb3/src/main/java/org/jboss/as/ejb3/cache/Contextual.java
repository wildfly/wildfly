/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

/**
 * Exposes a mechanism for attaching a context to a cached object
 * @author Paul Ferraro
 * @param <C> a cache context
 */
public interface Contextual<C> {
    /**
     * Returns the cache context of this cached object.
     * @return a cache context
     */
    C getCacheContext();

    /**
     * Sets the cache context of this cached object.
     * @param context a cache context
     */
    void setCacheContext(C context);

    /**
     * Removes any cache context associated with this cached object
     * @return the removed cache context
     */
    default C removeCacheContext() {
        C context = this.getCacheContext();
        this.setCacheContext(null);
        return context;
    }
}
