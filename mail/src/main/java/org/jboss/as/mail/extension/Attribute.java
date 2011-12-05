package org.jboss.as.mail.extension;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Tomaz Cerar
 * @created 10.8.11 22:41
 */
public enum Attribute {
    UNKNOWN(null),
    USERNAME(ModelKeys.USERNAME),
    PASSWORD(ModelKeys.PASSWORD),
    JNDI_NAME(ModelKeys.JNDI_NAME),
    DEBUG(ModelKeys.DEBUG),
    OUTBOUND_SOCKET_BINDING_REF(ModelKeys.OUTBOUND_SOCKET_BINDING_REF),
    SSL(ModelKeys.SSL);

    private final String name;

    private Attribute(final String name) {
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

    private static final Map<String, Attribute> attributes;

    static {
        final Map<String, Attribute> map = new HashMap<String, Attribute>();
        for (Attribute attribute : values()) {
            final String name = attribute.getLocalName();
            if (name != null) { map.put(name, attribute); }
        }
        attributes = map;
    }

    public static Attribute forName(String localName) {
        final Attribute attribute = attributes.get(localName);
        return attribute == null ? UNKNOWN : attribute;
    }
}
