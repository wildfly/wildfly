/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;
import org.wildfly.clustering.ejb.timer.ScheduleTimerConfiguration;

/**
 * An schedule-based timer metadata cache entry.
 * @author Paul Ferraro
 * @param <C> the timer context type
 */
public class ScheduleTimerMetaDataEntry<C> extends AbstractTimerMetaDataEntry<C> implements ImmutableScheduleTimerMetaDataEntry<C> {

    private final ImmutableScheduleExpression expression;
    private final Predicate<Method> timeoutMatcher;

    private volatile UnaryOperator<Instant> operator = null;

    public ScheduleTimerMetaDataEntry(C context, ScheduleTimerConfiguration config) {
        this(context, config, DefaultTimeoutMatcher.INSTANCE);
    }

    public ScheduleTimerMetaDataEntry(C context, ScheduleTimerConfiguration config, Method method) {
        this(context, config, new TimeoutDescriptor(method));
    }

    private ScheduleTimerMetaDataEntry(C context, ScheduleTimerConfiguration config, Predicate<Method> timeoutMatcher) {
        // Create operator eagerly when timer entry is created
        this(context, config, timeoutMatcher, ScheduleTimerOperatorFactory.INSTANCE.createOperator(config.getScheduleExpression()));
    }

    private ScheduleTimerMetaDataEntry(C context, ScheduleTimerConfiguration config, Predicate<Method> timeoutMatcher, UnaryOperator<Instant> operator) {
        // Use operator to calculate start time
        this(context, operator.apply(null), config.getScheduleExpression(), timeoutMatcher, operator);
    }

    ScheduleTimerMetaDataEntry(C context, Instant start, ImmutableScheduleExpression expression, Predicate<Method> timeoutMatcher) {
        // Create operator lazily
        this(context, start, expression, timeoutMatcher, null);
    }

    private ScheduleTimerMetaDataEntry(C context, Instant start, ImmutableScheduleExpression expression, Predicate<Method> timeoutMatcher, UnaryOperator<Instant> operator) {
        super(context, start);
        this.expression = expression;
        this.timeoutMatcher = timeoutMatcher;
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
    public Predicate<Method> getTimeoutMatcher() {
        return this.timeoutMatcher;
    }

    @Override
    public Instant apply(Instant lastTimeout) {
        return this.getOperator().apply(lastTimeout);
    }

    @Override
    protected RemappableTimerMetaDataEntry<C> clone() {
        return new ScheduleTimerMetaDataEntry<>(this.getContext(), this.getStart(), this.expression, this.timeoutMatcher, this.operator);
    }
}
