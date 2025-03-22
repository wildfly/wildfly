/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Enumeration of XML attributes.
 * @author Paul Ferraro
 */
public enum XMLAttribute {

    CACHE(SingletonPolicyResourceDefinition.CACHE_ATTRIBUTE_GROUP::getCacheAttribute),
    CACHE_CONTAINER(SingletonPolicyResourceDefinition.CACHE_ATTRIBUTE_GROUP::getContainerAttribute),
    DEFAULT(SingletonResourceDefinition.Attribute.DEFAULT),
    NAME(ModelDescriptionConstants.NAME),
    POSITION(SimpleElectionPolicyResourceDefinition.Attribute.POSITION),
    QUORUM(SingletonPolicyResourceDefinition.Attribute.QUORUM),
    ;
    private final String localName;

    XMLAttribute(Attribute attribute) {
        this(attribute.getDefinition().getXmlName());
    }

    XMLAttribute(String localName) {
        this.localName = localName;
    }

    public String getLocalName() {
        return this.localName;
    }

    public String require(XMLExtendedStreamReader reader) throws XMLStreamException {
        String value = reader.getAttributeValue(null, this.localName);
        if (value == null) {
            throw ParseUtils.missingRequired(reader, this.localName);
        }
        return value;
    }

    private static final Map<String, XMLAttribute> map = new HashMap<>();
    static {
        for (XMLAttribute attribute : XMLAttribute.values()) {
            map.put(attribute.getLocalName(), attribute);
        }
    }

    static XMLAttribute forName(XMLExtendedStreamReader reader, int index) throws XMLStreamException {
        XMLAttribute attribute = map.get(reader.getAttributeLocalName(index));
        if (attribute == null) {
            throw ParseUtils.unexpectedAttribute(reader, index);
        }
        return attribute;
    }
}
