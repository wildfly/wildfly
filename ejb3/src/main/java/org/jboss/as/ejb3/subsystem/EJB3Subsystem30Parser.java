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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DATABASE_DATA_STORE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.REMOTE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVICE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.TIMER_SERVICE;


/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class EJB3Subsystem30Parser extends EJB3Subsystem20Parser {

    protected EJB3Subsystem30Parser() {
    }

    @Override
    protected EJB3SubsystemNamespace getExpectedNamespace() {
        return EJB3SubsystemNamespace.EJB3_3_0;
    }

    @Override
    protected void readElement(final XMLExtendedStreamReader reader, final EJB3SubsystemXMLElement element, final List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        switch (element) {
            case LOG_SYSTEM_EXCEPTIONS: {
                parseLogEjbExceptions(reader, ejb3SubsystemAddOperation);
                break;
            }
            default: {
                super.readElement(reader, element, operations, ejb3SubsystemAddOperation);
            }
        }
    }


    private void parseLogEjbExceptions(XMLExtendedStreamReader reader, ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> missingRequiredAttributes = EnumSet.of(EJB3SubsystemXMLAttribute.VALUE);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case VALUE:
                    EJB3SubsystemRootResourceDefinition.LOG_EJB_EXCEPTIONS.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    // found the mandatory attribute
                    missingRequiredAttributes.remove(EJB3SubsystemXMLAttribute.VALUE);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        if (!missingRequiredAttributes.isEmpty()) {
            throw missingRequired(reader, missingRequiredAttributes);
        }
    }
    @Override
    protected void parseDatabaseDataStore(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        String name = null;

        final ModelNode databaseDataStore = new ModelNode();
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.NAME, EJB3SubsystemXMLAttribute.DATASOURCE_JNDI_NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    if (name != null) {
                        throw unexpectedAttribute(reader, i);
                    }
                    name = reader.getAttributeValue(i);
                    break;
                case DATASOURCE_JNDI_NAME:
                    DatabaseDataStoreResourceDefinition.DATASOURCE_JNDI_NAME.parseAndSetParameter(value, databaseDataStore, reader);
                    break;
                case DATABASE:
                    DatabaseDataStoreResourceDefinition.DATABASE.parseAndSetParameter(value, databaseDataStore, reader);
                    break;
                case PARTITION:
                    DatabaseDataStoreResourceDefinition.PARTITION.parseAndSetParameter(value, databaseDataStore, reader);
                    break;
                case REFRESH_INTERVAL:
                    DatabaseDataStoreResourceDefinition.REFRESH_INTERVAL.parseAndSetParameter(value, databaseDataStore, reader);
                    break;
                case ALLOW_EXECUTION:
                    DatabaseDataStoreResourceDefinition.ALLOW_EXECUTION.parseAndSetParameter(value, databaseDataStore, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);
        address.add(SERVICE, TIMER_SERVICE);
        address.add(DATABASE_DATA_STORE, name);
        databaseDataStore.get(OP).set(ADD);
        databaseDataStore.get(ADDRESS).set(address);
        operations.add(databaseDataStore);
        requireNoContent(reader);
    }

    protected void parseRemote(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        final PathAddress ejb3RemoteServiceAddress = SUBSYSTEM_PATH.append(SERVICE, REMOTE);
        ModelNode operation = Util.createAddOperation(ejb3RemoteServiceAddress);
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.CONNECTOR_REF,
                EJB3SubsystemXMLAttribute.THREAD_POOL_NAME);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case CONNECTOR_REF:
                    EJB3RemoteResourceDefinition.CONNECTOR_REF.parseAndSetParameter(value, operation, reader);
                    break;
                case THREAD_POOL_NAME:
                    EJB3RemoteResourceDefinition.THREAD_POOL_NAME.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        operation.get(EJB3SubsystemModel.EXECUTE_IN_WORKER).set(new ModelNode(false));
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

    protected void parseProfiles(final XMLExtendedStreamReader reader, final List<ModelNode> operations)
            throws XMLStreamException {
        // no attributes expected
        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case PROFILE: {
                    this.parseProfile(reader, operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    protected void parseProfile(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String profileName = null;

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

        final PathAddress address = SUBSYSTEM_PATH.append(EJB3SubsystemModel.REMOTING_PROFILE, profileName);
        operation.get(OP_ADDR).set(address.toModelNode());
        operations.add(operation);

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case REMOTING_EJB_RECEIVER: {
                    parseRemotingReceiver(reader, address, operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (profileName == null) {
            throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.NAME.getLocalName()));
        }
    }

    protected void parseRemotingReceiver(final XMLExtendedStreamReader reader, final PathAddress profileAddress,
            final List<ModelNode> operations) throws XMLStreamException {

        final ModelNode operation = Util.createAddOperation();

        String name = null;
        final Set<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.OUTBOUND_CONNECTION_REF);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = value;
                    break;
                case OUTBOUND_CONNECTION_REF:
                    RemotingEjbReceiverDefinition.OUTBOUND_CONNECTION_REF.parseAndSetParameter(value, operation, reader);
                    break;
                case CONNECT_TIMEOUT:
                    RemotingEjbReceiverDefinition.CONNECT_TIMEOUT.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        final PathAddress receiverAddress = profileAddress.append(EJB3SubsystemModel.REMOTING_EJB_RECEIVER, name);
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
