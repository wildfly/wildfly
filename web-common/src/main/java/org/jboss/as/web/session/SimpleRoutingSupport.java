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
package org.jboss.as.web.session;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;

/**
 * Implements logic for parsing/appending routing information from/to a session identifier.
 *
 * @author Paul Ferraro
 */
public class SimpleRoutingSupport implements RoutingSupport {

    public static final String DEFAULT_DELIMITER = ".";

    private final String delimiter;

    public SimpleRoutingSupport() {
        this(DEFAULT_DELIMITER);
    }

    public SimpleRoutingSupport(String delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public Map.Entry<String, String> parse(String id) {
        int index = (id != null) ? id.indexOf(this.delimiter) : -1;
        return (index < 0) ? new SimpleImmutableEntry<String, String>(id, null) : new SimpleImmutableEntry<>(id.substring(0, index), id.substring(index + this.delimiter.length()));
    }

    @Override
    public String format(String sessionId, String routeId) {
        if ((routeId != null) && !routeId.isEmpty()) {
            StringBuilder sb = new StringBuilder(sessionId.length() + delimiter.length() + routeId.length());
            sb.append(sessionId);
            sb.append(this.delimiter);
            sb.append(routeId);
            return sb.toString();
        } else {
            return sessionId;
        }
    }
}
