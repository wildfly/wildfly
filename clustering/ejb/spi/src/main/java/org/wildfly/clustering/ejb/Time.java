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
package org.wildfly.clustering.ejb;

import java.util.concurrent.TimeUnit;

/**
 * Specifies a time duration.
 *
 * @author Paul Ferraro
 */
public class Time {
    private final long value;
    private final TimeUnit unit;

    public Time(long value, TimeUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    public long getValue() {
        return this.value;
    }

    public TimeUnit getUnit() {
        return this.unit;
    }

    public long convert(TimeUnit unit) {
        return unit.convert(this.value, this.unit);
    }

    @Override
    public boolean equals(Object object) {
        if ((object == null) || !(object instanceof Time)) return false;
        Time time = (Time) object;
        TimeUnit compareUnit = TimeUnit.MILLISECONDS;
        return compareUnit.convert(this.value, this.unit) == compareUnit.convert(time.value, time.unit);
    }

    @Override
    public int hashCode() {
        return Long.valueOf(TimeUnit.MILLISECONDS.convert(this.value, this.unit)).hashCode();
    }

    @Override
    public String toString() {
        return String.format("%d %s", this.value, this.unit);
    }
}
