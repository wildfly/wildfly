/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.timerservice.persistence;

import java.util.List;

/**
 * @author Stuart Douglas
 */
public interface TimerPersistence {

    /**
     * Called when a timer is being persisted
     *
     * @param timerEntity
     */
    void addTimer(TimerEntity timerEntity);

    /**
     * Called when a timer is being persisted
     *
     * @param timerEntity
     */
    void persistTimer(TimerEntity timerEntity);

    /**
     * Signals that a timer is being undeployed, and all cached data relating to this object should
     * be dropped to prevent a class loader leak
     *
     * @param timedObjectId
     */
    void timerUndeployed(String timedObjectId);

    /**
     * Load a timer from persistent storage
     *
     * @param id
     * @param timedObjectId
     * @return
     */
    TimerEntity loadTimer(String id, String timedObjectId);

    /**
     * Load all active timers for the given entity bean with the given primary key
     *
     * @param timedObjectId The timed object id to load timers for
     * @param  primaryKey The primary key of the entity bean, or null for all timers
     * @return A list of all active timers
     */
    List<TimerEntity> loadActiveTimers(String timedObjectId, Object primaryKey);


    /**
     * Load all active timers for the given object. If the object is an entity bean timers for all beans will be returned.
     *
     * @param timedObjectId The timed object id to load timers for
     * @return A list of all active timers
     */
    List<TimerEntity> loadActiveTimers(String timedObjectId);

}
