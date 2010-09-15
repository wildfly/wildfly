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

package org.jboss.as.logging;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public enum Element {
    UNKNOWN(null),

    ACCEPT("accept"),
    ALL("all"),
    ANY("any"),
    ASYNC_HANDLER("async-handler"),
    CHANGE_LEVEL("change-level"),
    CONSOLE_HANDLER("console-handler"),
    DENY("deny"),
    FILE_HANDLER("file-handler"),
    FILTER("filter"),
    FORMATTER("formatter"),
    HANDLER("handler"),
    HANDLERS("handlers"),
    LEVEL("level"),
    LEVEL_RANGE("level-range"),
    LOGGER("logger"),
    MATCH("match"),
    NOT("not"),
    PATTERN_FORMATTER("pattern-formatter"),
    PERIODIC_ROTATING_FILE_HANDLER("periodic-rotating-file-handler"),
    PROPERTIES("properties"),
    REPLACE("replace"),
    ROOT_LOGGER("root-logger"),
    SIZE_ROTATING_FILE_HANDLER("size-rotating-file-handler"),
    SUBHANDLERS("subhandlers"),

    ;

    private final String name;

    Element(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, Element> MAP;

    static {
        final Map<String, Element> map = new HashMap<String, Element>();
        for (Element element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static Element forName(String localName) {
        final Element element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

}
