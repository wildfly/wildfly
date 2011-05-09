/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.web.session;

import java.util.Map;
import java.util.WeakHashMap;

import org.apache.catalina.Manager;

/**
 * Tracks sessions invalidated by the current thread so that JBossCacheManager can avoid trying to retrieve them from the
 * distributed cache if the thread asks it to {@link DistributableSessionManager#findSession(String) find} them again.
 * <p>
 * Needed for cases where code that executes after {@link ClusteredSessionValve} has returned asks the request for the session
 * again. With an invalidated session this results in a <code>findSession</code> call to JBossCacheManager, which results in a
 * read of the distributed cache. With buddy replication, that will lead to a data gravitation attempt, which is at a minimum
 * expensive and with asynchronous replication may result pulling a stale version of the session back into the cache.
 * </p>
 *
 * @author Brian Stansberry
 */
public class SessionInvalidationTracker {
    private static final ThreadLocal<Map<Manager, String>> invalidatedSessions = new ThreadLocal<Map<Manager, String>>();
    private static final ThreadLocal<Boolean> suspended = new ThreadLocal<Boolean>();

    public static void suspend() {
        suspended.set(Boolean.TRUE);
    }

    public static void resume() {
        suspended.set(null);
    }

    public static void sessionInvalidated(String id, Manager manager) {
        if (Boolean.TRUE != suspended.get()) {
            Map<Manager, String> map = invalidatedSessions.get();
            if (map == null) {
                map = new WeakHashMap<Manager, String>(2);
                invalidatedSessions.set(map);
            }
            map.put(manager, id);
        }
    }

    public static void clearInvalidatedSession(String id, Manager manager) {
        Map<Manager, String> map = invalidatedSessions.get();
        if (map != null) {
            map.remove(manager);
        }
    }

    public static boolean isSessionInvalidated(String id, Manager manager) {
        boolean result = false;
        Map<Manager, String> map = invalidatedSessions.get();
        if (map != null) {
            result = id.equals(map.get(manager));
        }
        return result;
    }
}
