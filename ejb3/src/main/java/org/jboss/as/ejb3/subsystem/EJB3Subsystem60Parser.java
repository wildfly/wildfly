/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CLASS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CLIENT_INTERCEPTORS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.MODULE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVER_INTERCEPTORS;

import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parser for ejb3:6.0 namespace.
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class EJB3Subsystem60Parser extends EJB3Subsystem50Parser {

    EJB3Subsystem60Parser() {
    }

    @Override
    protected EJB3SubsystemNamespace getExpectedNamespace() {
        return EJB3SubsystemNamespace.EJB3_6_0;
    }

    @Override
    protected void readElement(final XMLExtendedStreamReader reader, final EJB3SubsystemXMLElement element, final List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        switch (element) {
            case SERVER_INTERCEPTORS: {
                parseServerInterceptors(reader, ejb3SubsystemAddOperation);
                break;
            }
            case CLIENT_INTERCEPTORS: {
                parseClientInterceptors(reader, ejb3SubsystemAddOperation);
                break;
            }
            default: {
                super.readElement(reader, element, operations, ejb3SubsystemAddOperation);
            }
        }
    }

    protected void parseServerInterceptors(final XMLExtendedStreamReader reader, final ModelNode ejbSubsystemAddOperation) throws XMLStreamException {
        final ModelNode interceptors = new ModelNode();

        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case INTERCEPTOR: {
                    parseInterceptor(reader, interceptors);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        ejbSubsystemAddOperation.get(SERVER_INTERCEPTORS).set(interceptors);
    }

    protected void parseClientInterceptors(final XMLExtendedStreamReader reader, final ModelNode ejbSubsystemAddOperation) throws XMLStreamException {
        final ModelNode interceptors = new ModelNode();

        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case INTERCEPTOR: {
                    parseInterceptor(reader, interceptors);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        ejbSubsystemAddOperation.get(CLIENT_INTERCEPTORS).set(interceptors);
    }


    protected void parseInterceptor(final XMLExtendedStreamReader reader, final ModelNode interceptors) throws XMLStreamException {
        final ModelNode interceptor = new ModelNode();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            switch (EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i))) {
                case MODULE: {
                    interceptor.get(MODULE).set(value);
                    break;
                }
                case CLASS: {
                    interceptor.get(CLASS).set(value);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoContent(reader);

        interceptors.add(interceptor);
    }
}
