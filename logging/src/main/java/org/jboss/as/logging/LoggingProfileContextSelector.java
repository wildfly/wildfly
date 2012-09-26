/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import java.util.concurrent.ConcurrentMap;

import org.jboss.logmanager.LogContext;
import org.jboss.util.collection.ConcurrentSkipListMap;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
final class LoggingProfileContextSelector {
    private static final LoggingProfileContextSelector INSTANCE = new LoggingProfileContextSelector();

    private final ConcurrentMap<String, LogContext> profileContexts = new ConcurrentSkipListMap<String, LogContext>();

    private LoggingProfileContextSelector() {

    }

    public static LoggingProfileContextSelector getInstance() {
        return INSTANCE;
    }

    /**
     * Get or create the log context based on the logging profile.
     *
     * @param loggingProfile the logging profile to get or create the log context for
     *
     * @return the log context that was found or a new log context
     */
    public LogContext getOrCreate(final String loggingProfile) {
        LogContext result = profileContexts.get(loggingProfile);
        if (result == null) {
            result = LogContext.create();
            final LogContext current = profileContexts.putIfAbsent(loggingProfile, result);
            if (current != null) {
                result = current;
            }
        }
        return result;
    }

    /**
     * Returns the log context associated with the logging profile or {@code null} if the logging profile does not have
     * an associated log context.
     *
     * @param loggingProfile the logging profile associated with the log context
     *
     * @return the log context or {@code null} if the logging profile is not associated with a log context
     */
    public LogContext get(final String loggingProfile) {
        return profileContexts.get(loggingProfile);
    }

    /**
     * Checks to see if the logging profile has a log context associated with it.
     *
     * @param loggingProfile the logging profile to check
     *
     * @return {@code true} if the logging profile has an associated log context, otherwise {@code false}
     */
    public boolean exists(final String loggingProfile) {
        return profileContexts.containsKey(loggingProfile);
    }

    /**
     * Removes the associated log context from the logging profile.
     *
     * @param loggingProfile the logging profile associated with the log context
     *
     * @return the log context that was removed or {@code null} if there was no log context associated
     */
    public LogContext remove(final String loggingProfile) {
        return profileContexts.remove(loggingProfile);
    }
}
