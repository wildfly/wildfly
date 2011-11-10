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

import java.io.Serializable;

/**
 * @author Paul Ferraro
 *
 * @param <K> type of the identifier of this cacheable object
 */
public interface Cacheable<K extends Serializable> extends Serializable, Identifiable<K> {
    /**
     * Gets whether this object's internal state has been modified since
     * the last request to this method.
     * <p>
     * Implementations must be aggressive about returning <code>true</code> if
     * they are uncertain about whether they have been modified; <code>false</code>
     * should only be returned if the implementing object is certain its
     * internal state has not been modified.
     * </p>
     *
     * @return <code>true</code> if the state has been modified or the
     *         implementing object does not know; <code>false</code> otherwise.
     */
    boolean isModified();
}
