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
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Generic {@link org.jboss.as.controller.OperationStepHandler} for runtime metrics.
 * @author Paul Ferraro
 */
public class MetricHandler<C> extends AbstractRuntimeOnlyHandler implements Registration<ManagementResourceRegistration> {

    private final Collection<? extends Metric<C>> metrics;
    private final Map<String, Metric<C>> executables = new HashMap<>();
    private final Executor<C, Metric<C>> executor;

    public <M extends Enum<M> & Metric<C>> MetricHandler(MetricExecutor<C> executor, Class<M> metricClass) {
        this(executor, EnumSet.allOf(metricClass));
    }

    public MetricHandler(MetricExecutor<C> executor, Metric<C>[] metrics) {
        this(executor, Arrays.asList(metrics));
    }

    public MetricHandler(MetricExecutor<C> executor, Collection<? extends Metric<C>> metrics) {
        this.executor = executor;
        for (Metric<C> executable : metrics) {
            this.executables.put(executable.getName(), executable);
        }
        this.metrics = metrics;
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        for (Metric<C> metric : this.metrics) {
            registration.registerReadOnlyAttribute(metric.getDefinition(), this);
        }
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) {
        String name = Operations.getAttributeName(operation);
        Metric<C> executable = this.executables.get(name);
        try {
            ModelNode result = this.executor.execute(context, executable);
            if (result != null) {
                context.getResult().set(result);
            }
        } catch (OperationFailedException e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        }
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }
}
