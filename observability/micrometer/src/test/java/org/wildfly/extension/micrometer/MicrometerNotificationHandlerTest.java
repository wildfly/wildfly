/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.notification.Notification;
import org.jboss.as.controller.notification.NotificationFilter;
import org.jboss.as.controller.notification.NotificationHandler;
import org.jboss.as.controller.notification.NotificationHandlerRegistry;
import org.junit.Test;

/** Verifies notification registration, filtering, idempotency, and dispatch. */
public class MicrometerNotificationHandlerTest {
    /** Verifies that unregistering uses the exact registration supplied at start. */
    @Test
    public void registersAndUnregistersWithTheSameRegistration() {
        RecordingRegistry registry = new RecordingRegistry();
        MicrometerNotificationHandler handler = new MicrometerNotificationHandler(registry, notification -> { }, notification -> { });

        handler.start();
        handler.stop();

        assertEquals("Handler should register once", 1, registry.registered.size());
        assertEquals("Unregistration should match registration", registry.registered.get(0), registry.unregistered.get(0));
        assertEquals("Handler should listen at any address", NotificationHandlerRegistry.ANY_ADDRESS, registry.registered.get(0).address());
    }

    /** Verifies repeated start and stop calls do not duplicate registry operations. */
    @Test
    public void startAndStopAreIdempotent() {
        RecordingRegistry registry = new RecordingRegistry();
        MicrometerNotificationHandler handler = new MicrometerNotificationHandler(registry, notification -> { }, notification -> { });

        handler.start();
        handler.start();
        handler.stop();
        handler.stop();

        assertEquals("Repeated start should register once", 1, registry.registered.size());
        assertEquals("Repeated stop should unregister once", 1, registry.unregistered.size());
    }

    /** Verifies that only resource lifecycle notifications pass the filter. */
    @Test
    public void filtersResourceLifecycleNotifications() {
        RecordingRegistry registry = new RecordingRegistry();
        MicrometerNotificationHandler handler = new MicrometerNotificationHandler(registry, notification -> { }, notification -> { });

        handler.start();
        NotificationFilter filter = registry.registered.get(0).filter();

        assertTrue("Added notification should pass", filter.isNotificationEnabled(new Notification("resource-added", PathAddress.EMPTY_ADDRESS, "added")));
        assertTrue("Removed notification should pass", filter.isNotificationEnabled(new Notification("resource-removed", PathAddress.EMPTY_ADDRESS, "removed")));
        assertTrue("Attribute notification should be filtered", !filter.isNotificationEnabled(new Notification("attribute-value-written", PathAddress.EMPTY_ADDRESS, "written")));
    }

    /** Verifies that added and removed notifications reach their callbacks. */
    @Test
    public void dispatchesResourceLifecycleNotifications() {
        RecordingRegistry registry = new RecordingRegistry();
        AtomicReference<PathAddress> added = new AtomicReference<>();
        AtomicReference<PathAddress> removed = new AtomicReference<>();
        MicrometerNotificationHandler handler = new MicrometerNotificationHandler(registry, added::set, removed::set);

        handler.handleNotification(new Notification("resource-added", PathAddress.pathAddress("subsystem", "test"), "added"));
        handler.handleNotification(new Notification("resource-removed", PathAddress.pathAddress("subsystem", "test"), "removed"));

        assertEquals("Added address should be dispatched", PathAddress.pathAddress("subsystem", "test"), added.get());
        assertEquals("Removed address should be dispatched", PathAddress.pathAddress("subsystem", "test"), removed.get());
    }

    /** Captures one notification registration for test assertions. */
    private record Registration(PathAddress address, NotificationHandler handler, NotificationFilter filter) { }

    /** Records registry interactions without requiring a live management server. */
    private static class RecordingRegistry implements NotificationHandlerRegistry {
        private final List<Registration> registered = new ArrayList<>();
        private final List<Registration> unregistered = new ArrayList<>();

        /** Records one registration request. */
        @Override
        public void registerNotificationHandler(PathAddress source, NotificationHandler handler, NotificationFilter filter) {
            registered.add(new Registration(source, handler, filter));
        }

        /** Records one unregistration request. */
        @Override
        public void unregisterNotificationHandler(PathAddress source, NotificationHandler handler, NotificationFilter filter) {
            unregistered.add(new Registration(source, handler, filter));
        }
    }
}
