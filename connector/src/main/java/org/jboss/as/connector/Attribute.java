package org.jboss.as.connector;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * A Attribute.
 *
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 *
 */
public enum Attribute {
    /** alweays the first **/
    UNKNOWN(null),

    /**
     * fail-on-error attribute
     *
     */
    FAIL_ON_ERROR("fail-on-error"),

    /**
     * fail-on-warn attribute
     *
     */
    FAIL_ON_WARN("fail-on-warn");

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
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    public static Attribute forName(String localName) {
        final Attribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    @Override
    public String toString() {
        return getLocalName();
    }
}
