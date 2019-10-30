/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Generic {@link Service} whose value is created and destroyed by contextual functions.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link FunctionalService}.
 */
@Deprecated
public class FunctionalValueService<T, V> implements Service<V> {
    private static final Logger LOGGER = Logger.getLogger(FunctionalValueService.class);

    private final Function<T, V> mapper;
    private final ExceptionFunction<StartContext, T, StartException> factory;
    private final BiConsumer<StopContext, T> destroyer;

    private volatile T value;

    /**
     * Constructs a new functional value service.
     * @param mapper a function that maps the result of the factory function to the service value
     * @param factory a function that creates a value
     * @param destroyer a consumer that destroys the value created by the factory function
     */
    public FunctionalValueService(Function<T, V> mapper, ExceptionFunction<StartContext, T, StartException> factory, BiConsumer<StopContext, T> destroyer) {
        this.mapper = mapper;
        this.factory = factory;
        this.destroyer = destroyer;
    }

    @Override
    public V getValue() {
        return this.mapper.apply(this.value);
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            this.value = this.factory.apply(context);
        } catch (RuntimeException | Error e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (this.destroyer != null) {
            try {
                this.destroyer.accept(context, this.value);
            } catch (RuntimeException | Error e) {
                LOGGER.warn(e.getLocalizedMessage(), e);
            } finally {
                this.value = null;
            }
        }
    }
}