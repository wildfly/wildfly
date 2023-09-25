/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.function.UnaryOperator;

import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;
import org.wildfly.clustering.ejb.timer.ScheduleTimerConfiguration;

/**
 * Cache value for scheduled-based timer creation meta data.
 * @author Paul Ferraro
 */
public class ScheduleTimerCreationMetaDataEntry<V> extends AbstractTimerCreationMetaDataEntry<V> implements ScheduleTimerCreationMetaData<V> {

    private final ImmutableScheduleExpression expression;
    private final TimeoutDescriptor descriptor;

    private volatile UnaryOperator<Instant> operator = null;

    public ScheduleTimerCreationMetaDataEntry(V context, ScheduleTimerConfiguration config, Method method) {
        // Create operator eagerly when timer entry is created
        this(context, config, method != null ? new TimeoutDescriptor(method) : null, ScheduleTimerOperatorFactory.INSTANCE.createOperator(config.getScheduleExpression()));
    }

    private ScheduleTimerCreationMetaDataEntry(V context, ScheduleTimerConfiguration config, TimeoutDescriptor descriptor, UnaryOperator<Instant> operator) {
        // Use operator to calculate start time
        this(context, operator.apply(null), config.getScheduleExpression(), descriptor, operator);
    }

    public ScheduleTimerCreationMetaDataEntry(V context, Instant start, ImmutableScheduleExpression expression, TimeoutDescriptor descriptor) {
        // Create operator lazily
        this(context, start, expression, descriptor, null);
    }

    private ScheduleTimerCreationMetaDataEntry(V context, Instant start, ImmutableScheduleExpression expression, TimeoutDescriptor descriptor, UnaryOperator<Instant> operator) {
        super(context, start);
        this.expression = expression;
        this.descriptor = descriptor;
        this.operator = operator;
    }

    /* Get operator, lazily creating if necessary */
    private UnaryOperator<Instant> getOperator() {
        if (this.operator == null) {
            synchronized (this) {
                if (this.operator == null) {
                    this.operator = ScheduleTimerOperatorFactory.INSTANCE.createOperator(this.expression);
                }
            }
        }
        return this.operator;
    }

    @Override
    public ImmutableScheduleExpression getScheduleExpression() {
        return this.expression;
    }

    @Override
    public TimeoutDescriptor getTimeoutMatcher() {
        return this.descriptor;
    }

    @Override
    public Instant apply(Instant lastTimeout) {
        return (lastTimeout != null) ? this.getOperator().apply(lastTimeout) : this.getStart();
    }
}
