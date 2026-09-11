/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_ADDED_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_REMOVED_NOTIFICATION;

import java.util.function.Consumer;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.notification.Notification;
import org.jboss.as.controller.notification.NotificationFilter;
import org.jboss.as.controller.notification.NotificationHandler;
import org.jboss.as.controller.notification.NotificationHandlerRegistry;

final class MicrometerNotificationHandler implements NotificationHandler {
    private static final NotificationFilter FILTER = notification ->
            RESOURCE_ADDED_NOTIFICATION.equals(notification.getType())
                    || RESOURCE_REMOVED_NOTIFICATION.equals(notification.getType());

    private final NotificationHandlerRegistry registry;
    private final Consumer<PathAddress> resourceAdded;
    private final Consumer<PathAddress> resourceRemoved;

    MicrometerNotificationHandler(NotificationHandlerRegistry registry,
                                   Consumer<PathAddress> resourceAdded,
                                   Consumer<PathAddress> resourceRemoved) {
        this.registry = registry;
        this.resourceAdded = resourceAdded;
        this.resourceRemoved = resourceRemoved;
    }

    void start() {
        registry.registerNotificationHandler(NotificationHandlerRegistry.ANY_ADDRESS, this, FILTER);
    }

    void stop() {
        registry.unregisterNotificationHandler(NotificationHandlerRegistry.ANY_ADDRESS, this, FILTER);
    }

    @Override
    public void handleNotification(Notification notification) {
        switch (notification.getType()) {
            case RESOURCE_ADDED_NOTIFICATION -> resourceAdded.accept(notification.getSource());
            case RESOURCE_REMOVED_NOTIFICATION -> resourceRemoved.accept(notification.getSource());
            default -> { }
        }
    }
}
