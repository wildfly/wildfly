/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.singleton.deployment.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Paul Ferraro
 */
enum Attribute {
    UNKNOWN(null),

    CACHE_CONTAINER(ModelKeys.CACHE_CONTAINER),
    NAME(ModelKeys.NAME),
    PREFERRED_NODES(ModelKeys.PREFERRED_NODES),
    ;

    private final String localName;

    private Attribute(String localName) {
        this.localName = localName;
    }

    public String getLocalName() {
        return this.localName;
    }

    private static final Map<String, Attribute> attributes = new HashMap<String, Attribute>();

    static {
        for (Attribute attribute: values()) {
            String localName = attribute.getLocalName();
            if (localName != null) {
                attributes.put(localName, attribute);
            }
        }
    }

    public static Attribute forName(String localName) {
        Attribute attribute = attributes.get(localName);
        return (attribute != null) ? attribute : Attribute.UNKNOWN;
    }
}
