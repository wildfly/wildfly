/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.REMOTE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVICE;

/**
 * Parser for ejb3:7.0 namespace.
 *
 * @author <a href="mailto:rachmato@redhat.com">Richard Achmatowicz</a>
 */
public class EJB3Subsystem70Parser extends EJB3Subsystem60Parser {

    EJB3Subsystem70Parser() {
    }

    @Override
    protected EJB3SubsystemNamespace getExpectedNamespace() {
        return EJB3SubsystemNamespace.EJB3_7_0;
    }

    @Override
    protected void parseStatefulBean(final XMLExtendedStreamReader reader, final List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> missingRequiredAttributes = EnumSet.of(EJB3SubsystemXMLAttribute.DEFAULT_ACCESS_TIMEOUT, EJB3SubsystemXMLAttribute.CACHE_REF);

        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));

            switch (attribute) {
                case DEFAULT_ACCESS_TIMEOUT: {
                    EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    break;
                }
                case DEFAULT_SESSION_TIMEOUT: {
                    EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    break;
                }
                case CACHE_REF: {
                    EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_CACHE.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    break;
                }
                case CLUSTERED_CACHE_REF: {
                    EJB3SubsystemRootResourceDefinition.DEFAULT_CLUSTERED_SFSB_CACHE.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    break;
                }
                case PASSIVATION_DISABLED_CACHE_REF: {
                    EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
            // found the mandatory attribute
            missingRequiredAttributes.remove(attribute);
        }
        requireNoContent(reader);
        if (!missingRequiredAttributes.isEmpty()) {
            throw missingRequired(reader, missingRequiredAttributes);
        }
    }

    protected void parseRemote(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        final PathAddress ejb3RemoteServiceAddress = SUBSYSTEM_PATH.append(SERVICE, REMOTE);
        ModelNode operation = Util.createAddOperation(ejb3RemoteServiceAddress);
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.CONNECTORS, EJB3SubsystemXMLAttribute.THREAD_POOL_NAME);

        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case CLIENT_MAPPINGS_CLUSTER_NAME:
                    EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_CLUSTER_NAME.parseAndSetParameter(value, operation, reader);
                    break;
                case CONNECTORS:
                    // can't use the obvious: EJB3RemoteResourceDefinition.CONNECTORS.parseAndSetParameter(value, operation, reader);
                    EJB3RemoteResourceDefinition.CONNECTORS.getParser().parseAndSetParameter(EJB3RemoteResourceDefinition.CONNECTORS, value, operation, reader);
                    break;
                case THREAD_POOL_NAME:
                    EJB3RemoteResourceDefinition.THREAD_POOL_NAME.parseAndSetParameter(value, operation, reader);
                    break;
                case EXECUTE_IN_WORKER:
                    EJB3RemoteResourceDefinition.EXECUTE_IN_WORKER.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        // each profile adds it's own operation
        operations.add(operation);

        final Set<EJB3SubsystemXMLElement> parsedElements = new HashSet<EJB3SubsystemXMLElement>();
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            EJB3SubsystemXMLElement element = EJB3SubsystemXMLElement.forName(reader.getLocalName());
            switch (element) {
                case CHANNEL_CREATION_OPTIONS: {
                    if (parsedElements.contains(EJB3SubsystemXMLElement.CHANNEL_CREATION_OPTIONS)) {
                        throw unexpectedElement(reader);
                    }
                    parsedElements.add(EJB3SubsystemXMLElement.CHANNEL_CREATION_OPTIONS);
                    this.parseChannelCreationOptions(reader, ejb3RemoteServiceAddress, operations);
                    break;
                }
                case PROFILES: {
                    parseProfiles(reader, operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }
}
