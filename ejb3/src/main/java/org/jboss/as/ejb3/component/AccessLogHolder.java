/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component;

import java.util.concurrent.atomic.AtomicLong;

import org.jboss.as.ejb3.subsystem.AccessLogService;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * MSC service that bridges the runtime lifecycle of {@link AccessLogService} to the
 * statically-registered {@link EjbAccessLogInterceptor}.
 *
 * <p>Installed <em>unconditionally</em> at subsystem boot (by {@code EJB3SubsystemAdd}),
 * so it is always present whether or not the {@code service=access-log} resource has been
 * added.  Deployments take a normal MSC dependency on it through
 * {@link #ACCESS_LOG_HOLDER_SERVICE_NAME}; the interceptor reads {@link #get()} on every
 * invocation — one volatile field read.
 *
 * <p>{@link AccessLogService#start}/{@link AccessLogService#stop} publish and clear the
 * service reference inside this holder; the holder service itself never stops.
 */
public final class AccessLogHolder implements org.jboss.msc.service.Service<AccessLogHolder> {

    /**
     * Well-known service name under which this holder is registered.
     * Deployment-time MSC dependencies are added against this name.
     */
    public static final ServiceName ACCESS_LOG_HOLDER_SERVICE_NAME =
            ServiceName.JBOSS.append("ejb3", "access-log", "holder");

    private volatile AccessLogService service;

    // Metrics — live for the lifetime of this holder (server boot until server shutdown or resource removal).
    private final AtomicLong eventsLogged = new AtomicLong();
    private final AtomicLong eventsDropped = new AtomicLong();

    public AccessLogHolder() {
    }

    // ---- Service<AccessLogHolder> ----

    @Override
    public void start(final StartContext context) {
        // Nothing to do — the holder is ready as soon as it is constructed.
    }

    @Override
    public void stop(final StopContext context) {
        // Nothing to do — the holder is never stopped independently of the server.
    }

    @Override
    public AccessLogHolder getValue() {
        return this;
    }

    // ---- Accessor used by EjbAccessLogInterceptor (hot path) ----

    /**
     * Returns the live {@link AccessLogService}, or {@code null} when the access-log resource
     * is not present.  This is the hot-path read; it is a single volatile field read.
     */
    public AccessLogService get() {
        return service;
    }

    // ---- Mutators called by AccessLogService.start/stop ----

    /**
     * Called by {@link AccessLogService#start} to publish the running service.
     */
    public void set(final AccessLogService service) {
        this.service = service;
    }

    /**
     * Called by {@link AccessLogService#stop} to clear the running service.
     */
    public void clear() {
        this.service = null;
    }

    // ---- Metric Accessors and Mutators ----

    public long getEventsLogged() {
        return eventsLogged.get();
    }

    public long getEventsDropped() {
        return eventsDropped.get();
    }

    public void incrementEventsLogged() {
        eventsLogged.incrementAndGet();
    }

    public void incrementEventsDropped() {
        eventsDropped.incrementAndGet();
    }

    public void addEventsDropped(final long count) {
        if (count > 0L) {
            eventsDropped.addAndGet(count);
        }
    }

    /**
     * Resets metrics to zero. Called when the access-log resource is removed.
     */
    public void resetCounters() {
        eventsLogged.set(0L);
        eventsDropped.set(0L);
    }
}
