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

import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.PathAddress;

/**
 * The NotificationSupport can be used to register/unregister notification handlers and emit notifications.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public interface NotificationSupport {

    /**
     * Special path address to register a notification handler for *any* source.
     */
    PathAddress ANY_ADDRESS = PathAddress.EMPTY_ADDRESS;

    /**
     * Register the given NotificationHandler to receive notifications emitted by the resource at the given source address.
     * The {@link NotificationHandler#handleNotification(Notification)} method will only be called on the registered handler if the filter's {@link NotificationFilter#isNotificationEnabled(Notification)}
     * returns {@code true} for the given notification.
     * <br />
     * The source PathAddress can be a pattern if at least one of its element value is a wildcard ({@link org.jboss.as.controller.PathElement#getValue()} is {@code *}).
     * For example:
     * <ul>
     *     <li>{@code /subsystem=messaging/hornetq-server=default/jms-queue=&#42;} is an address pattern.</li>
     *     <li>{@code /subsystem=messaging/hornetq-server=&#42;/jms-queue=&#42;} is an address pattern.</li>
     *     <li>{@code /subsystem=messaging/hornetq-server=default/jms-queue=myQueue} is <strong>not</strong> an address pattern.</li>
     * </ul>
     *
     * @param source the path address of the resource that emit notifications.
     * @param handler the notification handler
     * @param filter the notification filter. Use {@link NotificationFilter#ALL} to let the handler always handle notifications
     */
    void registerNotificationHandler(PathAddress source, NotificationHandler handler, NotificationFilter filter);

    /**
     * Unregister the given NotificationHandler to stop receiving notifications emitted by the resource at the given source address.
     *
     * The source, handler and filter must match the values that were used during registration to be effectively unregistered.
     *
     * @param source the path address of the resource that emit notifications.
     * @param handler the notification handler
     * @param filter the notification filter
     */
     void unregisterNotificationHandler(PathAddress source, NotificationHandler handler, NotificationFilter filter);

    /**
     * Emit {@link Notification}(s).
     *
     * @param notifications the notifications to emit
     */
    void emit(final Notification... notifications);

    class Factory {
        private Factory() {
        }

        public static NotificationSupport create(ExecutorService executorService) {
            return new NotificationSupportImpl(executorService);
        }
    }
}
