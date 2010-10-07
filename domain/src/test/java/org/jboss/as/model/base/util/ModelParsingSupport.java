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

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.Element;
import org.jboss.as.model.HostModel;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.staxmapper.XMLMapper;

/**
 * Support class for tests of domain model parsing.
 *
 * @author Brian Stansberry
 */
public class ModelParsingSupport {

    public static String getXmlContent(final String rootElementName, final String defaultNamespace,
            final String namespaceLocation, final String children)  {
        StringBuffer sb = new StringBuffer(getElementStart(rootElementName, defaultNamespace, namespaceLocation));
        sb.append(children);
        sb.append(getElementEnd(rootElementName));
        return sb.toString();
    }

    public static String getElementStart(final String rootElementName, String defaultNamespace, String namespaceLocation) {
        StringBuilder sb = new StringBuilder();
        sb.append('<');
        sb.append(rootElementName);
        if (defaultNamespace != null) {
            sb.append(" xmlns=\"");
            sb.append(defaultNamespace);
            sb.append('\"');
            sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            sb.append(" xsi:schemaLocation=\"");
            sb.append(defaultNamespace);
            sb.append(" ");
            sb.append(namespaceLocation);
            sb.append("\">");
        }
        else {
            sb.append('>');
        }
        return sb.toString();
    }

    public static String getElementEnd(final String rootElementName) {
        return ("</" + rootElementName + ">");
    }

    public static String wrap(final String wrapperElement, final String toWrap) {
        StringBuffer sb = new StringBuffer(getElementStart(wrapperElement, null, null));
        sb.append(toWrap);
        sb.append(getElementEnd(wrapperElement));
        return sb.toString();
    }

    public static String wrapServerGroup(final String toWrap) {
        StringBuffer sb = new StringBuffer("<server-group name=\"test\" profile=\"test\">");
        sb.append(toWrap);
        sb.append(getElementEnd("server-group"));
        return wrap(Element.SERVER_GROUPS.getLocalName(), sb.toString());
    }

    public static String wrapJvm(final String toWrap) {
        StringBuffer sb = new StringBuffer("<jvm name=\"test\">");
        sb.append(toWrap);
        sb.append(getElementEnd("jvm"));
        return sb.toString();
    }

    public static DomainModel parseDomainModel(final XMLMapper mapper, final String xmlContent) throws XMLStreamException, FactoryConfigurationError, UpdateFailedException {
        System.out.println("Parsing " + xmlContent);
        Reader reader = new StringReader(xmlContent);
        XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(reader);
        List<AbstractDomainModelUpdate<?>> updates = new ArrayList<AbstractDomainModelUpdate<?>>();
        mapper.parseDocument(updates, xmlReader);
        DomainModel result = new DomainModel();
        for (AbstractDomainModelUpdate<?> update : updates) {
            result.update(update);
        }
        return result;
    }

    public static HostModel parseHostModel(final XMLMapper mapper, final String xmlContent) throws XMLStreamException, UpdateFailedException {
        System.out.println("Parsing " + xmlContent);
        final Reader reader = new StringReader(xmlContent);
        XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(reader);
        final List<AbstractHostModelUpdate<?>> updates = new ArrayList<AbstractHostModelUpdate<?>>();
        mapper.parseDocument(updates, xmlReader);
        final HostModel model = new HostModel();
        for(final AbstractHostModelUpdate<?> update : updates) {
            model.update(update);
        }
        return model;
    }

    public static ServerModel parseServerModel(final XMLMapper mapper, final String xmlContent) throws XMLStreamException, UpdateFailedException {
        System.out.println("Parsing " + xmlContent);
        final Reader reader = new StringReader(xmlContent);
        XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(reader);
        final List<AbstractServerModelUpdate<?>> updates = new ArrayList<AbstractServerModelUpdate<?>>();
        mapper.parseDocument(updates, xmlReader);
        final ServerModel model = new ServerModel();
        for(final AbstractServerModelUpdate<?> update : updates) {
            model.update(update);
        }
        return model;
    }

    private ModelParsingSupport() {}
}
