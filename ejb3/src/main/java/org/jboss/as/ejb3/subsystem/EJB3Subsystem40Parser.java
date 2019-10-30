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
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DERIVE_SIZE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.MAX_POOL_SIZE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.REMOTE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVICE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL;

/**
 * Parser for ejb3:4.0 namespace.
 *
 * @author <a href="mailto:rachmato@redhat.com">Richard Achmatowicz</a>
 */
public class EJB3Subsystem40Parser extends EJB3Subsystem30Parser {

    protected EJB3Subsystem40Parser() {
    }

    @Override
    protected EJB3SubsystemNamespace getExpectedNamespace() {
        return EJB3SubsystemNamespace.EJB3_4_0;
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
                case CLIENT_MAPPINGS_CLUSTER_NAME:
                    EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_CLUSTER_NAME.parseAndSetParameter(value, operation, reader);
                    break;
                case CONNECTOR_REF:
                    EJB3RemoteResourceDefinition.CONNECTOR_REF.parseAndSetParameter(value, operation, reader);
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

    @Override
    protected void parseMDB(final XMLExtendedStreamReader reader, List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        // no attributes expected
        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case BEAN_INSTANCE_POOL_REF: {
                    final String poolName = readStringAttributeElement(reader, EJB3SubsystemXMLAttribute.POOL_NAME.getLocalName());
                    EJB3SubsystemRootResourceDefinition.DEFAULT_MDB_INSTANCE_POOL.parseAndSetParameter(poolName, ejb3SubsystemAddOperation, reader);
                    break;
                }
                case RESOURCE_ADAPTER_REF: {
                    final String resourceAdapterName = readStringAttributeElement(reader, EJB3SubsystemXMLAttribute.RESOURCE_ADAPTER_NAME.getLocalName());
                    EJB3SubsystemRootResourceDefinition.DEFAULT_RESOURCE_ADAPTER_NAME.parseAndSetParameter(resourceAdapterName, ejb3SubsystemAddOperation, reader);
                    break;
                }
                case DELIVERY_GROUPS: {
                    parseDeliveryGroups(reader, operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseDeliveryGroups(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        // no attributes expected
        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case DELIVERY_GROUP: {
                    final int count = reader.getAttributeCount();
                    String groupName = null;
                    final ModelNode operation = Util.createAddOperation();
                    for (int i = 0; i < count; i++) {
                        requireNoNamespaceAttribute(reader, i);
                        final String value = reader.getAttributeValue(i);
                        final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                            case NAME:
                                groupName = value;
                                break;
                            case ACTIVE:
                                MdbDeliveryGroupResourceDefinition.ACTIVE.parseAndSetParameter(reader.getAttributeValue(i), operation, reader);
                                break;
                            default:
                                throw unexpectedAttribute(reader, i);
                        }
                    }
                    requireNoContent(reader);
                    if (groupName == null) {
                        throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.NAME.getLocalName()));
                    }
                    // create and add the operation
                    // create /subsystem=ejb3/mdb-delivery-group=name:add(...)
                    final PathAddress address = SUBSYSTEM_PATH.append(EJB3SubsystemModel.MDB_DELIVERY_GROUP, groupName);
                    operation.get(OP_ADDR).set(address.toModelNode());
                    operations.add(operation);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    void parseStrictMaxPool(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String poolName = null;
        final ModelNode operation = Util.createAddOperation();
        boolean sizeAttribute = false;
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    poolName = value;
                    break;
                case MAX_POOL_SIZE:
                    if (sizeAttribute) {
                        throw mutuallyExclusiveAttributes(reader);
                    }
                    sizeAttribute = true;
                    StrictMaxPoolResourceDefinition.MAX_POOL_SIZE.parseAndSetParameter(value, operation, reader);
                    break;
                case DERIVE_SIZE:
                    if (sizeAttribute) {
                        throw mutuallyExclusiveAttributes(reader);
                    }
                    sizeAttribute = true;
                    StrictMaxPoolResourceDefinition.DERIVE_SIZE.parseAndSetParameter(value, operation, reader);
                    break;
                case INSTANCE_ACQUISITION_TIMEOUT:
                    StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT.parseAndSetParameter(value, operation, reader);
                    break;
                case INSTANCE_ACQUISITION_TIMEOUT_UNIT:
                    StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT_UNIT.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        if (poolName == null) {
            throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.NAME.getLocalName()));
        }
        // create and add the operation
        // create /subsystem=ejb3/strict-max-bean-instance-pool=name:add(...)
        final PathAddress address = this.getEJB3SubsystemAddress().append(STRICT_MAX_BEAN_INSTANCE_POOL, poolName);
        operation.get(OP_ADDR).set(address.toModelNode());
        operations.add(operation);
    }

    private XMLStreamException mutuallyExclusiveAttributes(XMLExtendedStreamReader reader) {
        return EjbLogger.ROOT_LOGGER.mutuallyExclusiveAttributes(reader.getLocation(), MAX_POOL_SIZE, DERIVE_SIZE);
    }

}
