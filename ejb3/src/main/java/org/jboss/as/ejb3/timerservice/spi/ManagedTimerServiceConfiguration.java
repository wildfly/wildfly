/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.spi;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import jakarta.ejb.TimerConfig;

import org.jboss.msc.service.ServiceName;

/**
 * Encapsulates the common configuration of a managed timer service.
 * @author Paul Ferraro
 */
public interface ManagedTimerServiceConfiguration extends TimerServiceApplicableComponentConfiguration {

    enum TimerFilter implements Predicate<TimerConfig>, UnaryOperator<ServiceName> {
        ALL() {
            @Override
            public boolean test(TimerConfig config) {
                return true;
            }

            @Override
            public ServiceName apply(ServiceName name) {
                return name;
            }
        },
        TRANSIENT() {
            @Override
            public boolean test(TimerConfig config) {
                return !config.isPersistent();
            }

            @Override
            public ServiceName apply(ServiceName name) {
                return name.append("transient");
            }
        },
        PERSISTENT() {
            @Override
            public boolean test(TimerConfig config) {
                return config.isPersistent();
            }

            @Override
            public ServiceName apply(ServiceName name) {
                return name.append("persistent");
            }
        },
        ;

        @Override
        public ServiceName apply(ServiceName name) {
            return name.append(this.name());
        }
    }

    /**
     * An invoker for the EJB component associated with this timer service
     * @return an invoker
     */
    TimedObjectInvoker getInvoker();

    /**
     * Returns a filter to determine whether or not to create a given timer.
     * @return a filter that returns true, if the given timer should be created, false otherwise.
     */
    Predicate<TimerConfig> getTimerFilter();
}
