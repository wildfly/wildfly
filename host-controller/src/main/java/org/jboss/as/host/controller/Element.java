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

package org.jboss.as.host.controller;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of all the recognized XML element local names, by name.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public enum Element {
    // must be first
    UNKNOWN(null),

    // Domain 1.0 elements in alpha order
    AGENT_LIB("agent-lib"),
    AGENT_PATH("agent-path"),
    ANY("any"),
    ANY_ADDRESS("any-address"),
    ANY_IPV4_ADDRESS("any-ipv4-address"),
    ANY_IPV6_ADDRESS("any-ipv6-address"),

    DOMAIN("domain"),
    DOMAIN_CONTROLLER("domain-controller"),
    DEPLOYMENT("deployment"),
    DEPLOYMENTS("deployments"),
    DEPLOYMENT_REPOSITORY("deployment-repository"),

    ENVIRONMENT_VARIABLES("environment-variables"),
    EXTENSION("extension"),
    EXTENSIONS("extensions"),

    HEAP("heap"),
    HOST("host"),

    INCLUDE("include"),
    INET_ADDRESS("inet-address"),
    INTERFACE("interface"),
    INTERFACE_SPECS("interface-specs"),
    INTERFACES("interfaces"),

    JAVA_AGENT("java-agent"),
    JVM("jvm"),
    JVMS("jvms"),
    JVM_OPTIONS("jvm-options"),

    LINK_LOCAL_ADDRESS("link-local-address"),
    LOCAL("local"),
    LOOPBACK("loopback"),

    MANAGEMENT("management"),
    MULTICAST("multicast"),

    NAME("name"),
    NIC("nic"),
    NIC_MATCH("nic-match"),
    NOT("not"),

    OPTION("option"),

    PATH("path"),
    PATHS("paths"),

    POINT_TO_POINT("point-to-point"),
    PERMGEN("permgen"),
    PROFILE("profile"),
    PROFILES("profiles"),
    PROPERTY("property"),
    PUBLIC_ADDRESS("public-address"),

    REMOTE("remote"),

    SCANNING("scanning"),
    SERVER("server"),
    SERVERS("servers"),
    SERVER_GROUP("server-group"),
    SERVER_GROUPS("server-groups"),
    SITE_LOCAL_ADDRESS("site-local-address"),
    SOCKET_BINDING("socket-binding"),
    SOCKET_BINDING_GROUP("socket-binding-group"),
    SOCKET_BINDING_GROUPS("socket-binding-groups"),
    STACK("stack"),
    STANDALONE("standalone"),
    SUBNET_MATCH("subnet-match"),
    SUBSYSTEM("subsystem"),
    SYSTEM_PROPERTIES("system-properties"),

    UP("up"),

    VARIABLE("variable"),
    VIRTUAL("virtual"),
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
