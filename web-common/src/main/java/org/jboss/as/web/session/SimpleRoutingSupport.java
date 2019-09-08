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

import java.nio.CharBuffer;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;

import org.wildfly.common.string.CompositeCharSequence;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Implements logic for parsing/appending routing information from/to a session identifier.
 *
 * @author Paul Ferraro
 */
public class SimpleRoutingSupport implements RoutingSupport {

    private static final String DELIMITER = WildFlySecurityManager.getPropertyPrivileged("jboss.session.route.delimiter", ".");

    @Override
    public Map.Entry<CharSequence, CharSequence> parse(CharSequence id) {
        if (id != null) {
            int length = id.length();
            int delimiterLength = DELIMITER.length();
            for (int i = 0; i <= length - delimiterLength; ++i) {
                int routeStart = i + delimiterLength;
                if (DELIMITER.contentEquals(id.subSequence(i, routeStart))) {
                    return new SimpleImmutableEntry<>(CharBuffer.wrap(id, 0, i), CharBuffer.wrap(id, routeStart, length));
                }
            }
        }
        return new SimpleImmutableEntry<>(id, null);
    }

    @Override
    public CharSequence format(CharSequence sessionId, CharSequence routeId) {
        if ((routeId == null) || (routeId.length() == 0)) return sessionId;

        return new CompositeCharSequence(sessionId, DELIMITER, routeId);
    }
}
