package org.jboss.as.remoting;

import java.util.HashMap;
import java.util.Map;

import org.jboss.dmr.ModelNode;

/**
 * Protocols that can be used for a remoting connection
 *
 * @author Stuart Douglas
 */
public enum Protocol {

    REMOTE("remote"),
    HTTP_REMOTING("http-remoting"),
    HTTPS_REMOTING("https-remoting");

    private static final Map<String, Protocol> MAP;

    static {
        final Map<String, Protocol> map = new HashMap<String, Protocol>();
        for (Protocol value : values()) {
            map.put(value.localName, value);
        }
        MAP = map;
    }

    public static Protocol forName(String localName) {
        final Protocol value = localName != null ? MAP.get(localName.toLowerCase()) : null;
        return value == null ? Protocol.valueOf(localName.toUpperCase()) : value;
    }

    private final String localName;

    Protocol(final String localName) {
        this.localName = localName;
    }

    @Override
    public String toString() {
        return localName;
    }

    public ModelNode toModelNode() {
        return new ModelNode().set(toString());
    }
}
