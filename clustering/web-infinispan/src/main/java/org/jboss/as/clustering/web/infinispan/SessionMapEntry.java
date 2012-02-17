/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.web.infinispan;

import java.util.Map;

import org.jboss.as.clustering.web.DistributableSessionMetadata;

/**
 * Enumerates the properties of a session to be stored in a cache entry. Provides get/put methods which encapsulate away the
 * details of the map key.
 *
 * @author Paul Ferraro
 */
public enum SessionMapEntry {
    VERSION(Integer.class), TIMESTAMP(Long.class), METADATA(DistributableSessionMetadata.class), ATTRIBUTES(Object.class);

    private Class<?> targetClass;

    private SessionMapEntry(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    /**
     * Returns the value associated with this atomic map entry.
     *
     * @param <T> the value type
     * @param map an atomic map
     * @return the entry value
     */
    public <T> T get(Map<Object, Object> map) {
        return this.<T> cast(map.get(this.key()));
    }

    /**
     * Add this entry to the specified map if the specified value is non-null.
     *
     * @param <T> the value type
     * @param map an atomic map
     * @param value the entry value
     * @return the old entry value, or null if no previous entry existed
     */
    public <T> T put(Map<Object, Object> map, Object value) throws IllegalArgumentException {
        if (value == null)
            return null;

        if (!this.targetClass.isInstance(value)) {
            throw InfinispanWebMessages.MESSAGES.invalidMapValue(value.getClass().getName(), this);
        }

        return this.<T> cast(map.put(this.key(), value));
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Object value) {
        Class<T> targetClass = (Class<T>) this.targetClass;
        return (value != null) ? targetClass.cast(value) : null;
    }

    private Byte key() {
        return Byte.valueOf((byte) this.ordinal());
    }
}
