/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.puparser;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of all the possible XML attributes in the Persistence Unit schema, by name.
 *
 * @author Scott Marlow
 */
public enum Element {
    // always first
    UNKNOWN(null),
    // followed by all entries in sorted order
    CLASS("class"),
    DESCRIPTION("description"),
    EXCLUDEUNLISTEDCLASSES("exclude-unlisted-classes"),
    JARFILE("jar-file"),
    JTADATASOURCE("jta-data-source"),
    MAPPINGFILE("mapping-file"),
    NAME("name"),
    NONJTADATASOURCE("non-jta-data-source"),
    PERSISTENCE("persistence"),
    PERSISTENCEUNIT("persistence-unit"),
    PROPERTIES("properties"),
    PROPERTY("property"),
    PROVIDER("provider"),
    SHAREDCACHEMODE("shared-cache-mode"),
    VALIDATIONMODE("validation-mode"),
    VERSION("version"),
    QUALIFIER("qualifier"),
    SCOPE("scope")
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
