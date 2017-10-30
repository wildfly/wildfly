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
package org.jboss.as.test.compat.jpa.eclipselink.wildfly8954;

/**
 * This event here is interesting because it will be reporting the old value that existed on an entity and the new value that
 * should have been committed. With this information we can determine if we are dealing with a stale entity. A stale entity will
 * show us the old value.
 */
class SomeEntityChangeEvent {

    final String oldValue;
    final String newValue;
    final Integer someEntityId;

    /**
     * Create a new SomeEntityChangeEvent.
     *
     * @param oldValue
     * @param newValue
     * @param someEntityId
     */
    public SomeEntityChangeEvent(String oldValue, String newValue, Integer someEntityId) {
        super();
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.someEntityId = someEntityId;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public Integer getSomeEntityId() {
        return someEntityId;
    }

    /**
     * Check if we are being given a stale value.
     *
     * <P>
     * NOTE: <br>
     * To ensure the value passed is read from the shared cache, ensure that the business transaction that is observing the
     * event is behind a transaction requires new. This will ensure that eclipselink first level cache must be empty an when an
     * entity is fetched by its primary key the entiy is coming from the shared/session cache.
     *
     * @param valueReadFromSharedCache an arbitrary value we want to compare to the "new value" that should be found on the
     *        entity.
     *
     * @return TRUE - if the value given is not the same as the new value on the event. This would most likely be an indication
     *         that the value in hand is stale.
     *
     */
    public boolean isValueReadFromSharedCacheStale(String valueReadFromSharedCache) {
        return !newValue.equals(valueReadFromSharedCache);
    }

    @Override
    public String toString() {
        return "SomeEntityChangeEvent [oldValue=" + oldValue + ", newValue=" + newValue + ", someEntityId=" + someEntityId + "]";
    }

}
