/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.metrics;

import java.util.HashSet;
import java.util.Set;

import io.micrometer.core.instrument.Meter;

/** Tracks model-metric read failures during one caller-scoped Prometheus scrape. */
public final class MetricScrapeContext implements AutoCloseable {
    private static final ThreadLocal<MetricScrapeContext> CURRENT = new ThreadLocal<>();

    private final MetricScrapeContext previous;
    private final Set<Meter.Id> deniedMeters = new HashSet<>();

    /** Saves the previous context and activates this one on the current request thread. */
    private MetricScrapeContext() {
        previous = CURRENT.get();
        CURRENT.set(this);
    }

    /**
     * Starts collecting model-metric access failures for the current scrape.
     *
     * @return the scrape context that must be closed after serialization
     */
    public static MetricScrapeContext begin() {
        return new MetricScrapeContext();
    }

    /**
     * Marks a meter as unreadable in the active scrape, if one exists.
     *
     * @param meterId the meter whose read failed
     */
    public static void deny(Meter.Id meterId) {
        MetricScrapeContext context = CURRENT.get();
        if (context != null && meterId != null) {
            context.deniedMeters.add(meterId);
        }
    }

    /**
     * Returns the meters denied during this scrape.
     *
     * @return an immutable snapshot of denied meter identities
     */
    public Set<Meter.Id> deniedMeters() {
        return Set.copyOf(deniedMeters);
    }

    /** Restores the prior scrape context when this scrape finishes. */
    @Override
    public void close() {
        if (CURRENT.get() == this) {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
