/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import java.math.BigInteger;
import java.util.function.LongUnaryOperator;

/**
 * Units for the size attribute of the memory resource.
 * @author Paul Ferraro
 */
public enum MemorySizeUnit implements LongUnaryOperator {
    ENTRIES(0),
    BYTES(1),
    KB(1000, 1),
    KiB(1024, 1),
    MB(1000, 2),
    MiB(1024, 2),
    GB(1000, 3),
    GiB(1024, 3),
    TB(1000, 4),
    TiB(1024, 4),
    ;
    private final long value;

    MemorySizeUnit(int base, int exponent) {
        this(BigInteger.valueOf(base).pow(exponent).longValueExact());
    }

    MemorySizeUnit(long value) {
        this.value = value;
    }

    @Override
    public long applyAsLong(long size) {
        return size * this.value;
    }
}
