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
