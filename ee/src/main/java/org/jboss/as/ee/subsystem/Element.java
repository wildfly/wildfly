/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of elements used in the EE subsystem
 *
 * @author Stuart Douglas
 */
enum Element {

    GLOBAL_MODULES(GlobalModulesDefinition.GLOBAL_MODULES),
    MODULE("module"),

    EAR_SUBDEPLOYMENTS_ISOLATED(EeSubsystemRootResource.EAR_SUBDEPLOYMENTS_ISOLATED.getXmlName()),

    SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT(EeSubsystemRootResource.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT.getXmlName()),

    JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT(EeSubsystemRootResource.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT.getXmlName()),

    ANNOTATION_PROPERTY_REPLACEMENT(EeSubsystemRootResource.ANNOTATION_PROPERTY_REPLACEMENT.getXmlName()),

    CONCURRENT("concurrent"),

    DEFAULT_BINDINGS("default-bindings"),

    GLOBAL_DIRECTORIES("global-directories"),
    DIRECTORY("directory"),

    UNKNOWN(null);

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
