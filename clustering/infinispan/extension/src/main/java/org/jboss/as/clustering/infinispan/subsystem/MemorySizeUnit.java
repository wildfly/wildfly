/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
