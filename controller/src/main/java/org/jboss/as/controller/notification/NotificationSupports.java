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

package org.jboss.as.controller.notification;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.registry.NotificationHandlerRegistration;

/**
 * Provides implementation of the {@code NotificationSupport}.
 *
 * The {@code BlockingNotificationSupport} will fire the notifications and deliver them to the handlers on the current thread.
 * Its {@code emit()} method will return after the notifications have all been delivered (and blocks the code execution until it is done).
 *
 * The {@code NonBlockingNotificationSupport} will fire the notifications in a separate thread (provided by its {@code
 *  executorService}.
 * Its {@code emit()} method will return immediately and will not block the code execution.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
class NotificationSupports {

    static class BlockingNotificationSupport implements NotificationSupport {

        private final NotificationHandlerRegistration registry;

        public BlockingNotificationSupport(NotificationHandlerRegistration registry) {
            this.registry = registry;
        }

        @Override
        public void emit(Notification... notifications) {
            fireNotifications(registry, notifications);
        }

        @Override
        public NotificationHandlerRegistration getNotificationRegistry() {
            return registry;
        }
    }

    static class NonBlockingNotificationSupport implements  NotificationSupport {

        private final NotificationHandlerRegistration registry;
        private final ExecutorService executor;

        /**
         * Use a concurrent queue to put the notifications in it when {@code emit()} is called.
         * The queue will be drained in a separate thread and the notifications effectively delivered to the handlers.
         *
         * This ensures that the notifications will be delivered in the same order they were emitted.
         */
        private final Queue<Notification> queue = new ConcurrentLinkedQueue<Notification>();

        /**
         * use the lock's exclusive writeLock to ensure only one thread can drain the queue at a given time.
         */
        private final ReadWriteLock lock = new ReentrantReadWriteLock();

        public NonBlockingNotificationSupport(NotificationHandlerRegistration registry, ExecutorService executor) {
            this.registry = registry;
            this.executor = executor;
        }

        @Override
        public synchronized void emit(Notification... notifications) {
            queue.addAll(Arrays.asList(notifications));

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    lock.writeLock().lock();
                    try {
                        while (true) {
                            Notification notification = queue.poll();
                            if (notification == null) {
                                break;
                            }
                            fireNotifications(registry, notification);
                        }
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
            });
        }

        @Override
        public NotificationHandlerRegistration getNotificationRegistry() {
            return registry;
        }
    }


    private static void fireNotifications(NotificationHandlerRegistration registry, final Notification... notifications) {
        for (Notification notification : notifications) {
            try {
                // each notification may have a different subset of handlers depending on their filters
                for (NotificationHandler handler : registry.findMatchingNotificationHandlers(notification)) {
                    handler.handleNotification(notification);
                }
            } catch (Throwable t) {
                ControllerLogger.ROOT_LOGGER.failedToEmitNotification(notification, t);
            }
        }
    }
}
