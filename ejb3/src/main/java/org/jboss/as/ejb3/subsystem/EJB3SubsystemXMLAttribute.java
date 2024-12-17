/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jaikiran Pai
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public enum EJB3SubsystemXMLAttribute {
    UNKNOWN(null),

    ALIAS("alias"),
    @Deprecated ALIASES("aliases"),
    ALLOW_EXECUTION("allow-execution"),

    @Deprecated BEAN_CACHE("bean-cache"),
    BEAN_MANAGEMENT("bean-management"),

    @Deprecated CACHE_CONTAINER("cache-container"),
    CACHE_REF("cache-ref"),
    CLIENT_MAPPINGS_CLUSTER_NAME("cluster"),
    @Deprecated CLIENT_MAPPINGS_CACHE("client-mappings-cache"),
    @Deprecated CLUSTERED_CACHE_REF("clustered-cache-ref"),
    CONNECT_TIMEOUT("connect-timeout"),
    @Deprecated CONNECTOR_REF("connector-ref"),
    CONNECTORS("connectors"),
    CORE_THREADS("core-threads"),

    DEFAULT_ACCESS_TIMEOUT("default-access-timeout"),
    DEFAULT_SESSION_TIMEOUT("default-session-timeout"),
    DEFAULT_DATA_STORE("default-data-store"),
    DEFAULT_PERSISTENT_TIMER_MANAGEMENT(EJB3SubsystemModel.DEFAULT_PERSISTENT_TIMER_MANAGEMENT),
    DEFAULT_TRANSIENT_TIMER_MANAGEMENT(EJB3SubsystemModel.DEFAULT_TRANSIENT_TIMER_MANAGEMENT),
    DATABASE("database"),
    DATASOURCE_JNDI_NAME("datasource-jndi-name"),
    DATASOURCE_NAME("datasource-name"),

    ENABLED("enabled"),
    ENABLE_BY_DEFAULT("enable-by-default"),
    EXCLUDE_LOCAL_RECEIVER("exclude-local-receiver"),

    @Deprecated GROUPS_PATH("groups-path"),

    @Deprecated IDLE_TIMEOUT("idle-timeout"),
    @Deprecated IDLE_TIMEOUT_UNIT("idle-timeout-unit"),
    INSTANCE_ACQUISITION_TIMEOUT("instance-acquisition-timeout"),
    INSTANCE_ACQUISITION_TIMEOUT_UNIT("instance-acquisition-timeout-unit"),

    KEEPALIVE_TIME("keepalive-time"),

    LOCAL_RECEIVER_PASS_BY_VALUE("local-receiver-pass-by-value"),

    MAX_POOL_SIZE("max-pool-size"),
    MAX_SIZE("max-size"),
    DERIVE_SIZE("derive-size"),
    MAX_THREADS("max-threads"),

    NAME("name"),

    OUTBOUND_CONNECTION_REF("outbound-connection-ref"),

    PARTITION("partition"),
    REFRESH_INTERVAL("refresh-interval"),
    PASS_BY_VALUE("pass-by-value"),
    @Deprecated PASSIVATE_EVENTS_ON_REPLICATE("passivate-events-on-replicate"),
    PASSIVATION_DISABLED_CACHE_REF("passivation-disabled-cache-ref"),
    @Deprecated PASSIVATION_STORE_REF("passivation-store-ref"),
    PATH("path"),
    POOL_NAME("pool-name"),

    RELATIVE_TO("relative-to"),
    RESOURCE_ADAPTER_NAME("resource-adapter-name"),

    @Deprecated SESSIONS_PATH("sessions-path"),
    STATIC_URLS("static-urls"),
    @Deprecated SUBDIRECTORY_COUNT("subdirectory-count"),

    THREAD_POOL_NAME("thread-pool-name"),
    TYPE("type"),

    USE_QUALIFIED_NAME("use-qualified-name"),

    VALUE("value"),

    ACTIVE("active"),

    EXECUTE_IN_WORKER("execute-in-worker"),

    // Elytron integration
    OUTFLOW_SECURITY_DOMAINS("outflow-security-domains"),
    SECURITY_DOMAIN("security-domain"),
    ENABLE_JACC("enable-jacc"),
    URI("uri"),
    APP_NAME("app-name"),
    MODULE_NAME("module-name"),
    DISTINCT_NAME("distinct-name"),
    LEGACY_COMPLIANT_PRINCIPAL_PROPAGATION("legacy-compliant-principal-propagation"),


    // server interceptors
    MODULE("module"),
    CLASS("class"),
    BINDING("binding")
    ;

    private final String name;

    EJB3SubsystemXMLAttribute(final String name) {
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

    private static final Map<String, EJB3SubsystemXMLAttribute> MAP;

    static {
        final Map<String, EJB3SubsystemXMLAttribute> map = new HashMap<String, EJB3SubsystemXMLAttribute>();
        for (EJB3SubsystemXMLAttribute element : values()) {
            final String name = element.getLocalName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    public static EJB3SubsystemXMLAttribute forName(String localName) {
        final EJB3SubsystemXMLAttribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    @Override
    public String toString() {
        return getLocalName();
    }
}
