package org.jboss.as.ee.structure;

import java.util.HashMap;
import java.util.Map;

public enum EJBClientDescriptorXMLElement {
    CLIENT_CONTEXT("client-context"),
    NODE("node"),
    EJB_RECEIVERS("ejb-receivers"),
    JBOSS_EJB_CLIENT("jboss-ejb-client"),
    REMOTING_EJB_RECEIVER("remoting-ejb-receiver"),
    CLUSTERS("clusters"),
    CLUSTER("cluster"),
    CHANNEL_CREATION_OPTIONS("channel-creation-options"),
    CONNECTION_CREATION_OPTIONS("connection-creation-options"),
    PROPERTY("property"),
    PROFILE("profile"),
    // default unknown element
    UNKNOWN(null);

    private final String name;

    EJBClientDescriptorXMLElement(final String name) {
        this.name = name;
    }

    public String getLocalName() {
        return name;
    }

    private static final Map<String, EJBClientDescriptorXMLElement> MAP;

    static {
        final Map<String, EJBClientDescriptorXMLElement> map = new HashMap<String, EJBClientDescriptorXMLElement>();
        for (EJBClientDescriptorXMLElement element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static EJBClientDescriptorXMLElement forName(String localName) {
        final EJBClientDescriptorXMLElement element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }
}
