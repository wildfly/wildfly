package org.jboss.as.ee.structure;

import java.util.HashMap;
import java.util.Map;

public enum EJBClientDescriptorXMLAttribute {

    EXCLUDE_LOCAL_RECEIVER("exclude-local-receiver"),
    LOCAL_RECEIVER_PASS_BY_VALUE("local-receiver-pass-by-value"),
    CONNECT_TIMEOUT("connect-timeout"),
    NAME("name"),
    OUTBOUND_CONNECTION_REF("outbound-connection-ref"),
    VALUE("value"),
    MAX_ALLOWED_CONNECTED_NODES("max-allowed-connected-nodes"),
    CLUSTER_NODE_SELECTOR("cluster-node-selector"),
    USERNAME("username"),
    SECURITY_REALM("security-realm"),
    INVOCATION_TIMEOUT("invocation-timeout"),
    DEPLOYMENT_NODE_SELECTOR("deployment-node-selector"),
    // default unknown attribute
    UNKNOWN(null);

    private final String name;

    EJBClientDescriptorXMLAttribute(final String name) {
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

    private static final Map<String, EJBClientDescriptorXMLAttribute> MAP;

    static {
        final Map<String, EJBClientDescriptorXMLAttribute> map = new HashMap<String, EJBClientDescriptorXMLAttribute>();
        for (EJBClientDescriptorXMLAttribute element : values()) {
            final String name = element.getLocalName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    public static EJBClientDescriptorXMLAttribute forName(String localName) {
        final EJBClientDescriptorXMLAttribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    @Override
    public String toString() {
        return getLocalName();
    }

}
