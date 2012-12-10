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
public enum Element {
    UNKNOWN(null),

    DEPLOYMENT_POLICY(ModelKeys.DEPLOYMENT_POLICY),
    ;

    private final String localName;

    private Element(String localName) {
        this.localName = localName;
    }

    public String getLocalName() {
        return this.localName;
    }

    private static final Map<String, Element> elements = new HashMap<String, Element>();

    static {
        for (Element element: values()) {
            String localName = element.getLocalName();
            if (localName != null) {
                elements.put(localName, element);
            }
        }
    }

    public static Element forName(String localName) {
        Element element = elements.get(localName);
        return (element != null) ? element : Element.UNKNOWN;
    }
}
