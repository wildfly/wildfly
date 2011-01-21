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
 * @author Emanuel Muckenhuber
 */
enum LoggerHandlerType {

    UNKNOWN(null),

    ASYNC_HANDLER(CommonAttributes.ASYNC_HANDLER),
    CONSOLE_HANDLER(CommonAttributes.CONSOLE_HANDLER),
    FILE_HANDLER(CommonAttributes.FILE_HANDLER),
    HANDLER(CommonAttributes.HANDLER),
    PERIODIC_ROTATING_FILE_HANDLER(CommonAttributes.PERIODIC_ROTATING_FILE_HANDLER),
    SIZE_ROTATING_FILE_HANDLER(CommonAttributes.SIZE_ROTATING_FILE_HANDLER),
    ;

    private final String name;

    LoggerHandlerType(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getName() {
        return name;
    }

    private static final Map<String, LoggerHandlerType> MAP;

    static {
        final Map<String, LoggerHandlerType> map = new HashMap<String, LoggerHandlerType>();
        for (LoggerHandlerType element : values()) {
            final String name = element.getName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static LoggerHandlerType forName(String localName) {
        final LoggerHandlerType element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

}
