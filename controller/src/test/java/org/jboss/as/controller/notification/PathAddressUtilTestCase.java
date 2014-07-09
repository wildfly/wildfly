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

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.notification.NotificationRegistry.ANY_ADDRESS;
import static org.jboss.as.controller.notification.PathAddressUtil.matches;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jboss.as.controller.PathAddress;
import org.junit.Test;

/**
 * Tests for path address wildcard syntax.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 */
public class PathAddressUtilTestCase {

    @Test
    public void testPathAddressMatches() {
        // address is /subsystem=messaging/hornetq-server=default/jms-queue=myQueue
        PathAddress address = pathAddress(pathElement("subsystem", "messaging"), pathElement("hornetq-server", "default"), pathElement("jms-queue", "myQueue"));
        assertTrue(matches(address, address));
        assertTrue(matches(address, ANY_ADDRESS));

        // address does not match its parent /subsystem=messaging/hornetq-server=default
        PathAddress parent = address.subAddress(0, address.size() - 1);
        assertFalse(matches(address, parent));

        // address does not match its sibling /subsystem=messaging/hornetq-server=default/jms-topic=*
        PathAddress sibling = parent.append("jms-topic", "*");
        assertFalse(matches(address, sibling));

        // address does not match its child /subsystem=messaging/hornetq-server=default/jms-queue=myQueue/subresource=mySubresource
        PathAddress child = address.append("subresource", "mySubresource");
        assertFalse(matches(address, child));

        // address matches the pattern /subsystem=messaging/hornetq-server=default/jms-queue=*
        PathAddress pattern = pathAddress(pathElement("subsystem", "messaging"), pathElement("hornetq-server", "default"), pathElement("jms-queue", "*"));
        assertTrue(matches(address, pattern));
        // address matches the pattern /subsystem=messaging/hornetq-server=*/jms-queue=*
        pattern = pathAddress(pathElement("subsystem", "messaging"), pathElement("hornetq-server", "*"), pathElement("jms-queue", "*"));
        assertTrue(matches(address, pattern));
    }
}
