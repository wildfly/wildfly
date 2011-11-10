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

    BEAN_INSTANCE_POOLS("bean-instance-pools"),
    BEAN_INSTANCE_POOL_REF("bean-instance-pool-ref"),

    DATA_STORE("data-store"),

    IIOP("iiop"),

    MDB("mdb"),

    POOLS("pools"),

    CACHE("cache"),
    CACHES("caches"),

    PASSIVATION_STORES("passivation-stores"),
    CLUSTER_PASSIVATION_STORE("cluster-passivation-store"),
    FILE_PASSIVATION_STORE("file-passivation-store"),

    REMOTE("remote"),
    RESOURCE_ADAPTER_NAME("resource-adapter-name"),
    RESOURCE_ADAPTER_REF("resource-adapter-ref"),

    SESSION_BEAN("session-bean"),
    SINGLETON("singleton"),
    STATEFUL("stateful"),
    STATELESS("stateless"),
    STRICT_MAX_POOL("strict-max-pool"),

    THREAD_POOL("thread-pool"),
    THREAD_POOLS("thread-pools"),
    TIMER_SERVICE("timer-service"),

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
