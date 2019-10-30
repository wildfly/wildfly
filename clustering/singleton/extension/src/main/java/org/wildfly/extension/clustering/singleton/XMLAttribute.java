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
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Enumeration of XML attributes.
 * @author Paul Ferraro
 */
public enum XMLAttribute {

    CACHE(SingletonPolicyResourceDefinition.Attribute.CACHE),
    CACHE_CONTAINER(SingletonPolicyResourceDefinition.Attribute.CACHE_CONTAINER),
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
