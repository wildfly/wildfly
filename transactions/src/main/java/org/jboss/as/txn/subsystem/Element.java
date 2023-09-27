/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of elements used in the transactions subsystem.
 *
 * @author John E. Bailey
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 */
enum Element {
    // must be first
    UNKNOWN(null),

    RECOVERY_ENVIRONMENT(CommonAttributes.RECOVERY_ENVIRONMENT),
    CORE_ENVIRONMENT(CommonAttributes.CORE_ENVIRONMENT),
    COORDINATOR_ENVIRONMENT(CommonAttributes.COORDINATOR_ENVIRONMENT),
    OBJECT_STORE(CommonAttributes.OBJECT_STORE),
    PROCESS_ID(CommonAttributes.PROCESS_ID),
    SOCKET(CommonAttributes.SOCKET),
    UUID(CommonAttributes.UUID),
    JTS(CommonAttributes.JTS),
    USE_HORNETQ_STORE(CommonAttributes.USE_HORNETQ_STORE),
    USE_JOURNAL_STORE(CommonAttributes.USE_JOURNAL_STORE),
    JDBC_STORE(CommonAttributes.JDBC_STORE),
    JDBC_STATE_STORE(CommonAttributes.STATE_STORE),
    JDBC_COMMUNICATION_STORE(CommonAttributes.COMMUNICATION_STORE),
    JDBC_ACTION_STORE(CommonAttributes.ACTION_STORE),
    CM_RESPOURCE(CommonAttributes.CM_RESOURCE),
    CM_RESOURCES(CommonAttributes.CM_RESOURCES),
    CM_TABLE(CommonAttributes.CM_LOCATION),
    CLIENT(CommonAttributes.CLIENT)

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
