/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.notification;

import static org.jboss.as.controller.ControllerLogger.SERVER_MANAGEMENT_LOGGER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.PathAddress;

/**
 * This service manages notification handler registration and emit notifications on behalf of resources.
 *
 * Registration and unregistration operation are synchronous.
 * Emission of notifications is performed asynchronously if an executor service is passed to the constructor. Otherwise, it happens synchronously.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 */
class NotificationSupportImpl implements NotificationSupport {

    private final Map<PathAddress, Set<NotificationHandlerEntry>> notificationHandlers = new HashMap<PathAddress, Set<NotificationHandlerEntry>>();
    private final ExecutorService executor;

    /**
     * Create a new notification support object
     *
     * @param executorService an optional executor service. If {@code null} is passed, the emission of notification happens synchronously.
     */
    NotificationSupportImpl(ExecutorService executorService) {
        this.executor = executorService;
    }

    @Override
    public void registerNotificationHandler(PathAddress source, NotificationHandler handler, NotificationFilter filter) {
        Set<NotificationHandlerEntry> handlers = notificationHandlers.get(source);
        if (handlers == null) {
            handlers = new HashSet<NotificationHandlerEntry>();
        }
        handlers.add(new NotificationHandlerEntry(handler, filter));
        notificationHandlers.put(source, handlers);
    }

    @Override
    public void unregisterNotificationHandler(PathAddress source, NotificationHandler handler, NotificationFilter filter) {
        NotificationHandlerEntry entry = new NotificationHandlerEntry(handler, filter);
        Set<NotificationHandlerEntry> handlers = notificationHandlers.get(source);
        if (handlers != null) {
            boolean remove = handlers.remove(entry);
            System.out.println("remove = " + remove);
        }
    }

    @Override
    public void emit(final Notification... notifications) {
        if (executor == null) {
            fireNotifications(notifications);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    fireNotifications(notifications);
                }
            });
        }
    }

    private void fireNotifications(final Notification... notifications) {
        for (Notification notification : notifications) {
            try {
                // each notification may have a different subset of handlers depending on their filters
                for (NotificationHandler handler : findMatchingNotificationHandlers(notification)) {
                    handler.handleNotification(notification);
                }
            } catch (Throwable t) {
                SERVER_MANAGEMENT_LOGGER.failedToEmitNotification(notification, t);
            }
        }
    }

    /**
     * Find all the notification handlers that are registered for the notification's source (including those that are
     * registered against a path address pattern) and their filter accepted the notification
     *
     * @param notification the notification
     * @return the notification handlers that will effectively handled the notification
     */
    private List<NotificationHandler> findMatchingNotificationHandlers(Notification notification) {
        final List<NotificationHandler> handlers = new ArrayList<NotificationHandler>();
        for (Map.Entry<PathAddress, Set<NotificationHandlerEntry>> entries : notificationHandlers.entrySet()) {
            if (PathAddressUtil.matches(PathAddress.pathAddress(notification.getResource()), entries.getKey())) {
                for (NotificationHandlerEntry entry : entries.getValue()) {
                    if (entry.getFilter().isNotificationEnabled(notification)) {
                        handlers.add(entry.getHandler());
                    }
                }
            }
        }
        return handlers;
    }

    private class NotificationHandlerEntry {
        private final NotificationHandler handler;
        private final NotificationFilter filter;

        private NotificationHandlerEntry(NotificationHandler handler, NotificationFilter filter) {
            this.handler = handler;
            this.filter = filter;
        }

        public NotificationHandler getHandler() {
            return handler;
        }

        public NotificationFilter getFilter() {
            return filter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NotificationHandlerEntry that = (NotificationHandlerEntry) o;

            if (filter != null ? !filter.equals(that.filter) : that.filter != null) return false;
            if (handler != null ? !handler.equals(that.handler) : that.handler != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = handler != null ? handler.hashCode() : 0;
            result = 31 * result + (filter != null ? filter.hashCode() : 0);
            return result;
        }
    }
}
