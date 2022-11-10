/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
