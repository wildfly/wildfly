/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
