/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Generic {@link org.jboss.as.controller.OperationStepHandler} for runtime metrics.
 * @author Paul Ferraro
 */
public class MetricHandler<C> extends ExecutionHandler<C, Metric<C>> implements Registration<ManagementResourceRegistration> {

    private final Collection<? extends Metric<C>> metrics;

    public <M extends Enum<M> & Metric<C>> MetricHandler(MetricExecutor<C> executor, Class<M> metricClass) {
        this(executor, EnumSet.allOf(metricClass));
    }

    public MetricHandler(MetricExecutor<C> executor, Metric<C>[] metrics) {
        this(executor, Arrays.asList(metrics));
    }

    public MetricHandler(MetricExecutor<C> executor, Collection<? extends Metric<C>> metrics) {
        super(executor, metrics, Metric::getName, Operations::getAttributeName);
        this.metrics = metrics;
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        for (Metric<C> metric : this.metrics) {
            registration.registerReadOnlyAttribute(metric.getDefinition(), this);
        }
    }
}
