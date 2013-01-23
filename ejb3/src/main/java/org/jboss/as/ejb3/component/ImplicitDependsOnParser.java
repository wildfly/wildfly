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

package org.jboss.as.ejb3.component;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Responsible for parsing the <code>implicit-depends-on</code> in jboss-ejb3.xml
 *
 * If this is set then the EJB's views have a dependency on the EJB's start service,
 * so other components that inject the EJB will end up with an implicit dependency on the EJB
 *
 * @author Stuart Douglas
 */
public class ImplicitDependsOnParser extends AbstractEJBBoundMetaDataParser<ImplicitDependsOnMetaData> {

    public static final ImplicitDependsOnParser INSTANCE = new ImplicitDependsOnParser();
    public static final String NAMESPACE_URI_1_0 = "urn:implicit-depends-on:1.0";

    private static final String ELEMENT_IMPLICIT_DEPENDS_ON = "implicit-depends-on";
    private static final String ELEMENT_ENABLED = "enabled";

    @Override
    public ImplicitDependsOnMetaData parse(XMLStreamReader reader, PropertyReplacer propertyReplacer) throws XMLStreamException {
        // make sure it's the right namespace
        if (!reader.getNamespaceURI().equals(NAMESPACE_URI_1_0)) {
            throw unexpectedElement(reader);
        }
        // only process relevant elements
        if (!reader.getLocalName().equals(ELEMENT_IMPLICIT_DEPENDS_ON)) {
            throw unexpectedElement(reader);
        }

        final ImplicitDependsOnMetaData metaData = new ImplicitDependsOnMetaData();
        processElements(metaData, reader, propertyReplacer);
        return metaData;
    }

    @Override
    protected void processElement(final ImplicitDependsOnMetaData metaData, final XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        final String localName = reader.getLocalName();
        if (!NAMESPACE_URI_1_0.equals(reader.getNamespaceURI())) {
            super.processElement(metaData, reader, propertyReplacer);
            return;
        }
        if (!localName.equals(ELEMENT_ENABLED)) {
            throw unexpectedElement(reader);
        }
        metaData.setEnabled(Boolean.parseBoolean(reader.getElementText()));
    }

}
