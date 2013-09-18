/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.util;

import java.util.Map;

import javax.naming.Name;

/**
 * Default implementation of {@link NameParserResolver}. It expects keys to be equal to URL scheme.
 *
 * @author baranowb
 *
 */
public class DefaultNameParserResolver implements NameParserResolver {

    @Override
    public javax.naming.NameParser findNameParser(final String name, final Map<String, javax.naming.NameParser> parserMap,
            final javax.naming.NameParser defaultNameParser) {
        String key = getURLScheme(name);
        if (key == null) {
            key = KEY_DEFAULT;
        }
        return getParser(key, parserMap, defaultNameParser);
    }

    @Override
    public javax.naming.NameParser findNameParser(final Name name, final Map<String, javax.naming.NameParser> parserMap,
            final javax.naming.NameParser defaultNameParser) {
        String key = getURLScheme(name.get(0));
        if (key == null) {
            key = KEY_DEFAULT;
        }
        return getParser(key, parserMap, defaultNameParser);
    }

    private javax.naming.NameParser getParser(final String key, final Map<String, javax.naming.NameParser> parserMap,
            final javax.naming.NameParser defaultNameParser) {
        if (parserMap == null || parserMap.size() == 0) {
            return defaultNameParser;
        }
        if (parserMap.containsKey(key)) {
            return parserMap.get(key);
        } else {
            return defaultNameParser;
        }
    }

    private static String getURLScheme(String name) {
        int colon_posn = name.indexOf(':');
        int slash_posn = name.indexOf('/');

        if (colon_posn > 0 && ((slash_posn-1)==colon_posn))
            return name.substring(0, colon_posn);

        return null;
    }
}
