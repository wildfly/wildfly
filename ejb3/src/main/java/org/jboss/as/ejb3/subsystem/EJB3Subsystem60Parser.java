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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CLASS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CLIENT_INTERCEPTORS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.MODULE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVER_INTERCEPTORS;

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

    protected void parseProfile(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String profileName = null;

        final EJBClientContext.Builder builder = new EJBClientContext.Builder();

        final ModelNode operation = Util.createAddOperation();

        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    profileName = value;
                    break;
                case EXCLUDE_LOCAL_RECEIVER:
                    RemotingProfileResourceDefinition.EXCLUDE_LOCAL_RECEIVER.parseAndSetParameter(value, operation, reader);
                    break;
                case LOCAL_RECEIVER_PASS_BY_VALUE:
                    RemotingProfileResourceDefinition.LOCAL_RECEIVER_PASS_BY_VALUE.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (profileName == null) {
            throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.NAME.getLocalName()));
        }


        final PathAddress address = SUBSYSTEM_PATH.append(EJB3SubsystemModel.REMOTING_PROFILE, profileName);
        operation.get(OP_ADDR).set(address.toModelNode());
        operations.add(operation);

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case STATIC_EJB_DISCOVERY:
                    final ModelNode staticEjb = parseStaticEjbDiscoveryType(reader);
                    operation.get(StaticEJBDiscoveryDefinition.STATIC_EJB_DISCOVERY).set(staticEjb);
                    break;
                case REMOTING_EJB_RECEIVER: {
                    parseRemotingReceiver(reader, address, operations);
                    break;
                }
                case REMOTE_HTTP_CONNECTION:
                    parseRemoteHttpConnection(reader, address, operations);
                    break;
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    protected void parseRemoteHttpConnection(final XMLExtendedStreamReader reader, final PathAddress profileAddress,
                                         final List<ModelNode> operations) throws XMLStreamException {

        final ModelNode operation = Util.createAddOperation();

        String name = null;
        final Set<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.URI);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = value;
                    break;
                case URI:
                    RemoteHttpConnectionDefinition.URI.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        final PathAddress receiverAddress = profileAddress.append(EJB3SubsystemModel.REMOTE_HTTP_CONNECTION, name);
        operation.get(OP_ADDR).set(receiverAddress.toModelNode());
        operations.add(operation);

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case CHANNEL_CREATION_OPTIONS:
                    parseChannelCreationOptions(reader, receiverAddress, operations);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }
}
