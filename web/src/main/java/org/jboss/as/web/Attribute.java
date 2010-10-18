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

    CHECK_INTERVAL("check-interval"),

    DEFAULT_HOST("default-host"), DEVELOPMENT("development"), DIRECTORY("directory"), DISABLED("disabled"), DISPLAY_SOOURCE_FRAGMENT("display-source-fragment"), DUMP_SMAP("dump-smap"),

    ENABLED("enabled"), ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUT("error-on-use-bean-invalid-class-attribute"), EXECUTOR("executor"), EXTENDED("extended"),

    FILE_ENCONDING("file-encoding"),

    GENERATE_STRINGS_AS_CHAR_ARRAYS("generate-strings-as-char-arrays"),

    JAVA_ENCODING("java-encoding"),

    KEEP_GENERATED("keep-generated"),

    LISTINGS("listings"),

    MAPPED_FILE("mapped-file"), MAX_DEPTH("max-depth"), MAX_POST_SIZE("max-post-size"), MODIFIFICATION_TEST_INTERVAL("modification-test-interval"), MAX_SAVE_POST_SIZE("max-save-post-size"),

    NAME("name"),

    PATH("path"), PATTERN("pattern"), PREFIX("prefix"), PROTOCOL("protocol"), PROXY_NAME("proxy-name"), PROXY_PORT("proxy-port"),

    READ_ONLY("read-only"), REDIRECT_PORT("redirect-por"),  RECOMPILE_ON_FAIL("recompile-on-fail"), RELATIVE_TO("relative-to"), RESOLVE_HOSTS("resolve-hosts"), ROTATE("rotate"),

    SCHEME("scheme"), SCRATCH_DIR("scratch-dir"), SECRET("secret"), SECURE("secure"), SENDFILE("sendfile"), SMAP("smap"), SOCKET_BINDING("socket-binding"), SOURCE_VM("source-vm"),

    TARGET_VM("target-vm"), TRIM_SPACES("trim-spaces"), TAG_POOLING("tag-pooling"),

    WEBDAV("webdav"),

    X_POWERED_BY("x-powered-by"), ENABLE_LOOKUPS("enable-lookups"), VALUE("value"),
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
