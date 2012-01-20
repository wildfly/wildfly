/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.host.controller.parsing;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * A class that represents host identifier.
 *
 * There exist three variants of the identifier:
 * <ul>
 *  <li>explicit human name</li>
 *  <li>system variable<li>
 *  <li>an immutable universally unique identifier</li>
 * </ul>
 *
 * @author kulikov
 */
public class HostIdentifier {
    private String name;

    /**
     * Constructs host identifier follow to the offer.
     *
     *
     * @param offer user's offer for generating identifier. Possible values are:
     * - ${system.property} indicates that system.property will be used as identifier
     * - Text 'abc' indicates that this value will be used as identifier
     * - Null or ${jboss.domain.guid} generates unique identifier.
     */
    public HostIdentifier(String offer) {
        InetAddress localhost = null;
        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            //never happen
        }

        if (offer == null) {
            name = UUID.nameUUIDFromBytes(localhost.getAddress()).toString();
        } else if (offer.startsWith("${") && offer.endsWith("}")) {
            String val = offer.substring(2, offer.length() - 1);
            if (val.equalsIgnoreCase("jboss.domain.guid")) {
                name = UUID.nameUUIDFromBytes(localhost.getAddress()).toString();
            } else {
                name = System.getProperty(val);
            }
        } else {
            name = offer;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
