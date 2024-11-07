/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.security.PrivilegedAction;
import java.time.Instant;
import java.util.ServiceLoader;
import java.util.function.UnaryOperator;

import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;
import org.wildfly.clustering.ejb.timer.ScheduleTimerOperationProvider;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public enum ScheduleTimerOperatorFactory implements ScheduleTimerOperationProvider {
    INSTANCE;

    private final ScheduleTimerOperationProvider provider;

    ScheduleTimerOperatorFactory() {
        this.provider = load();
    }

    private static ScheduleTimerOperationProvider load() {
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
            @Override
            public ScheduleTimerOperationProvider run() {
                return ServiceLoader.load(ScheduleTimerOperationProvider.class, ScheduleTimerOperationProvider.class.getClassLoader()).findFirst().orElseThrow(IllegalStateException::new);
            }
        });
    }

    @Override
    public UnaryOperator<Instant> createOperator(ImmutableScheduleExpression expression) {
        return this.provider.createOperator(expression);
    }
}
