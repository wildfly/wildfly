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

public class MicrometerNotificationHandlerTest {
    @Test
    public void registersAndUnregistersWithTheSameRegistration() {
        RecordingRegistry registry = new RecordingRegistry();
        MicrometerNotificationHandler handler = new MicrometerNotificationHandler(registry, notification -> { }, notification -> { });

        handler.start();
        handler.stop();

        assertEquals(1, registry.registered.size());
        assertEquals(registry.registered.get(0), registry.unregistered.get(0));
        assertEquals(NotificationHandlerRegistry.ANY_ADDRESS, registry.registered.get(0).address());
    }

    @Test
    public void filtersResourceLifecycleNotifications() {
        RecordingRegistry registry = new RecordingRegistry();
        MicrometerNotificationHandler handler = new MicrometerNotificationHandler(registry, notification -> { }, notification -> { });

        handler.start();
        NotificationFilter filter = registry.registered.get(0).filter();

        assertTrue(filter.isNotificationEnabled(new Notification("resource-added", PathAddress.EMPTY_ADDRESS, "added")));
        assertTrue(filter.isNotificationEnabled(new Notification("resource-removed", PathAddress.EMPTY_ADDRESS, "removed")));
        assertTrue(!filter.isNotificationEnabled(new Notification("attribute-value-written", PathAddress.EMPTY_ADDRESS, "written")));
    }

    @Test
    public void dispatchesResourceLifecycleNotifications() {
        RecordingRegistry registry = new RecordingRegistry();
        AtomicReference<PathAddress> added = new AtomicReference<>();
        AtomicReference<PathAddress> removed = new AtomicReference<>();
        MicrometerNotificationHandler handler = new MicrometerNotificationHandler(registry, added::set, removed::set);

        handler.handleNotification(new Notification("resource-added", PathAddress.pathAddress("subsystem", "test"), "added"));
        handler.handleNotification(new Notification("resource-removed", PathAddress.pathAddress("subsystem", "test"), "removed"));

        assertEquals(PathAddress.pathAddress("subsystem", "test"), added.get());
        assertEquals(PathAddress.pathAddress("subsystem", "test"), removed.get());
    }

    private record Registration(PathAddress address, NotificationHandler handler, NotificationFilter filter) { }

    private static class RecordingRegistry implements NotificationHandlerRegistry {
        private final List<Registration> registered = new ArrayList<>();
        private final List<Registration> unregistered = new ArrayList<>();

        @Override
        public void registerNotificationHandler(PathAddress source, NotificationHandler handler, NotificationFilter filter) {
            registered.add(new Registration(source, handler, filter));
        }

        @Override
        public void unregisterNotificationHandler(PathAddress source, NotificationHandler handler, NotificationFilter filter) {
            unregistered.add(new Registration(source, handler, filter));
        }
    }
}
