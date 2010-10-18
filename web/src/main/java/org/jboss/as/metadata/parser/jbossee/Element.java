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

package org.jboss.as.metadata.parser.jbossee;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of all the possible XML elements in the EE schema, by name.
 *
 * @author Remy Maucherat
 */
public enum Element {
    // must be first
    UNKNOWN(null),

    // javaee 6 elements in alpha order
    ADDRESSING("addressing"),

    CLASS_NAME("class-name"),

    DATA_SOURCE("data-source"),
    DATABASE_NAME("database-name"),
    DESCRIPTION("description"),
    DISPLAY_NAME("display-name"),

    EJB_LINK("ejb-link"),
    EJB_LOCAL_REF("ejb-local-ref"),
    EJB_REF("ejb-ref"),
    EJB_REF_NAME("ejb-ref-name"),
    EJB_REF_TYPE("ejb-ref-type"),
    ENABLE_MTOM("enable-mtom"),
    ENABLED("enabled"),
    ENV_ENTRY("env-entry"),
    ENV_ENTRY_NAME("env-entry-name"),
    ENV_ENTRY_TYPE("env-entry-type"),
    ENV_ENTRY_VALUE("env-entry-value"),

    HANDLER("handler"),
    HANDLER_CHAIN("handler-chain"),
    HANDLER_CLASS("handler-class"),
    HANDLER_NAME("handler-name"),
    HOME("home"),

    ICON("icon"),
    IGNORE_DEPENDECY("ignore-dependency"),
    INIT_PARAM("init-param"),
    INITAL_POOL_SIZE("initial-pool-size"),
    INJECTION_TARGET("injection-target"),
    INJECTION_TARGET_CLASS("injection-target-class"),
    INJECTION_TARGET_NAME("injection-target-name"),
    ISOLATION_LEVEL("isolation-level"),

    JAXRPC_MAPPING_FILE("jaxrpc-mapping-file"),
    JNDI_NAME("jndi-name"),

    LARGE_ICON("large-icon"),
    LIFECYCLE_CALLBACK_CLASS("lifecycle-callback-class"),
    LIFECYCLE_CALLBACK_METHOD("lifecycle-callback-method"),
    LOCAL("local"),
    LOCAL_HOME("local-home"),
    LOCAL_JNDI_NAME("local-jndi-name"),
    LOGIN_TIMEOUT("login-timeout"),
    LOOKUP_NAME("lookup-name"),

    MAPPED_NAME("mapped-name"),
    MAX_IDLE_TIME("max-idle-time"),
    MAX_POOL_SIZE("max-pool-size"),
    MAX_STATEMENTS("max-statements"),
    MIN_POOL_SIZE("min-pool-size"),
    MESSAGE_DESTINATION("message-destination"),
    MESSAGE_DESTINATION_LINK("message-destination-link"),
    MESSAGE_DESTINATION_NAME("message-destination-name"),
    MESSAGE_DESTINATION_REF("message-destination-ref"),
    MESSAGE_DESTINATION_REF_NAME("message-destination-ref-name"),
    MESSAGE_DESTINATION_TYPE("message-destination-type"),
    MESSAGE_DESTINATION_USAGE("message-destination-usage"),
    MTOM_THRESHOLD("mtom-threshold"),

    NAME("name"),

    PARAM_NAME("param-name"),
    PARAM_VALUE("param-value"),
    PASSWORD("password"),
    PERSISTENCE_CONTEXT_REF("persistence-context-ref"),
    PERSISTENCE_CONTEXT_REF_NAME("persistence-context-ref-name"),
    PERSISTENCE_CONTEXT_TYPE("persistence-context-type"),
    PERSISTENCE_PROPERTY("persistence-property"),
    PERSISTENCE_UNIT_NAME("persistence-unit-name"),
    PERSISTENCE_UNIT_REF("persistence-unit-ref"),
    PERSISTENCE_UNIT_REF_NAME("persistence-unit-ref-name"),
    PORT_COMPONENT_LINK("port-component-link"),
    PORT_COMPONENT_REF("port-component-ref"),
    PORT_NAME("port-name"),
    PORT_NAME_PATTERN("port-name-pattern"),
    PORT_NUMBER("port-number"),
    POST_CONSTRUCT("post-construct"),
    PRE_DESTROY("pre-destroy"),
    PRINCIPAL_NAME("principal-name"),
    PROPERTY("property"),
    PROTOCOL_BINDINGS("protocol-bindings"),

    REMOTE("remote"),
    REQUIRED("required"),
    RES_AUTH("res-auth"),
    RES_REF_NAME("res-ref-name"),
    RES_SHARING_SCOPE("res-sharing-scope"),
    RES_TYPE("res-type"),
    RES_URL("res-url"),
    RESOURCE_ENV_REF("resource-env-ref"),
    RESOURCE_ENV_REF_NAME("resource-env-ref-name"),
    RESOURCE_ENV_REF_TYPE("resource-env-ref-type"),
    RESOURCE_NAME("resource-name"),
    RESOURCE_REF("resource-ref"),
    RESPECT_BINDING("respect-binding"),
    RESPONSES("responses"),
    ROLE_LINK("role-link"),
    ROLE_NAME("role-name"),

    SERVER_NAME("server-name"),
    SERVICE_ENDPOINT_INTERFACE("service-endpoint-interface"),
    SERVICE_INTERFACE("service-interface"),
    SERVICE_NAME_PATTERN("service-name-pattern"),
    SERVICE_QNAME("service-qname"),
    SERVICE_REF("service-ref"),
    SERVICE_REF_NAME("service-ref-name"),
    SERVICE_REF_TYPE("service-ref-type"),
    SMALL_ICON("small-icon"),
    SOAP_HEADER("soap-header"),
    SOAP_ROLE("soap-role"),

    TRANSACTIONAL("transactional"),

    URL("url"),
    USER("user"),

    VALUE("value"),

    WSDL_FILE("wsdl-file"),
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
