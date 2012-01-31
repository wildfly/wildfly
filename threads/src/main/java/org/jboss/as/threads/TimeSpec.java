/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.threads;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * A specification of a simple duration.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class TimeSpec implements Serializable {

    private static final long serialVersionUID = 5145007669106852119L;

    public static final TimeSpec DEFAULT_KEEPALIVE = new TimeSpec(TimeUnit.SECONDS, 30L);

    private final TimeUnit unit;
    private final long duration;

    /**
     * Construct a new instance.
     *
     * @param unit the unit of time
     * @param duration the quantity of units
     */
    public TimeSpec(final TimeUnit unit, final long duration) {
        if (unit == null) {
            throw ThreadsMessages.MESSAGES.nullUnit();
        }
        this.unit = unit;
        this.duration = duration;
    }

    /**
     * Get the time unit.
     *
     * @return the time unit
     */
    public TimeUnit getUnit() {
        return unit;
    }

    /**
     * Get the duration.
     *
     * @return the duration
     */
    public long getDuration() {
        return duration;
    }

    public long elementHash() {
        return Long.rotateLeft(duration, 3) ^ unit.hashCode() & 0xFFFFFFFFL;
    }

    public boolean equals(Object obj) {
        return obj instanceof TimeSpec && equals((TimeSpec) obj);
    }

    public boolean equals(TimeSpec obj) {
        return obj != null && unit == obj.unit && duration == obj.duration;
    }

    public int hashCode() {
        return (int) elementHash();
    }
}
