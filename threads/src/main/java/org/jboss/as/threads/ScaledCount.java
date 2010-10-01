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
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ScaledCount implements Serializable {
    private static final long serialVersionUID = 50591622989801087L;

    private final BigDecimal count;
    private final BigDecimal perCpu;

    public ScaledCount(final BigDecimal count, final BigDecimal perCpu) {
        if (count.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Count must be greater than or equal to zero");
        }
        if (perCpu.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Per-cpu must be greater than or equal to zero");
        }
        this.count = count;
        this.perCpu = perCpu;
    }

    public BigDecimal getCount() {
        return count;
    }

    public BigDecimal getPerCpu() {
        return perCpu;
    }

    public int getScaledCount() {
        return count.add(perCpu.multiply(BigDecimal.valueOf((long)Runtime.getRuntime().availableProcessors()), MathContext.DECIMAL64), MathContext.DECIMAL64).round(MathContext.DECIMAL64).intValueExact();
    }

    public boolean equals(final Object obj) {
        return obj instanceof ScaledCount && equals((ScaledCount) obj);
    }

    public boolean equals(final ScaledCount obj) {
        return obj != null && obj.count.equals(count) && obj.perCpu.equals(perCpu);
    }

    public int hashCode() {
        return count.hashCode() * 31 + perCpu.hashCode();
    }
}
