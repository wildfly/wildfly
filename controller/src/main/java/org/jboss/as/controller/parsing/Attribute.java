/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.parsing;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of all the recognized XML attributes, by local name.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public enum Attribute {
    // always first
    UNKNOWN(null),

    // xsi attributes in alpha order
    NO_NAMESPACE_SCHEMA_LOCATION("noNamespaceSchemaLocation"),
    SCHEMA_LOCATION("schemaLocation"),

    // domain 1.0 attributes in alpha order
    AUTO_START("auto-start"),
    ATTRIBUTE("attribute"),
    BASE_DN("base-dn"),
    BOOT_TIME("boot-time"),
    CODE("code"),
    CONNECTION("connection"),
    CONNECTOR("connector"),
    DEFAULT_INTERFACE("default-interface"),
    DEBUG_ENABLED("debug-enabled"),
    DEBUG_OPTIONS("debug-options"),
    DESTINATION_ADDRESS("destination-address"),
    DESTINATION_PORT("destination-port"),
    ENABLED("enabled"),
    ENV_CLASSPATH_IGNORED("env-classpath-ignored"),
    FILE("file"),
    FILTER("filter"),
    FIXED_PORT("fixed-port"),
    FIXED_SOURCE_PORT("fixed-source-port"),
    GROUP("group"),
    HOST("host"),
    HTTP("http"),
    HTTPS("https"),
    INITIAL_CONTEXT_FACTORY("initial-context-factory"),
    INTERFACE("interface"),
    JAVA_HOME("java-home"),
    MANAGEMENT_SUBSYSTEM_ENDPOINT("management-subsystem-endpoint"),
    MAX_SIZE("max-size"),
    MAX_THREADS("max-threads"),
    MODULE("module"),
    MULTICAST_ADDRESS("multicast-address"),
    MULTICAST_PORT("multicast-port"),
    NAME("name"),
    NATIVE("native"),
    PASSWORD("password"),
    PATH("path"),
    PATTERN("pattern"),
    PLAIN_TEXT("plain-text"),
    PORT("port"),
    PORT_OFFSET("port-offset"),
    PREFIX("prefix"),
    PROFILE("profile"),
    PROTOCOL("protocol"),
    RECURSIVE("recursive"),
    REF("ref"),
    RELATIVE_TO("relative-to"),
    REPOSITORY("repository"),
    RUNTIME_NAME("runtime-name"),
    SCAN_ENABLED("scan-enabled"),
    SCAN_INTERVAL("scan-interval"),
    SEARCH_CREDENTIAL("search-credential"),
    SEARCH_DN("search-dn"),
    SECURE_PORT("secure-port"),
    SECURITY_REALM("security-realm"),
    SHA1("sha1"),
    SIZE("size"),
    SOCKET_BINDING_GROUP("socket-binding-group"),
    SOCKET_BINDING_REF("socket-binding-ref"),
    SOURCE_INTERFACE("source-interface"),
    SOURCE_NETWORK("source-network"),
    SOURCE_PORT("source-port"),
    TYPE("type"),
    URL("url"),
    USER("user"),
    USER_DN("user-dn"),
    USERNAME("username"),
    USERNAME_ATTRIBUTE("username-attribute"),
    VALUE("value")
    ;

    private final String name;

    Attribute(final String name) {
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
}
