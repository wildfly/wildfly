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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.notification.NotificationFilter.ALL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class NotificationSupportImplTestCase {

    @Before
    public void setUp() {
    }

    @Test
    public void testNotificationOrderingWithExecutor() throws Exception {
        doNotificationOrdering(Executors.newFixedThreadPool(12));
    }

    @Test
    public void testNotificationOrderingWithoutExecutor() throws Exception {
        doNotificationOrdering(null);
    }

    @Ignore
    @Test
    public void doManyNotificationOrderingWithExecutor() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(12);
        for (int i = 0; i < 100000; i++) {
            doNotificationOrdering(executor);
        }
    }

    private void  doNotificationOrdering(ExecutorService executor) throws Exception {
        int numberOfNotificationsEmitted = 12;
        final CountDownLatch latch = new CountDownLatch(numberOfNotificationsEmitted);

        NotificationSupport notificationSupport = NotificationSupport.Factory.create(executor);

        CountdownListBackedNotificationHandler handler = new CountdownListBackedNotificationHandler(latch);

        notificationSupport.getNotificationRegistry().registerNotificationHandler(NotificationRegistry.ANY_ADDRESS, handler, ALL);

        List<Notification> notifications1 = new ArrayList<Notification>();
        notifications1.add(new Notification("foo", pathAddress("resource", "foo"), "foo"));
        notifications1.add(new Notification("foo", pathAddress("resource", "foo"), "bar"));
        notifications1.add(new Notification("foo", pathAddress("resource", "foo"), "baz"));

        List<Notification> notifications2 = new ArrayList<Notification>();
        notifications2.add(new Notification("bar", pathAddress("resource", "bar"), "foo"));
        notifications2.add(new Notification("bar", pathAddress("resource", "bar"), "bar"));
        notifications2.add(new Notification("bar", pathAddress("resource", "bar"), "baz"));

        final Notification[] notifs1 = notifications1.toArray(new Notification[notifications1.size()]);
        final Notification[] notifs2 = notifications2.toArray(new Notification[notifications2.size()]);

        // emit the notifications1 a 1st time
        notificationSupport.emit(notifs1);
        // emit the notifications2 a 1st time
        notificationSupport.emit(notifs2);
        // emit the notifications1 a 2nd time
        notificationSupport.emit(notifs1);
        // emit the notifications2 a 2nd time
        notificationSupport.emit(notifs2);

        assertTrue(latch.await(5, SECONDS));

        assertEquals(handler.getNotifications().toString(), numberOfNotificationsEmitted, handler.getNotifications().size());

        for (Notification notification : handler.getNotifications()) {
            assertNotNull(notification);
        }

        // handled the 1st notifications1 that were emitted
        assertEquals(notifications1, handler.getNotifications().subList(0, 3));
        // handled the 1st notifications2 that were emitted
        assertEquals(notifications2, handler.getNotifications().subList(3, 6));
        // handled the 2nd notifications1 that were emitted
        assertEquals(notifications1, handler.getNotifications().subList(6, 9));
        // handled the 2nd notifications2 that were emitted
        assertEquals(notifications2, handler.getNotifications().subList(9, 12));
    }
}
