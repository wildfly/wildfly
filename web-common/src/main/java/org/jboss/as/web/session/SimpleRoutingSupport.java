/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
