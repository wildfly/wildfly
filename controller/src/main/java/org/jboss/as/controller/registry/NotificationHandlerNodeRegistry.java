/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.registry;

import java.util.Collection;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.notification.Notification;
import org.jboss.as.controller.notification.NotificationHandler;

/**
 * A registry of {@code NotificationHandlerEntry} (in a tree) corresponding to a {@link PathElement#getValue()}.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
class NotificationHandlerNodeRegistry {

    /**
     * The value of the node's PathElement (can be {@code null} for the root node).
     */
    private final String value;
    /**
     * The node's parent (or {@code null} for the root node).
     */
    private final NotificationHandlerNodeSubregistry parent;

    @SuppressWarnings("unused")
    private volatile Map<String, NotificationHandlerNodeSubregistry> children;

    /**
     * The collection of ({@code NotificationHandler}, {@code NotificationFilter})
     */
    private volatile Collection<ConcreteNotificationHandlerRegistration.NotificationHandlerEntry> entries;

    private static final AtomicMapFieldUpdater<NotificationHandlerNodeRegistry, String, NotificationHandlerNodeSubregistry> childrenUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(NotificationHandlerNodeRegistry.class, Map.class, "children"));

    NotificationHandlerNodeRegistry(String value, NotificationHandlerNodeSubregistry parent) {
        this.value = value;
        this.parent = parent;
        childrenUpdater.clear(this);
        entries = new CopyOnWriteArraySet<ConcreteNotificationHandlerRegistration.NotificationHandlerEntry>();
    }

    /**
     * Register the entry here (if the registry is the leaf node) or continue to traverse the tree
     */
    void registerEntry(ListIterator<PathElement> iterator, ConcreteNotificationHandlerRegistration.NotificationHandlerEntry entry) {
        if (!iterator.hasNext()) {
            // leaf node, register the entry here
            entries.add(entry);
            return;
        }
        PathElement element = iterator.next();
        NotificationHandlerNodeSubregistry subregistry = getOrCreateSubregistry(element.getKey());
        subregistry.registerEntry(iterator, element.getValue(), entry);
    }

    /**
     * Unregister the entry from here (if the registry is the leaf node) or continue to traverse the tree
     */
    void unregisterEntry(ListIterator<PathElement> iterator, ConcreteNotificationHandlerRegistration.NotificationHandlerEntry entry) {
        if (!iterator.hasNext()) {
            // leaf node, unregister the entry here
            entries.remove(entry);
            return;
        }

        PathElement element = iterator.next();
        final NotificationHandlerNodeSubregistry subregistry = children.get(element.getKey());
        if (subregistry == null) {
            return;
        }
        subregistry.unregisterEntry(iterator, element.getValue(), entry);

    }

    /**
     * Collect all the entries in the {@code handler} notifications (if the registry is the leaf node) or continue to traverse the tree
     * Only entries that are not filtered out after calling {@link org.jboss.as.controller.notification.NotificationFilter#isNotificationEnabled(org.jboss.as.controller.notification.Notification)} for the given {@code notification} are collected
     */
    void findEntries(ListIterator<PathElement> iterator, Collection<NotificationHandler> handlers, Notification notification) {
        if (!iterator.hasNext()) {
            for (ConcreteNotificationHandlerRegistration.NotificationHandlerEntry entry : entries) {
                if (entry.getFilter().isNotificationEnabled(notification)) {
                    handlers.add(entry.getHandler());
                }
            }
            return;
        }

        PathElement next = iterator.next();
        try {
            final NotificationHandlerNodeSubregistry subregistry = children.get(next.getKey());
            if (subregistry == null) {
                return;
            }
            subregistry.findHandlers(iterator, next.getValue(), notification, handlers);
        } finally {
            iterator.previous();
        }
    }

    String getLocationString() {
        if (parent == null) {
            return "";
        } else {
            return parent.getLocationString() + value + ")";
        }
    }

    /**
     * Create a {@code NotificationHandlerNodeSubregistry} for the give {@code key} or use the existing one if it already exists.
     *
     * Copied from {@link org.jboss.as.controller.registry.ConcreteResourceRegistration#getOrCreateSubregistry(String)}
     */
    NotificationHandlerNodeSubregistry getOrCreateSubregistry(final String key) {
        for (;;) {
            final Map<String, NotificationHandlerNodeSubregistry> snapshot = childrenUpdater.get(this);
            final NotificationHandlerNodeSubregistry subregistry = snapshot.get(key);
            if (subregistry != null) {
                return subregistry;
            } else {
                final NotificationHandlerNodeSubregistry newRegistry = new NotificationHandlerNodeSubregistry(key, this);
                final NotificationHandlerNodeSubregistry appearing = childrenUpdater.putAtomic(this, key, newRegistry, snapshot);
                if (appearing == null) {
                    return newRegistry;
                } else if (appearing != newRegistry) {
                    // someone else added one
                    return appearing;
                }
                // otherwise, retry the loop because the map changed
            }
        }
    }
}
