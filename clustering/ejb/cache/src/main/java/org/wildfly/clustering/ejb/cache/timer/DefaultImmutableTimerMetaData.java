/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Predicate;

import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;
import org.wildfly.clustering.ejb.timer.TimerConfiguration;
import org.wildfly.clustering.ejb.timer.TimerType;
import org.wildfly.clustering.marshalling.Marshaller;

/**
 * The default implementation of the immutable view of a timer metadata.
 * @author Paul Ferraro
 * @param <C> the timer context type
 */
public class DefaultImmutableTimerMetaData<C> implements ImmutableTimerMetaData {

    private final ImmutableTimerMetaDataEntry<C> entry;
    private final boolean persistent;
    private final Marshaller<Object, C> marshaller;

    public DefaultImmutableTimerMetaData(TimerMetaDataConfiguration<C> configuration, ImmutableTimerMetaDataEntry<C> entry) {
        this.marshaller = configuration.getMarshaller();
        this.persistent = configuration.isPersistent();
        this.entry = entry;
    }

    @Override
    public TimerType getType() {
        return this.entry.getType();
    }

    @Override
    public Object getContext() {
        try {
            return this.marshaller.read(this.entry.getContext());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public boolean isPersistent() {
        return this.persistent;
    }

    @Override
    public Optional<Instant> getLastTimeout() {
        return Optional.ofNullable(this.entry.getLastTimeout()).map(this.entry.getStart()::plus);
    }

    @Override
    public Optional<Instant> getNextTimeout() {
        Optional<Instant> lastTimeout = this.getLastTimeout();
        return lastTimeout.isPresent() ? lastTimeout.map(this.entry) : Optional.of(this.entry.getStart());
    }

    @Override
    public <TC extends TimerConfiguration> TC getConfiguration(Class<TC> configurationClass) {
        return configurationClass.cast(this.entry);
    }

    @Override
    public Predicate<Method> getTimeoutMatcher() {
        return this.entry.getTimeoutMatcher();
    }
}
