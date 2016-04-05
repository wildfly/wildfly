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
 * @author Stuart Douglas
 */
enum Namespace {
    // must be first
    UNKNOWN(null, false),

    EE_1_0("urn:jboss:domain:ee:1.0", true),
    EE_1_1("urn:jboss:domain:ee:1.1", true),
    EE_1_2("urn:jboss:domain:ee:1.2", true),
    EE_2_0("urn:jboss:domain:ee:2.0", true),
    EE_3_0("urn:jboss:domain:ee:3.0", false),
    EE_4_0("urn:jboss:domain:ee:4.0", false),
    ;
    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = EE_4_0;

    private final String name;
    private final boolean beanValidationIncluded;

    Namespace(final String name, final boolean beanValidationIncluded) {
        this.name = name;
        this.beanValidationIncluded = beanValidationIncluded;
    }

    /**
     * Get the URI of this namespace.
     *
     * @return the URI
     */
    public String getUriString() {
        return name;
    }

    public boolean isBeanValidationIncluded() {
        return beanValidationIncluded;
    }

    private static final Map<String, Namespace> MAP;

    static {
        final Map<String, Namespace> map = new HashMap<String, Namespace>();
        for (Namespace namespace : values()) {
            final String name = namespace.getUriString();
            if (name != null) map.put(name, namespace);
        }
        MAP = map;
    }

    public static Namespace forUri(String uri) {
        final Namespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }
}
