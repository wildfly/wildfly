/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.singleton;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Enumeration of XML elements.
 * @author Paul Ferraro
 */
public enum XMLElement {

    SINGLETON_POLICIES("singleton-policies"),
    SINGLETON_POLICY(SingletonPolicyResourceDefinition.WILDCARD_PATH),
    NAME_PREFERENCES(ElectionPolicyResourceDefinition.Attribute.NAME_PREFERENCES),
    RANDOM_ELECTION_POLICY("random-election-policy"),
    SIMPLE_ELECTION_POLICY("simple-election-policy"),
    SOCKET_BINDING_PREFERENCES(ElectionPolicyResourceDefinition.Attribute.SOCKET_BINDING_PREFERENCES),
    ;
    private final String localName;

    XMLElement(PathElement path) {
        this(path.getKey());
    }

    XMLElement(Attribute attribute) {
        this(attribute.getDefinition().getXmlName());
    }

    XMLElement(String localName) {
        this.localName = localName;
    }

    public String getLocalName() {
        return this.localName;
    }

    private static final Map<String, XMLElement> map = new HashMap<>();
    static {
        for (XMLElement element : XMLElement.values()) {
            map.put(element.getLocalName(), element);
        }
    }

    static XMLElement forName(XMLExtendedStreamReader reader) throws XMLStreamException {
        XMLElement element = map.get(reader.getLocalName());
        if (element == null) {
            throw ParseUtils.unexpectedElement(reader);
        }
        return element;
    }
}
