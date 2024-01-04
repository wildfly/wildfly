/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.xts;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of elements used in the xts subsystem.
 *
 * @author <a href="mailto:adinn@redhat.com">Andrew Dinn</a>
 */
enum Element {
    // must be first
    UNKNOWN(null),

    HOST(CommonAttributes.HOST),
    XTS_ENVIRONMENT(CommonAttributes.XTS_ENVIRONMENT),
    DEFAULT_CONTEXT_PROPAGATION(CommonAttributes.DEFAULT_CONTEXT_PROPAGATION),
    ASYNC_REGISTRATION(CommonAttributes.ASYNC_REGISTRATION),
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
            if (name != null) { map.put(name, element); }
        }
        MAP = map;
    }

    public static Element forName(String localName) {
        final Element element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

}
