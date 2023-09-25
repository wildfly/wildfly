/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;

import org.jboss.as.ejb3.timerservice.spi.TimerServiceRegistry;

/**
 * A registry to which individual {@link jakarta.ejb.TimerService timer services} can register to (and un-register from). The main purpose
 * of this registry is to provide an implementation of {@link #getAllActiveTimers()} which returns all
 * {@link jakarta.ejb.TimerService#getTimers() active timers} after querying each of the {@link jakarta.ejb.TimerService timer services} registered
 * with this {@link TimerServiceRegistry registry}.
 * <p/>
 * Typical use of this registry is to maintain one instance of this registry, per deployment unit (also known as Jakarta Enterprise Beans module) and register the timer
 * services of all Jakarta Enterprise Beans components that belong to that deployment unit. Effectively, such an instance can then be used to fetch all active timers
 * that are applicable to that deployment unit (a.k.a Jakarta Enterprise Beans module).
 *
 * @author Jaikiran Pai
 */
public class TimerServiceRegistryImpl implements TimerServiceRegistry {

    private static final Function<TimerService, Collection<Timer>> GET_TIMERS = TimerService::getTimers;
    private static final Function<Collection<Timer>, Stream<Timer>> STREAM = Collection::stream;

    private final Set<TimerService> services = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

    @Override
    public void registerTimerService(TimerService service) {
        this.services.add(service);
    }

    @Override
    public void unregisterTimerService(TimerService service) {
        this.services.remove(service);
    }

    @Override
    public Collection<Timer> getAllTimers() {
        synchronized (this.services) {
            return Collections.unmodifiableCollection(this.services.stream().map(GET_TIMERS).flatMap(STREAM).collect(Collectors.toList()));
        }
    }
}
