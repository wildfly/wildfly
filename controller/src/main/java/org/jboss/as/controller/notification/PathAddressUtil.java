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

import java.util.ListIterator;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class PathAddressUtil {
    /**
     * Check if the given address matches the other address that can be a pattern.
     *
     * @param address a path address
     * @param other another path address that can be a pattern
     *
     * @return {@code true} if the address matches the other address
     */
    public static boolean matches(PathAddress address, PathAddress other) {
        if (other == NotificationSupport.ANY_ADDRESS) {
            return true;
        }
        if (!other.isMultiTarget()) {
            return address.equals(other);
        }
        if (address.size() != other.size()) {
            return false;
        }
        ListIterator<PathElement> addressIter = address.iterator();
        ListIterator<PathElement> otherIterator = other.iterator();
        while (addressIter.hasNext() && otherIterator.hasNext()) {
            PathElement element = addressIter.next();
            PathElement otherElement = otherIterator.next();
            if (!otherElement.isMultiTarget()) {
                if (!element.equals(otherElement)) {
                    return false;
                }
            } else {
                if (!element.getKey().equals(otherElement.getKey())) {
                    return false;
                }
            }
        }
        return true;
    }
}
