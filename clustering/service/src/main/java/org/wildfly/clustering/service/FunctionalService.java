/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.common.function.Functions;

/**
 * @author Paul Ferraro
 */
public class FunctionalService<T, V> implements Service {
    private static final Logger LOGGER = Logger.getLogger(FunctionalService.class);

    private final Consumer<V> consumer;
    private final Function<T, V> mapper;
    private final Supplier<T> factory;
    private final Consumer<T> destroyer;

    private volatile T value;

    public FunctionalService(Consumer<V> consumer, Function<T, V> mapper, Supplier<T> factory) {
        this(consumer, mapper, factory, Functions.discardingConsumer());
    }

    public FunctionalService(Consumer<V> consumer, Function<T, V> mapper, Supplier<T> factory, Consumer<T> destroyer) {
        this.consumer = consumer;
        this.mapper = mapper;
        this.factory = factory;
        this.destroyer = destroyer;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            this.value = this.factory.get();
            this.consumer.accept(this.mapper.apply(this.value));
        } catch (RuntimeException | Error e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        try {
            this.destroyer.accept(this.value);
        } catch (RuntimeException | Error e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        } finally {
            this.value = null;
            this.consumer.accept(null);
        }
    }
}
