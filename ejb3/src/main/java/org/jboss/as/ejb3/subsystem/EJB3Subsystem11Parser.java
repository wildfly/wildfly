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

package org.jboss.as.ejb3.subsystem;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_DATA_STORE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.FILE_DATA_STORE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT_UNIT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.MAX_POOL_SIZE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.PATH;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.RELATIVE_TO;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVICE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.TIMER_SERVICE;

/**
 * User: Jaikiran Pai
 */
public class EJB3Subsystem11Parser implements XMLElementReader<List<ModelNode>> {

    EJB3Subsystem11Parser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {


        final ModelNode ejb3SubsystemAddOperation = new ModelNode();
        ejb3SubsystemAddOperation.get(OP).set(ADD);
        ejb3SubsystemAddOperation.get(OP_ADDR).add(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);

        operations.add(ejb3SubsystemAddOperation);

        // elements
        final EnumSet<EJB3SubsystemXMLElement> encountered = EnumSet.noneOf(EJB3SubsystemXMLElement.class);
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemNamespace.forUri(reader.getNamespaceURI())) {
                case EJB3_1_1: {
                    final EJB3SubsystemXMLElement element = EJB3SubsystemXMLElement.forName(reader.getLocalName());
                    if (!encountered.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    switch (element) {
                        case MDB: {
                            // read <mdb>
                            this.parseMDB(reader, operations, ejb3SubsystemAddOperation);
                            break;
                        }
                        case POOLS: {
                            // read <pools>
                            this.parsePools(reader, operations);
                            break;
                        }
                        case SESSION_BEAN: {
                            // read <session-bean>
                            this.parseSessionBean(reader, operations, ejb3SubsystemAddOperation);
                            break;
                        }
                        case TIMER_SERVICE: {
                            parseTimerService(reader, operations);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private ModelNode parseMDB(final XMLExtendedStreamReader reader, List<ModelNode> operations, ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        ModelNode mdbModelNode = new ModelNode();
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
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        // if the resource-adapter-ref *hasn't* been explicitly specified, then default it to hornetq-ra
        if (!ejb3SubsystemAddOperation.hasDefined(EJB3SubsystemModel.DEFAULT_RESOURCE_ADAPTER_NAME)) {
            final ModelNode defaultRAName = EJB3SubsystemRootResourceDefinition.DEFAULT_RESOURCE_ADAPTER_NAME.getDefaultValue();
            if (defaultRAName != null) {
                ejb3SubsystemAddOperation.get(EJB3SubsystemModel.DEFAULT_RESOURCE_ADAPTER_NAME).set(defaultRAName);
            }
        }

        return mdbModelNode;

    }

    private void parseSessionBean(final XMLExtendedStreamReader reader, final List<ModelNode> operations, ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        // no attributes expected
        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case STATELESS: {
                    this.parseStatelessBean(reader, operations, ejb3SubsystemAddOperation);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseStatelessBean(final XMLExtendedStreamReader reader, final List<ModelNode> operations, ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        // no attributes expected
        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case BEAN_INSTANCE_POOL_REF: {
                    final String poolName = readStringAttributeElement(reader, EJB3SubsystemXMLAttribute.POOL_NAME.getLocalName());
                    EJB3SubsystemRootResourceDefinition.DEFAULT_SLSB_INSTANCE_POOL.parseAndSetParameter(poolName, ejb3SubsystemAddOperation, reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parsePools(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        // no attributes expected
        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case BEAN_INSTANCE_POOLS: {
                    this.parseBeanInstancePools(reader, operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseBeanInstancePools(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        // no attributes expected
        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case STRICT_MAX_POOL: {
                    this.parseStrictMaxPool(reader, operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    protected void parseStrictMaxPool(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String poolName = null;
        Integer maxPoolSize = null;
        Long timeout = null;
        String unit = null;
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    poolName = value;
                    break;
                case MAX_POOL_SIZE:
                    maxPoolSize = StrictMaxPoolResourceDefinition.MAX_POOL_SIZE.parse(value, reader).asInt();
                    break;
                case INSTANCE_ACQUISITION_TIMEOUT:
                    timeout = StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT.parse(value, reader).asLong();
                    break;
                case INSTANCE_ACQUISITION_TIMEOUT_UNIT:
                    unit = StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT_UNIT.parse(value, reader).asString();
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
        operations.add(this.createAddStrictMaxBeanInstancePoolOperation(poolName, maxPoolSize, timeout, unit));
    }

    private void parseTimerService(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        requireNoAttributes(reader);

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);
        address.add(SERVICE, TIMER_SERVICE);
        final ModelNode timerServiceAdd = new ModelNode();
        timerServiceAdd.get(OP).set(ADD);
        timerServiceAdd.get(OP_ADDR).set(address);

        String dataStorePath = null;
        String dataStorePathRelativeTo = null;
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case THREAD_POOL: {
                    final int count = reader.getAttributeCount();
                    for (int i = 0; i < count; i++) {
                        requireNoNamespaceAttribute(reader, i);
                        final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                            case CORE_THREADS:
                                //ignore, no longer supported
                                break;
                            case MAX_THREADS:
                                //ignore, no longer supported
                                break;
                            default:
                                throw unexpectedAttribute(reader, i);
                        }
                    }
                    requireNoContent(reader);
                    break;
                }
                case DATA_STORE: {
                    final int count = reader.getAttributeCount();
                    for (int i = 0; i < count; i++) {
                        requireNoNamespaceAttribute(reader, i);
                        final String value = reader.getAttributeValue(i);
                        final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                            case PATH:
                                if (dataStorePath != null) {
                                    throw unexpectedAttribute(reader, i);
                                }
                                dataStorePath = FileDataStoreResourceDefinition.PATH.parse(value, reader).asString();
                                break;
                            case RELATIVE_TO:
                                if (dataStorePathRelativeTo != null) {
                                    throw unexpectedAttribute(reader, i);
                                }
                                dataStorePathRelativeTo = FileDataStoreResourceDefinition.RELATIVE_TO.parse(value, reader).asString();
                                break;
                            default:
                                throw unexpectedAttribute(reader, i);
                        }
                    }
                    if (dataStorePath == null) {
                        throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.PATH));
                    }
                    timerServiceAdd.get(DEFAULT_DATA_STORE).set("default-file-store");
                    final ModelNode fileDataStoreAdd = new ModelNode();
                    final ModelNode fileDataAddress = address.clone();
                    fileDataAddress.add(FILE_DATA_STORE, "default-file-store");
                    fileDataStoreAdd.get(OP).set(ADD);
                    fileDataStoreAdd.get(OP_ADDR).set(fileDataAddress);
                    fileDataStoreAdd.get(PATH).set(dataStorePath);
                    if (dataStorePathRelativeTo != null) {
                        fileDataStoreAdd.get(RELATIVE_TO).set(dataStorePathRelativeTo);
                    }
                    requireNoContent(reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        operations.add(timerServiceAdd);
    }

    private ModelNode createAddStrictMaxBeanInstancePoolOperation(final String name, final Integer maxPoolSize, final Long timeout, final String timeoutUnit) {
        // create /subsystem=ejb3/strict-max-bean-instance-pool=name:add(...)
        final ModelNode addStrictMaxPoolOperation = new ModelNode();
        addStrictMaxPoolOperation.get(OP).set(ADD);
        // set the address for this operation
        final PathAddress address = this.getEJB3SubsystemAddress().append(PathElement.pathElement(STRICT_MAX_BEAN_INSTANCE_POOL, name));
        addStrictMaxPoolOperation.get(OP_ADDR).set(address.toModelNode());
        // set the params for the operation
        if (maxPoolSize != null) {
            addStrictMaxPoolOperation.get(MAX_POOL_SIZE).set(maxPoolSize);
        }
        if (timeout != null) {
            addStrictMaxPoolOperation.get(INSTANCE_ACQUISITION_TIMEOUT).set(timeout);
        }
        if (timeoutUnit != null) {
            addStrictMaxPoolOperation.get(INSTANCE_ACQUISITION_TIMEOUT_UNIT).set(timeoutUnit);
        }

        return addStrictMaxPoolOperation;
    }

    private PathAddress getEJB3SubsystemAddress() {
       return EJB3Subsystem12Parser.SUBSYSTEM_PATH;
    }
}
