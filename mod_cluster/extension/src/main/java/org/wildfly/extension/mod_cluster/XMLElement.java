/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.mod_cluster;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * Enumeration of XML elements used by {@link ModClusterSubsystemXMLReader} and {@link ModClusterSubsystemXMLWriter}.
 *
 * @author Jean-Frederic Clere
 * @author Radoslav Husar
 */
enum XMLElement {
    UNKNOWN((String) null),

    MOD_CLUSTER_CONFIG("mod-cluster-config"),
    PROXY(ProxyConfigurationResourceDefinition.WILDCARD_PATH),

    SIMPLE_LOAD_PROVIDER("simple-load-provider"),
    DYNAMIC_LOAD_PROVIDER("dynamic-load-provider"),

    CUSTOM_LOAD_METRIC(CustomLoadMetricResourceDefinition.WILDCARD_PATH),
    LOAD_METRIC(LoadMetricResourceDefinition.WILDCARD_PATH),
    PROPERTY(ModelDescriptionConstants.PROPERTY),

    SSL("ssl"),
    ;

    private final String name;

    XMLElement(final String name) {
        this.name = name;
    }

    XMLElement(PathElement path) {
        this.name = path.getKey();
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, XMLElement> MAP;

    static {
        Map<String, XMLElement> map = new HashMap<>();
        for (XMLElement element : values()) {
            final String name = element.getLocalName();
            if (name != null) {
                map.put(name, element);
            }
        }
        MAP = map;
    }

    public static XMLElement forName(String localName) {
        XMLElement element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

}
