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

import org.jboss.as.controller.registry.NotificationHandlerRegistration;

/**
 * The NotificationSupport can be used to emit notifications.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public interface NotificationSupport {

    /**
     * Get the notification registry to register/unregister notification handlers
     */
    NotificationHandlerRegistration getNotificationRegistry();

    /**
     * Emit {@link Notification}(s).
     *
     * @param notifications the notifications to emit
     */
    void emit(final Notification... notifications);

    class Factory {
        private Factory() {
        }

        /**
         * If the {@code executorService} parameter is null, the notifications will be emitted synchronously
         * and may be subject to handlers blocking the execution.
         *
         * @param executorService can be {@code null}.
         */
        public static NotificationSupport create(ExecutorService executorService) {
            NotificationHandlerRegistration registry = NotificationHandlerRegistration.Factory.create();
            if (executorService == null) {
                return new NotificationSupports.BlockingNotificationSupport(registry);
            } else {
                return new NotificationSupports.NonBlockingNotificationSupport(registry, executorService);
            }
        }
    }
}
