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

package org.jboss.as.ejb3.resourceadapterbinding.parser;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ejb3.resourceadapterbinding.metadata.EJBBoundResourceAdapterBindingMetaData;
import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;

/**
 * Parser for EJBBoundSecurityMetaData components.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class EJBBoundResourceAdapterBindingMetaDataParser extends AbstractEJBBoundMetaDataParser<EJBBoundResourceAdapterBindingMetaData> {

    @Override
    public EJBBoundResourceAdapterBindingMetaData parse(XMLStreamReader reader) throws XMLStreamException {
        EJBBoundResourceAdapterBindingMetaData metaData = new EJBBoundResourceAdapterBindingMetaData();
        processElements(metaData, reader);
        return metaData;
    }

    @Override
    protected void processElement(EJBBoundResourceAdapterBindingMetaData metaData, XMLStreamReader reader) throws XMLStreamException {
        final String namespaceURI = reader.getNamespaceURI();
        final String localName = reader.getLocalName();
        if ("urn:resource-adapter-binding".equals(namespaceURI)) {
            if ("resource-adapter-name".equals(localName))
                metaData.setResourceAdapterName(getElementText(reader));
            else
                throw unexpectedElement(reader);
        } else {
            super.processElement(metaData, reader);
        }
    }

}
