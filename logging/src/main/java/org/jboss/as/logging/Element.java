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

import org.jboss.as.controller.AttributeDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public enum Element {

    UNKNOWN((String) null),

    ACCEPT(CommonAttributes.ACCEPT),
    ALL(CommonAttributes.ALL),
    ANY(CommonAttributes.ANY),
    APPEND(CommonAttributes.APPEND),
    ASYNC_HANDLER(CommonAttributes.ASYNC_HANDLER),
    CHANGE_LEVEL(CommonAttributes.CHANGE_LEVEL),
    CONSOLE_HANDLER(CommonAttributes.CONSOLE_HANDLER),
    CUSTOM_HANDLER(CommonAttributes.CUSTOM_HANDLER),
    DENY(CommonAttributes.DENY),
    ENCODING(CommonAttributes.ENCODING),
    FILE(CommonAttributes.FILE),
    FILE_HANDLER(CommonAttributes.FILE_HANDLER),
    FILTER(CommonAttributes.FILTER),
    FORMATTER(CommonAttributes.FORMATTER),
    HANDLER(CommonAttributes.HANDLER),
    HANDLERS(CommonAttributes.HANDLERS),
    LEVEL(CommonAttributes.LEVEL),
    LEVEL_RANGE(CommonAttributes.LEVEL_RANGE),
    LOGGER(CommonAttributes.LOGGER),
    MATCH(CommonAttributes.MATCH),
    MAX_BACKUP_INDEX(CommonAttributes.MAX_BACKUP_INDEX),
    NOT(CommonAttributes.NOT),
    OVERFLOW_ACTION(CommonAttributes.OVERFLOW_ACTION),
    PATTERN_FORMATTER(CommonAttributes.PATTERN_FORMATTER),
    PERIODIC_ROTATING_FILE_HANDLER(CommonAttributes.PERIODIC_ROTATING_FILE_HANDLER),
    PROPERTIES(CommonAttributes.PROPERTIES),
    PROPERTY(CommonAttributes.PROPERTY),
    QUEUE_LENGTH(CommonAttributes.QUEUE_LENGTH),
    REPLACE(CommonAttributes.REPLACE),
    ROOT_LOGGER(CommonAttributes.ROOT_LOGGER),
    ROTATE_SIZE(CommonAttributes.ROTATE_SIZE),
    SIZE_ROTATING_FILE_HANDLER(CommonAttributes.SIZE_ROTATING_FILE_HANDLER),
    SUBHANDLERS(CommonAttributes.SUBHANDLERS),
    SUFFIX(CommonAttributes.SUFFIX),
    TARGET(CommonAttributes.TARGET),;

    private final String name;
    private final AttributeDefinition definition;

    Element(final String name) {
        this.name = name;
        this.definition = null;
    }

    Element(final AttributeDefinition definition) {
        this.name = definition.getXmlName();
        this.definition = definition;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    public boolean hasDefinition() {
        return definition != null;
    }

    public AttributeDefinition getDefinition() {
        return definition;
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
