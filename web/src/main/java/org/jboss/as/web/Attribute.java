/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.web;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Emanuel Muckenhuber
 * @author Jean-Frederic Clere
 */
enum Attribute {
    UNKNOWN(null),

    CHECK_INTERVAL(CommonAttributes.CHECK_INTERVAL),
    DEFAULT_HOST(CommonAttributes.DEFAULT_HOST),
    DEVELOPMENT(CommonAttributes.DEVELOPMENT),
    DIRECTORY(CommonAttributes.DIRECTORY),
    DISABLED(CommonAttributes.DISABLED),
    DISPLAY_SOOURCE_FRAGMENT(CommonAttributes.DISPLAY_SOURCE_FRAGMENT),
    DUMP_SMAP(CommonAttributes.DUMP_SMAP),
    ENABLED(CommonAttributes.ENABLED),
    ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUT(CommonAttributes.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE),
    EXECUTOR(CommonAttributes.EXECUTOR),
    EXTENDED(CommonAttributes.EXTENDED),
    FILE_ENCONDING(CommonAttributes.FILE_ENCONDING),
    GENERATE_STRINGS_AS_CHAR_ARRAYS(CommonAttributes.GENERATE_STRINGS_AS_CHAR_ARRAYS),
    JAVA_ENCODING(CommonAttributes.JAVA_ENCODING),
    KEEP_GENERATED(CommonAttributes.KEEP_GENERATED),
    LISTINGS(CommonAttributes.LISTINGS),
    MAPPED_FILE(CommonAttributes.MAPPED_FILE),
    MAX_DEPTH(CommonAttributes.MAX_DEPTH),
    MAX_POST_SIZE(CommonAttributes.MAX_POST_SIZE),
    MODIFIFICATION_TEST_INTERVAL(CommonAttributes.MODIFIFICATION_TEST_INTERVAL),
    MAX_SAVE_POST_SIZE(CommonAttributes.MAX_SAVE_POST_SIZE),
    NAME(CommonAttributes.NAME),
    PATH(CommonAttributes.PATH),
    PATTERN(CommonAttributes.PATTERN),
    PREFIX(CommonAttributes.PREFIX),
    PROTOCOL(CommonAttributes.PROTOCOL),
    PROXY_NAME(CommonAttributes.PROXY_NAME),
    PROXY_PORT(CommonAttributes.PROXY_PORT),
    READ_ONLY(CommonAttributes.READ_ONLY),
    REDIRECT_PORT(CommonAttributes.REDIRECT_PORT),
    RECOMPILE_ON_FAIL(CommonAttributes.RECOMPILE_ON_FAIL),
    RELATIVE_TO(CommonAttributes.RELATIVE_TO),
    RESOLVE_HOSTS(CommonAttributes.RESOLVE_HOSTS),
    ROTATE(CommonAttributes.ROTATE),
    SCHEME(CommonAttributes.SCHEME),
    SCRATCH_DIR(CommonAttributes.SCRATCH_DIR),
    SECRET(CommonAttributes.SECRET),
    SECURE(CommonAttributes.SECURE),
    SENDFILE(CommonAttributes.SENDFILE),
    SMAP(CommonAttributes.SMAP),
    SOCKET_BINDING(CommonAttributes.SOCKET_BINDING),
    SOURCE_VM(CommonAttributes.SOURCE_VM),
    TARGET_VM(CommonAttributes.TARGET_VM),
    TRIM_SPACES(CommonAttributes.TRIM_SPACES),
    TAG_POOLING(CommonAttributes.TAG_POOLING),
    WEBDAV(CommonAttributes.WEBDAV),
    X_POWERED_BY(CommonAttributes.X_POWERED_BY),
    ENABLE_LOOKUPS(CommonAttributes.ENABLE_LOOKUPS),
    VALUE(CommonAttributes.VALUE),
    ;

    private final String name;

    Attribute(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this attribute.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, Attribute> MAP;

    static {
        final Map<String, Attribute> map = new HashMap<String, Attribute>();
        for (Attribute element : values()) {
            final String name = element.getLocalName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    public static Attribute forName(String localName) {
        final Attribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    @Override
    public String toString() {
        return getLocalName();
    }
}
