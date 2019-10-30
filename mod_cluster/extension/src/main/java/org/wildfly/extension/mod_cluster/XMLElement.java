/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

    @Deprecated MOD_CLUSTER_CONFIG(ProxyConfigurationResourceDefinition.LEGACY_PATH),
    PROXY(ProxyConfigurationResourceDefinition.WILDCARD_PATH),

    SIMPLE_LOAD_PROVIDER(ProxyConfigurationResourceDefinition.DeprecatedAttribute.SIMPLE_LOAD_PROVIDER.getName()),
    DYNAMIC_LOAD_PROVIDER(DynamicLoadProviderResourceDefinition.LEGACY_PATH),

    CUSTOM_LOAD_METRIC(CustomLoadMetricResourceDefinition.WILDCARD_PATH),
    LOAD_METRIC(LoadMetricResourceDefinition.WILDCARD_PATH),
    PROPERTY(ModelDescriptionConstants.PROPERTY),

    @Deprecated SSL(SSLResourceDefinition.PATH),
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
