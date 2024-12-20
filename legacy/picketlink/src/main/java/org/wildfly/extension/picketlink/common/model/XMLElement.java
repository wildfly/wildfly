/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.common.model;

import java.util.HashMap;
import java.util.Map;

/**
 * <p> XML elements used in the schema. This elements are not related with the subsystem's model. Usually they are used to group model elements.
 * </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 8, 2012
 */
public enum XMLElement {

    RELATIONSHIPS("relationships"),
    LDAP_MAPPINGS("mappings"),
    IDENTITY_STORE_CREDENTIAL_HANDLERS("credential-handlers"),
    TRUST("trust"),
    KEYS("keys"),
    SERVICE_PROVIDERS("service-providers"),
    HANDLERS("handlers");

    private static final Map<String, XMLElement> xmlElements = new HashMap<String, XMLElement>();

    static {
        for (XMLElement element : values()) {
            xmlElements.put(element.getName(), element);
        }
    }

    private final String name;

    private XMLElement(String name) {
        this.name = name;
    }

    public static XMLElement forName(String name) {
        return xmlElements.get(name);
    }

    public String getName() {
        return this.name;
    }
}
