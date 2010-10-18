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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.servlet.ServletMetaDataParser;
import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.DescriptionsImpl;
import org.jboss.metadata.javaee.spec.EJBReferenceMetaData;
import org.jboss.metadata.javaee.spec.EJBReferenceType;
import org.jboss.metadata.javaee.spec.EnvironmentEntryMetaData;
import org.jboss.metadata.javaee.spec.MessageDestinationMetaData;
import org.jboss.metadata.javaee.spec.PortComponentRef;
import org.jboss.metadata.javaee.spec.ServiceReferenceHandlerChainMetaData;
import org.jboss.metadata.javaee.spec.ServiceReferenceHandlerChainsMetaData;
import org.jboss.metadata.javaee.spec.ServiceReferenceHandlerMetaData;
import org.jboss.metadata.javaee.spec.ServiceReferenceHandlersMetaData;
import org.jboss.metadata.javaee.spec.ServiceReferenceMetaData;
import org.jboss.metadata.web.spec.ServletsMetaData;

/**
 * @author Remy Maucherat
 */
public class ServiceReferenceMetaDataParser extends MetaDataElementParser {

    public static ServiceReferenceMetaData parse(XMLStreamReader reader) throws XMLStreamException {
        ServiceReferenceMetaData serviceReference = new ServiceReferenceMetaData();

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
                    serviceReference.setId(value);
                    break;
                }
                default: throw unexpectedAttribute(reader, i);
            }
        }

        DescriptionGroupMetaData descriptionGroup = new DescriptionGroupMetaData();
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (DescriptionGroupMetaDataParser.parse(reader, descriptionGroup)) {
                if (serviceReference.getDescriptionGroup() == null) {
                    serviceReference.setDescriptionGroup(descriptionGroup);
                }
                continue;
            }
            if (ResourceInjectionMetaDataParser.parse(reader, serviceReference)) {
                continue;
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SERVICE_REF_NAME:
                    serviceReference.setServiceRefName(reader.getElementText());
                    break;
                case SERVICE_INTERFACE:
                    serviceReference.setServiceInterface(reader.getElementText());
                    break;
                case SERVICE_REF_TYPE:
                    serviceReference.setServiceRefType(reader.getElementText());
                    break;
                case WSDL_FILE:
                    serviceReference.setWsdlFile(reader.getElementText());
                    break;
                case JAXRPC_MAPPING_FILE:
                    serviceReference.setJaxrpcMappingFile(reader.getElementText());
                    break;
                case SERVICE_QNAME:
                    serviceReference.setServiceQname(parseQName(reader.getElementText()));
                    break;
                case PORT_COMPONENT_REF:
                    List<PortComponentRef> portComponentRefs = (List<PortComponentRef>) serviceReference.getPortComponentRef();
                    if (portComponentRefs == null) {
                        portComponentRefs = new ArrayList<PortComponentRef>();
                        serviceReference.setPortComponentRef(portComponentRefs);
                    }
                    portComponentRefs.add(PortComponentRefParser.parse(reader));
                    break;
                case HANDLER:
                    ServiceReferenceHandlersMetaData handlers = serviceReference.getHandlers();
                    if (handlers == null) {
                        handlers = new ServiceReferenceHandlersMetaData();
                        serviceReference.setHandlers(handlers);
                    }
                    handlers.add(ServiceReferenceHandlerMetaDataParser.parse(reader));
                    break;
                case HANDLER_CHAIN:
                    ServiceReferenceHandlerChainsMetaData handlerChains = serviceReference.getHandlerChains();
                    if (handlerChains == null) {
                        handlerChains = new ServiceReferenceHandlerChainsMetaData();
                        handlerChains.setHandlers(new ArrayList<ServiceReferenceHandlerChainMetaData>());
                        serviceReference.setHandlerChains(handlerChains);
                    }
                    handlerChains.getHandlers().add(ServiceReferenceHandlerChainMetaDataParser.parse(reader));
                    break;
                default: throw unexpectedElement(reader);
            }
        }

        return serviceReference;
    }

}
