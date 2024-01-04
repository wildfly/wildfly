/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.offset;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Encapsulates an offset that can be applied to a value.
 * @author Paul Ferraro
 * @param <V> the value to which the offset can be applied
 */
public interface Offset<V> extends UnaryOperator<V> {
    /**
     * Returns true if this offset is zero, false otherwise.
     * @return true if this offset is zero, false otherwise.
     */
    boolean isZero();

    static Offset<Duration> forDuration(Duration offset) {
        return offset.isZero() ? DurationOffset.ZERO : new DurationOffset(offset);
    }

    static Offset<Instant> forInstant(Duration offset) {
        return offset.isZero() ? InstantOffset.ZERO : new InstantOffset(offset);
    }

    class DefaultOffset<O, V> implements Offset<V>, Supplier<O> {

        private final O value;
        private final Predicate<O> isZero;
        private final BiFunction<V, O, V> applicator;

        DefaultOffset(O value, Predicate<O> isZero, BiFunction<V, O, V> applicator) {
            this.value = value;
            this.isZero = isZero;
            this.applicator = applicator;
        }

        @Override
        public V apply(V value) {
            return this.isZero() ? value : this.applicator.apply(value, this.value);
        }

        @Override
        public boolean isZero() {
            return this.isZero.test(this.value);
        }

        @Override
        public O get() {
            return this.value;
        }

        @Override
        public int hashCode() {
            return this.value.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Offset)) return false;
            @SuppressWarnings("unchecked")
            DefaultOffset<O, V> offset = (DefaultOffset<O, V>) object;
            return this.value.equals(offset.value);
        }

        @Override
        public String toString() {
            return this.value.toString();
        }
    }

    class TemporalOffset<V> extends DefaultOffset<Duration, V> {
        private static final Predicate<Duration> IS_ZERO = Duration::isZero;

        TemporalOffset(Duration offset, BiFunction<V, Duration, V> applicator) {
            super(offset, IS_ZERO, applicator);
        }
    }

    class DurationOffset extends TemporalOffset<Duration> {
        static final Offset<Duration> ZERO = new DurationOffset(Duration.ZERO);
        private static final BiFunction<Duration, Duration, Duration> DURATION_APPLICATOR = Duration::plus;

        DurationOffset(Duration value) {
            super(value, DURATION_APPLICATOR);
        }
    }

    class InstantOffset extends TemporalOffset<Instant> {
        static final Offset<Instant> ZERO = new InstantOffset(Duration.ZERO);
        private static final BiFunction<Instant, Duration, Instant> INSTANT_APPLICATOR = Instant::plus;

        InstantOffset(Duration value) {
            super(value, INSTANT_APPLICATOR);
        }
    }
}
