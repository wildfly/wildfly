/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Predicate;

import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;
import org.wildfly.clustering.ejb.timer.TimerConfiguration;
import org.wildfly.clustering.ejb.timer.TimerType;
import org.wildfly.clustering.marshalling.spi.Marshaller;

/**
 * @author Paul Ferraro
 */
public class CompositeImmutableTimerMetaData<V> implements ImmutableTimerMetaData {

    private final TimerCreationMetaData<V> creationMetaData;
    private final TimerAccessMetaData accessMetaData;
    private final Marshaller<Object, V> marshaller;
    private final boolean persistent;

    public CompositeImmutableTimerMetaData(TimerMetaDataConfiguration<V> configuration, TimerCreationMetaData<V> creationMetaData, TimerAccessMetaData accessMetaData) {
        this.marshaller = configuration.getMarshaller();
        this.creationMetaData = creationMetaData;
        this.accessMetaData = accessMetaData;
        this.persistent = configuration.isPersistent();
    }

    @Override
    public TimerType getType() {
        return this.creationMetaData.getType();
    }

    @Override
    public Object getContext() {
        try {
            return this.marshaller.read(this.creationMetaData.getContext());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public boolean isPersistent() {
        return this.persistent;
    }

    @Override
    public Optional<Instant> getLastTimout() {
        Duration lastTimeout = this.accessMetaData.getLastTimout();
        return (lastTimeout != null) ? Optional.of(this.creationMetaData.getStart().plus(lastTimeout)) : Optional.empty();
    }

    @Override
    public Optional<Instant> getNextTimeout() {
        return Optional.ofNullable(this.creationMetaData.apply(this.getLastTimout().orElse(null)));
    }

    @Override
    public <TC extends TimerConfiguration> TC getConfiguration(Class<TC> configurationClass) {
        return configurationClass.cast(this.creationMetaData);
    }

    @Override
    public Predicate<Method> getTimeoutMatcher() {
        return this.creationMetaData.getTimeoutMatcher();
    }
}
