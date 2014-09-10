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

import static org.jboss.as.controller.notification.NotificationFilter.ALL;
import static org.jboss.as.controller.registry.NotificationHandlerRegistration.ANY_ADDRESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.notification.Notification;
import org.jboss.as.controller.notification.NotificationFilter;
import org.jboss.as.controller.notification.NotificationHandler;
import org.jboss.as.controller.registry.NotificationHandlerRegistration;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class NotificationRegistryTestCase {

    @Test
    public void testRegisterAndEmitFromRootAddress() {
        // register listener at /
        // find listener at /
        doTestNotificationRegistration(PathAddress.EMPTY_ADDRESS, PathAddress.EMPTY_ADDRESS, true);
    }

    @Test
    public void testRegisterAtANY_ADDRESSAndEmitFromRootAddress() {
        // register listener at ANY_ADDRESS
        // find listener at /
        doTestNotificationRegistration(ANY_ADDRESS, PathAddress.EMPTY_ADDRESS, true);
    }

    @Test
    public void testRegisterAndEmitFromOneLevelResource() {
        // register listener at /subsystem=messaging
        // find listener at /subsystem=messaging
        PathAddress oneLevelAddress = PathAddress.pathAddress("subsystem", "messaging");
        doTestNotificationRegistration(oneLevelAddress, oneLevelAddress, true);

        // do not find listener at /
        doTestNotificationRegistration(oneLevelAddress, PathAddress.EMPTY_ADDRESS, false);

        // do not find listener at /subsystem=web
        PathAddress otherAddress = PathAddress.pathAddress("subsystem", "web");
        doTestNotificationRegistration(oneLevelAddress, otherAddress, false);
    }

    @Test
    public void testRegisterAtAnyAddressAndEmitFromOneLevelResource() {
        // register listener at ANY_ADDRESS
        // find listener at /subsystem=messaging
        PathAddress oneLevelAddress = PathAddress.pathAddress("subsystem", "messaging");
        doTestNotificationRegistration(ANY_ADDRESS, oneLevelAddress, true);

        // find listener at /
        doTestNotificationRegistration(ANY_ADDRESS, PathAddress.EMPTY_ADDRESS, true);

        // find listener at /subsystem=web
        PathAddress otherAddress = PathAddress.pathAddress("subsystem", "web");
        doTestNotificationRegistration(ANY_ADDRESS, otherAddress, true);
    }

    @Test
    public void testRegisterWildCardAndEmitFromOneLevelResource() {
        // register listener at /subsystem=*
        // find listener at /subsystem=messaging
        PathAddress addressForRegistration = PathAddress.pathAddress("subsystem", "*");
        PathAddress oneLevelAddress = PathAddress.pathAddress("subsystem", "messaging");
        doTestNotificationRegistration(addressForRegistration, oneLevelAddress, true);
    }

    @Test
    public void testRegisterAndEmitFromTwoLevelResource() {
        // register listener at /subsystem=messaging/hornetq-server=default
        // find listener at /subsystem=messaging/hornetq-server=default
        PathAddress twoLevelAddress = PathAddress.pathAddress("subsystem", "messaging").append("hornetq-server", "default");
        doTestNotificationRegistration(twoLevelAddress, twoLevelAddress, true);

        // do not find listener at /subsystem=messaging/hornetq-server=bar
        PathAddress otherAddress = PathAddress.pathAddress("subsystem", "messaging").append("hornetq-server", "bar");
        doTestNotificationRegistration(twoLevelAddress, otherAddress, false);
    }

    @Test
    public void testRegisterWildCardAndEmitFromTwoLevelResource() {
        // register listener at /subsystem=messaging/hornetq-server=*
        PathAddress addressForRegistration = PathAddress.pathAddress("subsystem", "messaging").append("hornetq-server", "*");

        // find listener at /subsystem=messaging/hornetq-server=default
        PathAddress twoLevelAddress = PathAddress.pathAddress("subsystem", "messaging").append("hornetq-server", "default");
        doTestNotificationRegistration(addressForRegistration, twoLevelAddress, true);

        // do not find listener at /subsystem=web/hornetq-server=default
        PathAddress otherAddress = PathAddress.pathAddress("subsystem", "web").append("hornetq-server", "default");
        doTestNotificationRegistration(addressForRegistration, otherAddress, false);
    }

    @Test
    public void testRegisterWildCardAndEmitFromTwoLevelResource_2() {
        // register listener at /subsystem=*/hornetq-server=*
        PathAddress addressForRegistration = PathAddress.pathAddress("subsystem", "*").append("hornetq-server", "*");

        // find listener at /subsystem=messaging/hornetq-server=default
        PathAddress twoLevelAddress = PathAddress.pathAddress("subsystem", "messaging").append("hornetq-server", "default");
        doTestNotificationRegistration(addressForRegistration, twoLevelAddress, true);

        // find listener at /subsystem=web/hornetq-server=bar
        PathAddress otherAddress = PathAddress.pathAddress("subsystem", "web").append("hornetq-server", "bar");
        doTestNotificationRegistration(addressForRegistration, otherAddress, true);
    }

    @Test
    public void testRegisterWildCardAndEmitFromTwoLevelResource_3() {
        // register listener at /subsystem=*/hornetq-server=default
        // find listener at     /subsystem=messaging/hornetq-server=default
        PathAddress addressForRegistration = PathAddress.pathAddress("subsystem", "*").append("hornetq-server", "default");
        PathAddress twoLevelAddress = PathAddress.pathAddress("subsystem", "messaging").append("hornetq-server", "default");
        doTestNotificationRegistration(addressForRegistration, twoLevelAddress, true);
    }

    private void doTestNotificationRegistration(PathAddress addressForRegistration, PathAddress notificationSource, boolean mustFindHandler) {
        System.out.println("addressForRegistration = [" + addressForRegistration + "], notificationSource = [" + notificationSource + "]");
        NotificationHandlerRegistration registry = NotificationHandlerRegistration.Factory.create();

        NotificationHandler handler = new SimpleNotificationHandler();
        NotificationFilter filter = ALL;

        registry.registerNotificationHandler(addressForRegistration, handler, filter);

        Notification notification = new Notification("foo", notificationSource, "bar");

        Collection<NotificationHandler> handlers = registry.findMatchingNotificationHandlers(notification);

        if (mustFindHandler) {
            assertEquals(1, handlers.size());
            assertTrue(handlers.contains(handler));
        } else {
            assertFalse(handlers.contains(handler));
        }

        registry.unregisterNotificationHandler(addressForRegistration, handler, filter);

        handlers = registry.findMatchingNotificationHandlers(notification);

        assertEquals(0, handlers.size());
    }

    @Test
    public void testMixWildcardAndConcreteAddresses() {
        NotificationHandlerRegistration registry = NotificationHandlerRegistration.Factory.create();

        NotificationHandler handler1 = new SimpleNotificationHandler();
        NotificationHandler handler2 = new SimpleNotificationHandler();
        NotificationFilter filter = ALL;

        PathAddress wildCardRegistrationAddress = PathAddress.pathAddress("subsystem", "*");
        PathAddress concreteRegistrationAddress = PathAddress.pathAddress("subsystem", "messaging");

        registry.registerNotificationHandler(wildCardRegistrationAddress, handler1, filter);
        registry.registerNotificationHandler(concreteRegistrationAddress, handler2, filter);

        Notification notification = new Notification("foo", concreteRegistrationAddress, "bar");

        Collection<NotificationHandler> handlers = registry.findMatchingNotificationHandlers(notification);
        assertEquals(2, handlers.size());
        assertTrue(handlers.contains(handler1));
        assertTrue(handlers.contains(handler2));

        PathAddress otherAddress = PathAddress.pathAddress("subsystem", "web");
        notification = new Notification("foo", otherAddress, "bar");
        handlers = registry.findMatchingNotificationHandlers(notification);
        assertEquals(1, handlers.size());
        assertTrue(handlers.contains(handler1));
    }

    @Test
    public void testMixWildcardAndConcreteAddressesOnTwoLevels() {
        NotificationHandlerRegistration registry = NotificationHandlerRegistration.Factory.create();

        NotificationHandler handler1 = new SimpleNotificationHandler();
        NotificationHandler handler2 = new SimpleNotificationHandler();
        NotificationHandler handler3 = new SimpleNotificationHandler();
        NotificationFilter filter = ALL;

        PathAddress wildCardRegistrationAddress = PathAddress.pathAddress("subsystem", "*").append("foo", "*");
        PathAddress concreteRegistrationAddress = PathAddress.pathAddress("subsystem", "messaging").append("foo", "bar");
        PathAddress concreteRegistrationAddress2 = PathAddress.pathAddress("subsystem", "web").append("foo", "bar");

        registry.registerNotificationHandler(wildCardRegistrationAddress, handler1, filter);
        registry.registerNotificationHandler(concreteRegistrationAddress, handler2, filter);
        registry.registerNotificationHandler(concreteRegistrationAddress2, handler3, filter);

        Notification notification = new Notification("foo", concreteRegistrationAddress, "bar");

        Collection<NotificationHandler> handlers = registry.findMatchingNotificationHandlers(notification);
        assertEquals(2, handlers.size());
        assertTrue(handlers.contains(handler1));
        assertTrue(handlers.contains(handler2));

        PathAddress otherAddress = PathAddress.pathAddress("subsystem", "web").append("foo", "bar");
        notification = new Notification("foo", otherAddress, "bar");
        handlers = registry.findMatchingNotificationHandlers(notification);
        assertEquals(2, handlers.size());
        assertTrue(handlers.contains(handler1));
        assertTrue(handlers.contains(handler3));
    }

    private static class SimpleNotificationHandler implements NotificationHandler {

        @Override
        public void handleNotification(Notification notification) {

        }
    }
}
