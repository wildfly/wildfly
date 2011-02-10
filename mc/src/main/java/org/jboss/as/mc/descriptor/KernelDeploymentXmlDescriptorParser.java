/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.mc.descriptor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.mc.ParseResult;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parse Microcontainer jboss-beans.xml.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class KernelDeploymentXmlDescriptorParser implements XMLElementReader<ParseResult<KernelDeploymentXmlDescriptor>>, XMLStreamConstants {
    public static final String NAMESPACE = "urn:jboss:mc:7.0";

    private enum Element {
        BEAN(new QName(NAMESPACE, "bean")),
        CONSTRUCTOR(new QName(NAMESPACE, "constructor")),
        PROPERTY(new QName(NAMESPACE, "property")),
        INJECT(new QName(NAMESPACE, "inject")),
        VALUE_FACTORY(new QName(NAMESPACE, "value-factory")),
        PARAMETER(new QName(NAMESPACE, "parameter")),
        DEPENDS(new QName(NAMESPACE, "depends")),
        ALIAS(new QName(NAMESPACE, "alias")),
        ANNOTATION(new QName(NAMESPACE, "annotation")),
        UNKNOWN(null);

        private final QName qName;

        private static final Map<QName, Element> QNAME_MAP = new HashMap<QName, Element>();

        static {
            for(Element element : Element.values()) {
                QNAME_MAP.put(element.qName, element);
            }
        }

        private Element(final QName qName) {
            this.qName = qName;
        }

        static Element of(QName qName) {
            final Element element = QNAME_MAP.get(qName);
            return element == null ? UNKNOWN : element;
        }
    }

    private enum Attribute {
        MODE(new QName("mode")),
        NAME(new QName("name")),
        TYPE(new QName("type")),
        VALUE(new QName("value")),
        TRIM(new QName("trim")),
        REPLACE(new QName("replace")),
        BEAN(new QName("bean")),
        PROPERTY(new QName("property")),
        CLASS(new QName("class")),
        METHOD(new QName("method")),
        UNKNOWN(null);

        private final QName qName;

        private static final Map<QName, Attribute> QNAME_MAP = new HashMap<QName, Attribute>();

        static {
            for(Attribute attribute : Attribute.values()) {
                QNAME_MAP.put(attribute.qName, attribute);
            }
        }

        private Attribute(final QName qName) {
            this.qName = qName;
        }

        static Attribute of(QName qName) {
            final Attribute attribute = QNAME_MAP.get(qName);
            return attribute == null ? UNKNOWN : attribute;
        }
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, ParseResult<KernelDeploymentXmlDescriptor> value) throws XMLStreamException {
        final KernelDeploymentXmlDescriptor kernelDeploymentXmlDescriptor = new KernelDeploymentXmlDescriptor();
        final List<BeanMetaDataConfig> beansConfigs = new ArrayList<BeanMetaDataConfig>();
        kernelDeploymentXmlDescriptor.setBeans(beansConfigs);
        value.setResult(kernelDeploymentXmlDescriptor);

        final int count = reader.getAttributeCount();
        for(int i = 0; i < count; i++) {
            final QName attributeName = reader.getAttributeName(i);
            final Attribute attribute = Attribute.of(attributeName);
            final String attributeValue = reader.getAttributeValue(i);
            switch(attribute) {
                case MODE:
                    kernelDeploymentXmlDescriptor.setControllerMode(KernelDeploymentXmlDescriptor.ControllerMode.of(attributeValue));
                    break;
            }
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case COMMENT:
                    break;
                case END_ELEMENT:
                    return;
                case START_ELEMENT:
                    switch(Element.of(reader.getName())) {
                        case BEAN:
                            beansConfigs.add(parseBean(reader));
                            break;
                        case UNKNOWN:
                            throw unexpectedContent(reader);
                    }
                    break;
            }
        }
    }

    private BeanMetaDataConfig parseBean(final XMLExtendedStreamReader reader) throws XMLStreamException  {
        BeanMetaDataConfig beanConfig = new BeanMetaDataConfig();

        final int count = reader.getAttributeCount();
        final Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.CLASS);
        for(int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);

            switch(attribute) {
                case NAME:
                    beanConfig.setName(attributeValue);
                    break;
                case CLASS:
                    beanConfig.setBeanClass(attributeValue);
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if(required.isEmpty() == false) {
            throw missingAttributes(reader.getLocation(), required);
        }

        return beanConfig;
    }

    private static XMLStreamException unexpectedContent(final XMLStreamReader reader) {
        final String kind;
        switch (reader.getEventType()) {
            case XMLStreamConstants.ATTRIBUTE: kind = "attribute"; break;
            case XMLStreamConstants.CDATA: kind = "cdata"; break;
            case XMLStreamConstants.CHARACTERS: kind = "characters"; break;
            case XMLStreamConstants.COMMENT: kind = "comment"; break;
            case XMLStreamConstants.DTD: kind = "dtd"; break;
            case XMLStreamConstants.END_DOCUMENT: kind = "document end"; break;
            case XMLStreamConstants.END_ELEMENT: kind = "element end"; break;
            case XMLStreamConstants.ENTITY_DECLARATION: kind = "entity decl"; break;
            case XMLStreamConstants.ENTITY_REFERENCE: kind = "entity ref"; break;
            case XMLStreamConstants.NAMESPACE: kind = "namespace"; break;
            case XMLStreamConstants.NOTATION_DECLARATION: kind = "notation decl"; break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION: kind = "processing instruction"; break;
            case XMLStreamConstants.SPACE: kind = "whitespace"; break;
            case XMLStreamConstants.START_DOCUMENT: kind = "document start"; break;
            case XMLStreamConstants.START_ELEMENT: kind = "element start"; break;
            default: kind = "unknown"; break;
        }
        final StringBuilder b = new StringBuilder("Unexpected content of type '").append(kind).append('\'');
        if (reader.hasName()) {
            b.append(" named '").append(reader.getName()).append('\'');
        }
        if (reader.hasText()) {
            b.append(", text is: '").append(reader.getText()).append('\'');
        }
        return new XMLStreamException(b.toString(), reader.getLocation());
    }

    private static XMLStreamException missingAttributes(final Location location, final Set<Attribute> required) {
        final StringBuilder b = new StringBuilder("Missing one or more required attributes:");
        for (Attribute attribute : required) {
            b.append(' ').append(attribute);
        }
        return new XMLStreamException(b.toString(), location);
    }
}