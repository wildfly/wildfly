/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
    CONTEXT_SERVICES("context-services"),
    CONTEXT_SERVICE("context-service"),
    MANAGED_THREAD_FACTORIES("managed-thread-factories"),
    MANAGED_THREAD_FACTORY("managed-thread-factory"),
    MANAGED_EXECUTOR_SERVICES("managed-executor-services"),
    MANAGED_EXECUTOR_SERVICE("managed-executor-service"),
    MANAGED_SCHEDULED_EXECUTOR_SERVICES("managed-scheduled-executor-services"),
    MANAGED_SCHEDULED_EXECUTOR_SERVICE("managed-scheduled-executor-service"),

    DEFAULT_BINDINGS("default-bindings"),

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
