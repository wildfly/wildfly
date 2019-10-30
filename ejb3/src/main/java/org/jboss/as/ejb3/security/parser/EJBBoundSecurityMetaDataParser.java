/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.security.parser;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ejb3.security.metadata.EJBBoundSecurityMetaData;
import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Parser for EJBBoundSecurityMetaData components.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class EJBBoundSecurityMetaDataParser extends AbstractEJBBoundMetaDataParser<EJBBoundSecurityMetaData> {

    public static final String LEGACY_NAMESPACE_URI = "urn:security";
    public static final String NAMESPACE_URI_1_0 = "urn:security:1.0";

    public static final EJBBoundSecurityMetaDataParser INSTANCE = new EJBBoundSecurityMetaDataParser();

    protected EJBBoundSecurityMetaDataParser() {

    }

    @Override
    public EJBBoundSecurityMetaData parse(XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        EJBBoundSecurityMetaData metaData = new EJBBoundSecurityMetaData();
        processElements(metaData, reader, propertyReplacer);
        return metaData;
    }

    @Override
    protected void processElement(EJBBoundSecurityMetaData metaData, XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        if (reader.getNamespaceURI().equals(LEGACY_NAMESPACE_URI) ||
                reader.getNamespaceURI().equals(NAMESPACE_URI_1_0)) {
            final String localName = reader.getLocalName();
            if (localName.equals("security-domain"))
                metaData.setSecurityDomain(getElementText(reader, propertyReplacer));
            else if (localName.equals("run-as-principal"))
                metaData.setRunAsPrincipal(getElementText(reader, propertyReplacer));
            else
                throw unexpectedElement(reader);
        } else {
            super.processElement(metaData, reader, propertyReplacer);
        }
    }

}
