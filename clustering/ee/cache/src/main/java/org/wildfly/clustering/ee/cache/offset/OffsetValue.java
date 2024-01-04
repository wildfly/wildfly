/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.offset;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.wildfly.common.function.Functions;

/**
 * Encapsulates a value that is offset from some basis, and updated via {@link OffsetValue#set(Object)}.
 * @author Paul Ferraro
 * @param <V> the value type
 */
public interface OffsetValue<V> extends Value<V> {

    /**
     * Returns the basis from which the associated offset will be applied.
     * @return the basis from which the associated offset will be applied.
     */
    V getBasis();

    /**
     * Returns the current value, computed by applying the current offset to the basis.
     * @return the computed value
     */
    @Override
    default V get() {
        return this.getOffset().apply(this.getBasis());
    }

    /**
     * The current offset.
     * @return an offset
     */
    Offset<V> getOffset();

    /**
     * Sets the current offset.
     * @param offset an offset
     */
    default void setOffset(Offset<V> offset) {
        this.set(offset.apply(this.getBasis()));
    }

    /**
     * Returns a new offset value based on the current value.
     * @return a new offset value
     */
    OffsetValue<V> rebase();

    static OffsetValue<Duration> from(Duration duration) {
        return new DurationOffsetValue(Functions.constantSupplier(duration));
    }

    static OffsetValue<Instant> from(Instant instant) {
        return new InstantOffsetValue(Functions.constantSupplier(instant));
    }

    class DefaultOffsetValue<O, V> extends AbstractValue<V> implements OffsetValue<V> {
        private final BiFunction<V, V, O> factory;
        private final Function<O, Offset<V>> offsetFactory;
        private final Function<Supplier<V>, OffsetValue<V>> offsetValueFactory;
        private final Supplier<V> basis;
        private final O zero;

        private volatile Offset<V> offset;

        DefaultOffsetValue(Supplier<V> basis, O zero, BiFunction<V, V, O> factory, Function<O, Offset<V>> offsetFactory, Function<Supplier<V>, OffsetValue<V>> offsetValueFactory) {
            this.factory = factory;
            this.offsetFactory = offsetFactory;
            this.offsetValueFactory = offsetValueFactory;
            this.zero = zero;
            this.basis = basis;
            this.offset = this.offsetFactory.apply(zero);
        }

        @Override
        public V getBasis() {
            return this.basis.get();
        }

        @Override
        public void set(V value) {
            V basis = this.getBasis();
            this.offset = this.offsetFactory.apply(Objects.equals(basis, value) ? this.zero : this.factory.apply(basis, value));
        }

        @Override
        public void setOffset(Offset<V> offset) {
            this.offset = offset;
        }

        @Override
        public Offset<V> getOffset() {
            return this.offset;
        }

        @Override
        public OffsetValue<V> rebase() {
            return this.offsetValueFactory.apply(this);
        }
    }

    static class TemporalOffsetValue<V> extends DefaultOffsetValue<Duration, V> {

        TemporalOffsetValue(Supplier<V> basis, BiFunction<V, V, Duration> factory, Function<Duration, Offset<V>> offsetFactory, Function<Supplier<V>, OffsetValue<V>> offsetValueFactory) {
            super(basis, Duration.ZERO, factory, offsetFactory, offsetValueFactory);
        }
    }

    static class DurationOffsetValue extends TemporalOffsetValue<Duration> {
        private static final BiFunction<Duration, Duration, Duration> MINUS = Duration::minus;
        private static final BiFunction<Duration, Duration, Duration> FACTORY = MINUS.andThen(Duration::negated);

        DurationOffsetValue(Supplier<Duration> basis) {
            super(basis, FACTORY, Offset::forDuration, DurationOffsetValue::new);
        }
    }

    static class InstantOffsetValue extends TemporalOffsetValue<Instant> {
        private static final BiFunction<Instant, Instant, Duration> FACTORY = Duration::between;

        InstantOffsetValue(Supplier<Instant> basis) {
            super(basis, FACTORY, Offset::forInstant, InstantOffsetValue::new);
        }
    }
}
