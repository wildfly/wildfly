/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.service;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

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
        this(consumer, mapper, factory, new Consumer<T>() {
            @Override
            public void accept(T value) {
                // Do nothing
            }
        });
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
        }
    }
}
