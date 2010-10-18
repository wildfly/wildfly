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

package org.jboss.as.metadata.parser.ee;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.MessageDestinationMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.javaee.spec.ServiceReferenceHandlerMetaData;

/**
 * @author Remy Maucherat
 */
public class ServiceReferenceHandlerMetaDataParser extends MetaDataElementParser {

    public static ServiceReferenceHandlerMetaData parse(XMLStreamReader reader) throws XMLStreamException {
        ServiceReferenceHandlerMetaData handler = new ServiceReferenceHandlerMetaData();

        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                continue;
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ID: {
                    handler.setId(value);
                    break;
                }
                default: throw unexpectedAttribute(reader, i);
            }
        }

        DescriptionGroupMetaData descriptionGroup = new DescriptionGroupMetaData();
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (DescriptionGroupMetaDataParser.parse(reader, descriptionGroup)) {
                if (handler.getDescriptionGroup() == null) {
                    handler.setDescriptionGroup(descriptionGroup);
                }
                continue;
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case HANDLER_NAME:
                    handler.setHandlerName(reader.getElementText());
                    break;
                case HANDLER_CLASS:
                    handler.setHandlerClass(reader.getElementText());
                    break;
                case INIT_PARAM:
                    List<ParamValueMetaData> initParams = handler.getInitParam();
                    if (initParams == null) {
                        initParams = new ArrayList<ParamValueMetaData>();
                        handler.setInitParam(initParams);
                    }
                    initParams.add(ParamValueMetaDataParser.parse(reader));
                    break;
                case SOAP_HEADER:
                    List<QName> soapHeaders = handler.getSoapHeader();
                    if (soapHeaders == null) {
                        soapHeaders = new ArrayList<QName>();
                        handler.setSoapHeader(soapHeaders);
                    }
                    soapHeaders.add(parseQName(reader.getElementText()));
                    break;
                case SOAP_ROLE:
                    List<String> soapRoles = handler.getSoapRole();
                    if (soapRoles == null) {
                        soapRoles = new ArrayList<String>();
                        handler.setSoapRole(soapRoles);
                    }
                    soapRoles.add(reader.getElementText());
                    break;
                case PORT_NAME:
                    List<String> portNames = handler.getPortName();
                    if (portNames == null) {
                        portNames = new ArrayList<String>();
                        handler.setPortName(portNames);
                    }
                    portNames.add(reader.getElementText());
                    break;
                default: throw unexpectedElement(reader);
            }
        }

        return handler;
    }

}
