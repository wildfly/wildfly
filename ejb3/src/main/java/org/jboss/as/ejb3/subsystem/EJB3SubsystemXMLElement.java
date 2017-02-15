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
package org.jboss.as.ejb3.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of elements used in the EJB3 subsystem
 *
 * @author Jaikiran Pai
 */
public enum EJB3SubsystemXMLElement {

    // must be first
    UNKNOWN(null),

    ASYNC("async"),
    ALLOW_EJB_NAME_REGEX("allow-ejb-name-regex"),

    BEAN_INSTANCE_POOLS("bean-instance-pools"),
    BEAN_INSTANCE_POOL_REF("bean-instance-pool-ref"),

    ENTITY_BEAN("entity-bean"),

    DATA_STORE("data-store"),
    DATA_STORES("data-stores"),
    DEFAULT_DISTINCT_NAME("default-distinct-name"),
    DEFAULT_SECURITY_DOMAIN("default-security-domain"),
    DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS(EJB3SubsystemModel.DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS),
    DISABLE_DEFAULT_EJB_PERMISSIONS(EJB3SubsystemModel.DISABLE_DEFAULT_EJB_PERMISSIONS),
    ENABLE_GRACEFUL_TXN_SHUTDOWN(EJB3SubsystemModel.ENABLE_GRACEFUL_TXN_SHUTDOWN),

    FILE_DATA_STORE("file-data-store"),

    IIOP("iiop"),
    IN_VM_REMOTE_INTERFACE_INVOCATION("in-vm-remote-interface-invocation"),

    MDB("mdb"),

    POOLS("pools"),

    CACHE("cache"),
    CACHES("caches"),
    CHANNEL_CREATION_OPTIONS("channel-creation-options"),

    DATABASE_DATA_STORE("database-data-store"),

    OPTIMISTIC_LOCKING("optimistic-locking"),
    OPTION("option"),
    OUTBOUND_CONNECTION_REF("outbound-connection-ref"),

    PASSIVATION_STORE("passivation-store"),
    PASSIVATION_STORES("passivation-stores"),
    PROFILE("profile"),
    PROFILES("profiles"),
    PROPERTY("property"),
    @Deprecated CLUSTER_PASSIVATION_STORE("cluster-passivation-store"),
    @Deprecated FILE_PASSIVATION_STORE("file-passivation-store"),

    REMOTE("remote"),
    REMOTING_EJB_RECEIVER("remoting-ejb-receiver"),
    RESOURCE_ADAPTER_NAME("resource-adapter-name"),
    RESOURCE_ADAPTER_REF("resource-adapter-ref"),

    SESSION_BEAN("session-bean"),
    SINGLETON("singleton"),
    STATEFUL("stateful"),
    STATELESS("stateless"),
    STATISTICS("statistics"),
    STRICT_MAX_POOL("strict-max-pool"),

    GLOBAL_INTERCEPTORS("global-interceptors"),
    CONNECTIONS("connections"),

    THREAD_POOL("thread-pool"),
    THREAD_POOLS("thread-pools"),
    TIMER_SERVICE("timer-service"),
    LOG_SYSTEM_EXCEPTIONS(EJB3SubsystemModel.LOG_SYSTEM_EXCEPTIONS),
    DELIVERY_GROUPS("delivery-groups"),
    DELIVERY_GROUP("delivery-group"),

    // Elytron integration
    APPLICATION_SECURITY_DOMAIN("application-security-domain"),
    APPLICATION_SECURITY_DOMAINS("application-security-domains"),
    IDENTITY("identity"),

    STATIC_EJB_DISCOVERY("static-ejb-discovery"),
    MODULE("module"),
    ;

    private final String name;

    EJB3SubsystemXMLElement(final String name) {
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

    private static final Map<String, EJB3SubsystemXMLElement> MAP;

    static {
        final Map<String, EJB3SubsystemXMLElement> map = new HashMap<String, EJB3SubsystemXMLElement>();
        for (EJB3SubsystemXMLElement element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static EJB3SubsystemXMLElement forName(String localName) {
        final EJB3SubsystemXMLElement element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }
}
