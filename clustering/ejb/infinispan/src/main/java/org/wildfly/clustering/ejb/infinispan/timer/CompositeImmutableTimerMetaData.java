/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.infinispan.timer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
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
    public Instant getLastTimout() {
        Duration lastTimeout = this.accessMetaData.getLastTimout();
        return (lastTimeout != null) ? this.creationMetaData.getStart().plus(lastTimeout) : null;
    }

    @Override
    public Instant getNextTimeout() {
        return this.creationMetaData.apply(this.getLastTimout());
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
