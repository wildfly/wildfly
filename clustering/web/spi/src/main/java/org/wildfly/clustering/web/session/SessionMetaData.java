/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.session;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Abstraction for meta information about a web session.
 * @author Paul Ferraro
 */
public interface SessionMetaData extends ImmutableSessionMetaData {
    /**
     * Set the time this session was last accessed.
     * @param the time this session was last accessed
     */
    void setLastAccessedTime(Date date);

    /**
     * Set the time interval, using the specified unit, after which this session will expire.
     * @param interval a time interval
     * @param unit a time unit
     */
    void setMaxInactiveInterval(long interval, TimeUnit unit);
}
