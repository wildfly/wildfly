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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.notification.Notification;
import org.jboss.as.controller.notification.NotificationFilter;
import org.jboss.as.controller.notification.NotificationHandler;

/**
 * The NotificationHandlerRegistration is used to register and unregister notification handlers.
 *
 * Notification handlers are registered against {@code PathAddress}.
 *
 * The source PathAddress can be a pattern if at least one of its element value is a wildcard ({@link org.jboss.as.controller.PathElement#getValue()} is {@code *}).
 * For example:
 * <ul>
 *     <li>{@code /subsystem=messaging/hornetq-server=default/jms-queue=&#42;} is an address pattern.</li>
 *     <li>{@code /subsystem=messaging/hornetq-server=&#42;/jms-queue=&#42;} is an address pattern.</li>
 *     <li>{@code /subsystem=messaging/hornetq-server=default/jms-queue=myQueue} is <strong>not</strong> an address pattern.</li>
 * </ul>
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public interface NotificationHandlerRegistration {

    /**
     * Special path address to register a notification handler for <em>any</em> source.
     *
     * A handler registered with this address will receive <em>all</em> notifications emitted by <em>any</em> source.
     * It is advised to use a suitable {@code NotificationFilter} to constrain the received notifications (e.g. by their types).
     */
    PathAddress ANY_ADDRESS = PathAddress.EMPTY_ADDRESS;

    /**
     * Register the given NotificationHandler to receive notifications emitted by the resource at the given source address.
     * The {@link org.jboss.as.controller.notification.NotificationHandler#handleNotification(org.jboss.as.controller.notification.Notification)} method will only be called on the registered handler if the filter's {@link org.jboss.as.controller.notification.NotificationFilter#isNotificationEnabled(org.jboss.as.controller.notification.Notification)}
     * returns {@code true} for the given notification.
     * <br />
     *
     * @param source the path address of the resource that emit notifications.
     * @param handler the notification handler
     * @param filter the notification filter. Use {@link org.jboss.as.controller.notification.NotificationFilter#ALL} to let the handler always handle notifications
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
     * Return all the {@code NotificationHandler} that where registered to listen to the notification's source address (either directly
     * or though pattern addresses) after filtering them out using the {@code NotificationFilter} given at registration time.
     *
     * @param notification the source address of the notification must be a concrete address correspdonding to a resource
     *                     (and not a wildcard address)
     * @return all the filtered {@code NotificationHandler} that registered against the notification source.
     */
    Collection<NotificationHandler> findMatchingNotificationHandlers(Notification notification);

    /**
     * Factory to create a new {@code NotificationHandlerRegistration}
     */
    public static class Factory {
        /**
         * @return a new instance of {@code NotificationHandlerRegistration}
         */
        public static NotificationHandlerRegistration create() {
            return new ConcreteNotificationHandlerRegistration();
        }
    }
}
