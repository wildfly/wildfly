/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.function.Function;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.common.function.ExceptionFunction;

/**
 * A functional view of a runtime metric.
 * @author Paul Ferraro
 * @param <T> the type of value provided by the service on which the given runtime metric operates
 * @param <V> the type of the value of which the given runtime metric operates
 */
public class MetricFunction<T, V> implements ExceptionFunction<T, ModelNode, OperationFailedException> {

    private final Function<T, V> mapper;
    private final Metric<V> metric;

    /**
     * Creates a functional view of the specified metric using the specified mapper.
     * @param mapper maps the value of a service to the value on which the given metric operates
     * @param metric a runtime metric
     */
    public MetricFunction(Function<T, V> mapper, Metric<V> metric) {
        this.mapper = mapper;
        this.metric = metric;
    }

    @Override
    public ModelNode apply(T value) throws OperationFailedException {
        V mapped = this.mapper.apply(value);
        return (mapped != null) ? this.metric.execute(mapped) : null;
    }
}
