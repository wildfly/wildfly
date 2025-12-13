/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering;

import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Static class for tracking and recording passivation/activation events.
 * Provides a static event queue that can be polled without triggering activation of the tracked bean.
 * Typically accessed via HTTP HEAD method or PassivationEventTracker interface.
 * Event queue must be cleared during test initialization.
 * This class cannot be reasonably parametrized as the core functionality relies on a static field accessible outside the scope of the request/invocation.
 *
 * @author Radoslav Husar
 */
public final class PassivationEventTrackerUtil {

    private PassivationEventTrackerUtil() {
        // Disallow instantiation - access only through static methods
    }

    /**
     * Event types for passivation/activation tracking.
     */
    public enum EventType {
        PASSIVATION,
        ACTIVATION
    }

    /**
     * Static queue to collect passivation/activation events.
     * This allows test code to check for events without triggering activation.
     * Uses Object for the key type since the queue is shared across all subclasses.
     */
    public static final BlockingQueue<Map.Entry<Object, EventType>> EVENTS = new LinkedBlockingQueue<>();

    /**
     * Records a passivation event for the given identifier.
     *
     * @param identifier the identifier for the object being passivated
     */
    public static void recordPassivation(Object identifier) {
        EVENTS.add(new AbstractMap.SimpleImmutableEntry<>(identifier, EventType.PASSIVATION));
    }


    /**
     * Records an activation event for the given identifier.
     *
     * @param identifier the identifier for the object being activated
     */
    public static void recordActivation(Object identifier) {
        EVENTS.add(new AbstractMap.SimpleImmutableEntry<>(identifier, EventType.ACTIVATION));
    }

    /**
     * Clears all passivation/activation events from the queue.
     * Useful for test setup to ensure a clean state.
     */
    public static void clearEvents() {
        EVENTS.clear();
    }

    /**
     * Polls a single event from the event queue.
     *
     * @return the next event, or null if no events are available
     */
    public static Map.Entry<Object, EventType> pollEvent() {
        return EVENTS.poll();
    }

    /**
     * Drains all events from the queue and applies the given consumer to each event.
     * This is useful for processing all accumulated events at once.
     *
     * @param consumer the consumer to apply to each event in the queue
     */
    public static void drainEvents(Consumer<Map.Entry<Object, EventType>> consumer) {
        List<Map.Entry<Object, EventType>> events = new LinkedList<>();
        int count = EVENTS.drainTo(events);
        if (count > 0) {
            events.forEach(consumer);
        }
    }

}
