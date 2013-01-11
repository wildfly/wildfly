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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.services.path.PathResourceDefinition;

/**
 *
 */
enum Attribute {

    UNKNOWN((String) null),

    APPEND(CommonAttributes.APPEND),
    AUTOFLUSH(CommonAttributes.AUTOFLUSH),
    CATEGORY(LoggerResourceDefinition.CATEGORY),
    CLASS(CustomHandlerResourceDefinition.CLASS),
    ENABLED(CommonAttributes.ENABLED),
    MIN_INCLUSIVE(CommonAttributes.MIN_INCLUSIVE),
    MIN_LEVEL(CommonAttributes.MIN_LEVEL),
    MAX_BACKUP_INDEX(SizeRotatingHandlerResourceDefinition.MAX_BACKUP_INDEX),
    MAX_INCLUSIVE(CommonAttributes.MAX_INCLUSIVE),
    MAX_LEVEL(CommonAttributes.MAX_LEVEL),
    MODULE(CustomHandlerResourceDefinition.MODULE),
    NAME("name"),
    NEW_LEVEL(CommonAttributes.NEW_LEVEL),
    OVERFLOW_ACTION(AsyncHandlerResourceDefinition.OVERFLOW_ACTION),
    PATH(PathResourceDefinition.PATH),
    PATTERN(CommonAttributes.PATTERN),
    QUEUE_LENGTH(AsyncHandlerResourceDefinition.QUEUE_LENGTH),
    RELATIVE_TO(PathResourceDefinition.RELATIVE_TO),
    REPLACEMENT(CommonAttributes.REPLACEMENT),
    REPLACE_ALL(CommonAttributes.REPLACE_ALL),
    ROTATE_SIZE(SizeRotatingHandlerResourceDefinition.ROTATE_SIZE),
    SUFFIX(PeriodicHandlerResourceDefinition.SUFFIX),
    SYSLOG_TYPE("syslog-type"),
    TARGET(ConsoleHandlerResourceDefinition.TARGET),
    USE_PARENT_HANDLERS(LoggerResourceDefinition.USE_PARENT_HANDLERS),
    VALUE("value"),;

    private final String name;
    private final AttributeDefinition definition;

    Attribute(final AttributeDefinition definition) {
        if (definition == null) {
            this.name = null;
        } else {
            this.name = definition.getXmlName();
        }
        this.definition = definition;
    }

    Attribute(final String name) {
        this.name = name;
        this.definition = null;
    }

    /**
     * Get the local name of this attribute.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    public AttributeDefinition getDefinition() {
        return definition;
    }

    private static final Map<String, Attribute> MAP;

    static {
        final Map<String, Attribute> map = new HashMap<String, Attribute>();
        for (Attribute element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static Attribute forName(String localName) {
        final Attribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    public static Map<String, Attribute> getMap() {
        return MAP;
    }
}
