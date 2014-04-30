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
 * An enumeration of all the recognized core configuration XML element local names, by name.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public enum Element {
    // must be first
    UNKNOWN(null),

    // Domain elements in alpha order
    ACCESS_CONTROL("access-control"),
    ADVANCED_FILTER("advanced-filter"),
    AGENT_LIB("agent-lib"),
    AGENT_PATH("agent-path"),
    ANY("any"),
    ANY_ADDRESS("any-address"),
    ANY_IPV4_ADDRESS("any-ipv4-address"),
    ANY_IPV6_ADDRESS("any-ipv6-address"),
    APPLICATION_CLASSIFICATION("application-classification"),
    APPLICATION_CLASSIFICATIONS("application-classifications"),
    AUDIT_LOG("audit-log"),
    AUTHENTICATION("authentication"),
    AUTHORIZATION("authorization"),

    CACHE("cache"),
    CLIENT_CERT_STORE("client-certificate-store"),
    CLIENT_MAPPING("client-mapping"),
    CONSTRAINTS("constraints"),
    CONTENT("content"),

    DISCOVERY_OPTION("discovery-option"),
    DISCOVERY_OPTIONS("discovery-options"),
    DOMAIN("domain"),
    DOMAIN_CONTROLLER("domain-controller"),
    DEPLOYMENT("deployment"),
    DEPLOYMENTS("deployments"),
    DEPLOYMENT_OVERLAY("deployment-overlay"),
    DEPLOYMENT_OVERLAYS("deployment-overlays"),

    ENVIRONMENT_VARIABLES("environment-variables"),
    EXCLUDE("exclude"),
    EXTENSION("extension"),
    EXTENSIONS("extensions"),

    FILE_HANDLER("file-handler"),
    FORMATTER("formatter"),
    FORMATTERS("formatters"),
    FS_ARCHIVE("fs-archive"),
    FS_EXPLODED("fs-exploded"),

    GROUP("group"),
    GROUP_SEARCH("group-search"),
    GROUP_TO_PRINCIPAL("group-to-principal"),
    GROUPS_FILTER("groups-filter"),


    HANDLER("handler"),
    HANDLERS("handlers"),
    HEAP("heap"),
    HOST("host"),
    HOSTS("hosts"),
    HOST_SCOPED_ROLES("host-scoped-roles"),
    HTTP_INTERFACE("http-interface"),

    IGNORED_RESOURCE("ignored-resources"),
    INCLUDE("include"),
    INSTANCE("instance"),
    INET_ADDRESS("inet-address"),
    INTERFACE("interface"),
    INTERFACE_SPECS("interface-specs"),
    INTERFACES("interfaces"),

    JAAS("jaas"),
    JAVA_AGENT("java-agent"),
    JSON_FORMATTER("json-formatter"),
    JVM("jvm"),
    JVMS("jvms"),
    JVM_OPTIONS("jvm-options"),

    KEYSTORE("keystore"),

    LAUNCH_COMMAND("launch-command"),
    LDAP("ldap"),
    LINK_LOCAL_ADDRESS("link-local-address"),
    LOCAL("local"),
    LOCAL_DESTINATION("local-destination"),
    LOGGER ("logger"),
    LOOPBACK("loopback"),
    LOOPBACK_ADDRESS("loopback-address"),

    MANAGEMENT("management"),
    MANAGEMENT_CLIENT_CONTENT("management-client-content"),
    MANAGEMENT_INTERFACES("management-interfaces"),
    MEMBERSHIP_FILTER("membership-filter"),
    MULTICAST("multicast"),

    NAME("name"),
    NATIVE_INTERFACE("native-interface"),
    NATIVE_REMOTING_INTERFACE("native-remoting-interface"),
    NIC("nic"),
    NIC_MATCH("nic-match"),
    NOT("not"),

    OPTION("option"),
    OUTBOUND_CONNECTIONS("outbound-connections"),
    OUTBOUND_SOCKET_BINDING("outbound-socket-binding"),

    PASSWORD("password"),
    PATH("path"),
    PATHS("paths"),
    PERMGEN("permgen"),
    PLUG_IN("plug-in"),
    PLUG_INS("plug-ins"),
    POINT_TO_POINT("point-to-point"),
    PRINCIPAL_TO_GROUP("principal-to-group"),
    PROFILE("profile"),
    PROFILES("profiles"),
    PROPERTY("property"),
    PROPERTIES("properties"),
    PUBLIC_ADDRESS("public-address"),

    REMOTE("remote"),
    REMOTE_DESTINATION("remote-destination"),
    ROLE("role"),
    ROLE_MAPPING("role-mapping"),
    ROLLOUT_PLANS("rollout-plans"),

    SECRET("secret"),
    SECURITY_REALM("security-realm"),
    SECURITY_REALMS("security-realms"),
    SENSITIVE_CLASSIFICATION("sensitive-classification"),
    SENSITIVE_CLASSIFICATIONS("sensitive-classifications"),
    SERVER("server"),
    SERVER_GROUP_SCOPED_ROLES("server-group-scoped-roles"),
    SERVER_LOGGER("server-logger"),
    SERVER_IDENTITIES("server-identities"),
    SERVERS("servers"),
    SERVER_GROUP("server-group"),
    SERVER_GROUPS("server-groups"),
    SITE_LOCAL_ADDRESS("site-local-address"),
    SOCKET("socket"),
    SOCKET_BINDING("socket-binding"),
    SOCKET_BINDINGS("socket-bindings"),
    SOCKET_BINDING_GROUP("socket-binding-group"),
    SOCKET_BINDING_GROUPS("socket-binding-groups"),
    SSL("ssl"),
    STACK("stack"),
    STATIC_DISCOVERY("static-discovery"),
    SUBNET_MATCH("subnet-match"),
    SUBSYSTEM("subsystem"),
    SYSLOG_HANDLER("syslog-handler"),
    SYSTEM_PROPERTIES("system-properties"),
    TCP("tcp"),
    TLS("tls"),
    TRUSTSTORE("truststore"),
    TYPE("type"),

    UDP("udp"),
    UP("up"),
    USER("user"),
    USERNAME_FILTER("username-filter"),
    USERNAME_IS_DN("username-is-dn"),
    USERNAME_TO_DN("username-to-dn"),
    USERS("users"),

    VARIABLE("variable"),
    VIRTUAL("virtual"),
    VAULT("vault"),
    VAULT_EXPRESSION_SENSITIVITY("vault-expression-sensitivity"),
    VAULT_OPTION("vault-option");

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
