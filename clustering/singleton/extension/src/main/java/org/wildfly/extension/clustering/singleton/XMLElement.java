/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
