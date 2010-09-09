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

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ScaledCount implements Serializable{
    private static final long serialVersionUID = 50591622989801087L;

    private final long count;
    private final long perCpu;

    public ScaledCount(final long count, final long perCpu) {
        this.count = count;
        this.perCpu = perCpu;
    }

    public long getCount() {
        return count;
    }

    public long getPerCpu() {
        return perCpu;
    }

    public int getScaledCount() {
        return (int)(count + (perCpu * Runtime.getRuntime().availableProcessors()));
    }

    public long elementHash() {
        return Long.rotateLeft(perCpu, 1) ^ count;
    }

    public boolean equals(final Object obj) {
        return obj instanceof ScaledCount && equals((ScaledCount) obj);
    }

    public boolean equals(final ScaledCount obj) {
        return obj != null && obj.count == count && obj.perCpu == perCpu;
    }

    public int hashCode() {
        return (int) elementHash();
    }
}
