/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.model.base.util;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.AbstractModelRootElement;
import org.jboss.as.model.Namespace;
import org.jboss.as.model.NamespacePrefix;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import org.jboss.as.model.QNameComparator;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A fake root element for the domain xsd, equivalent to {@link org.jboss.as.model.DomainModel}.
 *
 * @author Brian Stansberry
 */
public class MockRootElement extends AbstractModelRootElement<MockRootElement> {

    private final NavigableMap<QName, AbstractModelElement<? extends AbstractModelElement<?>>> children =
        new TreeMap<QName, AbstractModelElement<? extends AbstractModelElement<?>>>(QNameComparator.getInstance());
    private final NavigableMap<String, NamespacePrefix> namespaces = new TreeMap<String, NamespacePrefix>();
    private String schemaLocation;

    public static final String ELEMENT_NAME = "mock-root";

    public static String getElementStart(String defaultNamespace, String namespaceLocation, boolean includeMockNamespace) {
        return getElementStart(ELEMENT_NAME, defaultNamespace, namespaceLocation, includeMockNamespace);
    }

    public static String getElementStart(final String rootElementName, String defaultNamespace, String namespaceLocation, boolean includeMockNamespace) {
        StringBuilder sb = new StringBuilder();
        sb.append('<');
        sb.append(rootElementName);
        sb.append(" xmlns=\"");
        sb.append(defaultNamespace);
        sb.append('\"');
        if (includeMockNamespace) {
            sb.append(" xmlns:mock=\"");
            sb.append(MockSubsystemElement.NAMESPACE);
            sb.append('\"');
        }
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"urn:jboss:domain:1.0 jboss_7_0.xsd");
        if (includeMockNamespace) {
            sb.append(' ');
            sb.append(MockSubsystemElement.NAMESPACE);
            sb.append(" jboss-mock-extension.xsd");
        }
        sb.append("\">");
        return sb.toString();
    }

    public static String getElementEnd() {
        return getElementEnd(ELEMENT_NAME);
    }

    public static String getElementEnd(final String rootElementName) {
        return ("</" + rootElementName + ">");
    }

    public static String getXmlContent(String defaultNamespace, String namespaceLocation, boolean includeMockNamespace, String children) {
        StringBuilder sb = new StringBuilder(getElementStart(defaultNamespace, namespaceLocation, includeMockNamespace));
        sb.append(children);
        sb.append(getElementEnd());
        return sb.toString();
    }

    public static String getXmlContent(final String rootElementName, String defaultNamespace, String namespaceLocation, String children) {
        StringBuilder sb = new StringBuilder(getElementStart(rootElementName, defaultNamespace, namespaceLocation, false));
        sb.append(children);
        sb.append(getElementEnd(rootElementName));
        return sb.toString();
    }

    private static final long serialVersionUID = 7973276964332988585L;

    public MockRootElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        final Namespace ns = Namespace.forUri(reader.getNamespaceURI());
        // Handle namespaces
        for (NamespacePrefix namespacePrefix : ParseUtils.readNamespaces(reader)) {
            namespaces.put(namespacePrefix.getPrefix(), namespacePrefix);
        }
        // Handle attributes
        int cnt = reader.getAttributeCount();
        for (int i = 0; i < cnt; i ++) {
            if (reader.getAttributeNamespace(i).equals(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI) && reader.getAttributeLocalName(i).equals("schemaLocation")) {
                schemaLocation = reader.getAttributeValue(i);
                break;
            }
        }
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
           String namespace = reader.getNamespaceURI();
            if (ns == Namespace.forUri(namespace)) {
                String localPart = reader.getLocalName();
                ParseResult<AbstractModelElement<?>> result = new ParseResult<AbstractModelElement<?>>();
                reader.handleAny(result);
                AbstractModelElement<?> child = result.getResult();
                QName qname = new QName(ns.getUriString(), localPart);
                children.put(qname, child);
            }
            else {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    public AbstractModelElement<? extends AbstractModelElement<?>> getChild(String namespace, String localPart) {
        return children.get(new QName(namespace, localPart));
    }

    @Override
    protected Class<MockRootElement> getElementClass() {
        return MockRootElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {

        for (NamespacePrefix namespace : namespaces.values()) {
            streamWriter.setPrefix(namespace.getPrefix(), namespace.getNamespaceURI());
        }

        if (schemaLocation != null) {
            NamespacePrefix ns = namespaces.get("http://www.w3.org/2001/XMLSchema-instance");
            streamWriter.writeAttribute(ns.getPrefix(), ns.getNamespaceURI(), "schemaLocation", schemaLocation);
        }

        if (!children.isEmpty()) {
            for (Map.Entry<QName, AbstractModelElement<? extends AbstractModelElement<?>>> entry : children.entrySet()) {
                QName qname = entry.getKey();
                streamWriter.writeStartElement(qname.getNamespaceURI(), qname.getLocalPart());
                entry.getValue().writeContent(streamWriter);
            }
        }
        streamWriter.writeEndElement();

    }

}
