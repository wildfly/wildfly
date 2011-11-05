/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.timerservice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TimerServiceRegistry
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class TimerServiceRegistry {

    private static final Map<String, TimerServiceImpl> timerServices = new ConcurrentHashMap<String, TimerServiceImpl>();

    public static void registerTimerService(TimerServiceImpl timerservice) {
        if (timerservice == null) {
            throw new IllegalArgumentException("null timerservice cannot be registered");
        }
        // get hold of the timed object id
        String timedObjectId = timerservice.getInvoker().getTimedObjectId();
        if (timerServices.containsKey(timedObjectId)) {
            throw new IllegalStateException("Timer service with timedObjectId: " + timedObjectId
                    + " is already registered");
        }
        // add to the registry
        timerServices.put(timedObjectId, timerservice);
    }

    public static TimerServiceImpl getTimerService(String timedObjectId) {
        return timerServices.get(timedObjectId);
    }

    public static boolean isRegistered(String timedObjectId) {
        return timerServices.containsKey(timedObjectId);
    }

    public static void unregisterTimerService(String timedObjectId) {
        if (timedObjectId == null) {
            throw new IllegalArgumentException("null timedObjectId cannot be used for unregistering timerservice");
        }
        if (isRegistered(timedObjectId) == false) {
            throw new IllegalArgumentException("Cannot unregister timer service with timedObjectId: " + timedObjectId
                    + " because it's not registered");
        }
        timerServices.remove(timedObjectId);

    }
}
