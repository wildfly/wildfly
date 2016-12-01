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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.threads.Namespace;
import org.jboss.as.threads.ThreadsParser;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

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
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.ASYNC;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CLUSTER_PASSIVATION_STORE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_DATA_STORE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.FILE_DATA_STORE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.FILE_PASSIVATION_STORE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.IIOP;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.PATH;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.RELATIVE_TO;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.REMOTE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVICE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.THREAD_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.TIMER_SERVICE;

/**
 * @author Jaikiran Pai
 */
public class EJB3Subsystem12Parser implements XMLElementReader<List<ModelNode>> {

    protected static final PathAddress SUBSYSTEM_PATH = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME));

    protected EJB3Subsystem12Parser() {
    }


    protected void readAttributes(final XMLExtendedStreamReader reader) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            throw ParseUtils.unexpectedAttribute(reader, i);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        final ModelNode ejb3SubsystemAddOperation = Util.createAddOperation(SUBSYSTEM_PATH);
        operations.add(ejb3SubsystemAddOperation);
        readAttributes(reader);
        // elements
        final EnumSet<EJB3SubsystemXMLElement> encountered = EnumSet.noneOf(EJB3SubsystemXMLElement.class);
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (EJB3SubsystemNamespace.forUri(reader.getNamespaceURI()) != getExpectedNamespace()) {
                throw unexpectedElement(reader);
            }
            final EJB3SubsystemXMLElement element = EJB3SubsystemXMLElement.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            readElement(reader, element, operations, ejb3SubsystemAddOperation);
        }
    }

    protected void readElement(final XMLExtendedStreamReader reader, final EJB3SubsystemXMLElement element, final List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        switch (element) {
            case CACHES: {
                this.parseCaches(reader, operations);
                break;
            }
            case PASSIVATION_STORES: {
                this.parsePassivationStores(reader, operations);
                break;
            }
            case MDB: {
                // read <mdb>
                this.parseMDB(reader, operations, ejb3SubsystemAddOperation);
                break;
            }
            case ENTITY_BEAN: {
                // read <entity-bean>
                this.parseEntityBean(reader, operations, ejb3SubsystemAddOperation);
                break;
            }
            case POOLS: {
                // read <pools>
                this.parsePools(reader, operations);
                break;
            }
            case REMOTE: {
                // read <remote>
                parseRemote(reader, operations);
                break;
            }
            case ASYNC: {
                // read <remote>
                parseAsync(reader, operations);
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
            case THREAD_POOLS: {
                parseThreadPools(reader, operations);
                break;
            }
            case IIOP: {
                parseIIOP(reader, operations);
                break;
            }
            case IN_VM_REMOTE_INTERFACE_INVOCATION:
                parseInVMRemoteInterfaceInvocation(reader, ejb3SubsystemAddOperation);
                break;
            default: {
                throw unexpectedElement(reader);
            }
        }
    }


    protected void parseRemote(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        ModelNode operation = Util.createAddOperation(SUBSYSTEM_PATH.append(SERVICE, REMOTE));
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.CONNECTOR_REF, EJB3SubsystemXMLAttribute.THREAD_POOL_NAME);
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
        requireNoContent(reader);
        operations.add(operation);
    }

    private void parseAsync(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        //String threadPoolName = null;
        ModelNode operation = Util.createAddOperation(SUBSYSTEM_PATH.append(SERVICE, ASYNC));
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.THREAD_POOL_NAME);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case THREAD_POOL_NAME:
                    EJB3AsyncResourceDefinition.THREAD_POOL_NAME.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        requireNoContent(reader);
        operations.add(operation);
    }

    private void parseIIOP(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        ModelNode operation = Util.createAddOperation(SUBSYSTEM_PATH.append(SERVICE, IIOP));
        final int count = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.ENABLE_BY_DEFAULT, EJB3SubsystemXMLAttribute.USE_QUALIFIED_NAME);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case ENABLE_BY_DEFAULT:
                    EJB3IIOPResourceDefinition.ENABLE_BY_DEFAULT.parseAndSetParameter(value,operation,reader);
                    break;
                case USE_QUALIFIED_NAME:
                    EJB3IIOPResourceDefinition.USE_QUALIFIED_NAME.parseAndSetParameter(value,operation,reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        requireNoContent(reader);
        operations.add(operation);
    }

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
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseEntityBean(final XMLExtendedStreamReader reader, List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        // no attributes expected
        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case BEAN_INSTANCE_POOL_REF: {
                    final String poolName = readStringAttributeElement(reader, EJB3SubsystemXMLAttribute.POOL_NAME.getLocalName());
                    EJB3SubsystemRootResourceDefinition.DEFAULT_ENTITY_BEAN_INSTANCE_POOL.parseAndSetParameter(poolName, ejb3SubsystemAddOperation, reader);
                    break;
                }
                case OPTIMISTIC_LOCKING: {
                    final String enabled = readStringAttributeElement(reader, EJB3SubsystemXMLAttribute.ENABLED.getLocalName());
                    EJB3SubsystemRootResourceDefinition.DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING.parseAndSetParameter(enabled, ejb3SubsystemAddOperation, reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

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
                case STATEFUL: {
                    this.parseStatefulBean(reader, operations, ejb3SubsystemAddOperation);
                    break;
                }
                case SINGLETON: {
                    this.parseSingletonBean(reader, operations, ejb3SubsystemAddOperation);
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
                case CACHE_REF: {
                    EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_CACHE.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    break;
                }
                case CLUSTERED_CACHE_REF: {
                    EJB3SubsystemRootResourceDefinition.DEFAULT_CLUSTERED_SFSB_CACHE.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
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

    private void parseSingletonBean(final XMLExtendedStreamReader reader, final List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> missingRequiredAttributes = EnumSet.of(EJB3SubsystemXMLAttribute.DEFAULT_ACCESS_TIMEOUT);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT_ACCESS_TIMEOUT:
                    EJB3SubsystemRootResourceDefinition.DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    // found the mandatory attribute
                    missingRequiredAttributes.remove(EJB3SubsystemXMLAttribute.DEFAULT_ACCESS_TIMEOUT);
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

    void parseStrictMaxPool(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String poolName = null;
        final ModelNode operation = Util.createAddOperation();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    poolName = value;
                    break;
                case MAX_POOL_SIZE:
                    StrictMaxPoolResourceDefinition.MAX_POOL_SIZE.parseAndSetParameter(value, operation, reader);
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

    private void parseCaches(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        // no attributes expected
        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case CACHE: {
                    this.parseCache(reader, operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseCache(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        String name = null;
        ModelNode operation = Util.createAddOperation();
        //Set<String> aliases = new LinkedHashSet<String>();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            switch (EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i))) {
                case NAME: {
                    name = value;
                    break;
                }
                case PASSIVATION_STORE_REF: {
                    CacheFactoryResourceDefinition.PASSIVATION_STORE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case ALIASES: {
                    for (String alias : reader.getListAttributeValue(i)) {
                        CacheFactoryResourceDefinition.ALIASES.parseAndAddParameterElement(alias, operation, reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoContent(reader);
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.NAME.getLocalName()));
        }
        final PathAddress address = this.getEJB3SubsystemAddress().append(PathElement.pathElement(CACHE, name));
        operation.get(OP_ADDR).set(address.toModelNode());
        operations.add(operation);
    }

    @SuppressWarnings("deprecation")
    protected void parsePassivationStores(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        // no attributes expected
        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case FILE_PASSIVATION_STORE: {
                    this.parseFilePassivationStore(reader, operations);
                    break;
                }
                case CLUSTER_PASSIVATION_STORE: {
                    this.parseClusterPassivationStore(reader, operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    protected void parseFilePassivationStore(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        String name = null;
        ModelNode operation = Util.createAddOperation();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            switch (EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i))) {
                case NAME: {
                    name = value;
                    break;
                }
                case MAX_SIZE: {
                    FilePassivationStoreResourceDefinition.MAX_SIZE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case IDLE_TIMEOUT: {
                    LegacyPassivationStoreResourceDefinition.IDLE_TIMEOUT.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case IDLE_TIMEOUT_UNIT: {
                    LegacyPassivationStoreResourceDefinition.IDLE_TIMEOUT_UNIT.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case RELATIVE_TO: {
                    FilePassivationStoreResourceDefinition.RELATIVE_TO.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case GROUPS_PATH: {
                    FilePassivationStoreResourceDefinition.GROUPS_PATH.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case SESSIONS_PATH: {
                    FilePassivationStoreResourceDefinition.SESSIONS_PATH.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case SUBDIRECTORY_COUNT: {
                    FilePassivationStoreResourceDefinition.SUBDIRECTORY_COUNT.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoContent(reader);
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.NAME.getLocalName()));
        }
        // create and add the operation
        operation.get(OP_ADDR).set(SUBSYSTEM_PATH.append(FILE_PASSIVATION_STORE, name).toModelNode());
        operations.add(operation);
    }

    @SuppressWarnings("deprecation")
    protected void parseClusterPassivationStore(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        String name = null;
        ModelNode operation = Util.createAddOperation();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            switch (EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i))) {
                case NAME: {
                    name = value;
                    break;
                }
                case MAX_SIZE: {
                    ClusterPassivationStoreResourceDefinition.MAX_SIZE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case IDLE_TIMEOUT: {
                    LegacyPassivationStoreResourceDefinition.IDLE_TIMEOUT.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case IDLE_TIMEOUT_UNIT: {
                    LegacyPassivationStoreResourceDefinition.IDLE_TIMEOUT_UNIT.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case CACHE_CONTAINER: {
                    ClusterPassivationStoreResourceDefinition.CACHE_CONTAINER.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case BEAN_CACHE: {
                    ClusterPassivationStoreResourceDefinition.BEAN_CACHE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case CLIENT_MAPPINGS_CACHE: {
                    ClusterPassivationStoreResourceDefinition.CLIENT_MAPPINGS_CACHE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case PASSIVATE_EVENTS_ON_REPLICATE: {
                    ClusterPassivationStoreResourceDefinition.PASSIVATE_EVENTS_ON_REPLICATE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoContent(reader);
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.NAME.getLocalName()));
        }
        // create and add the operation
        operation.get(OP_ADDR).set(SUBSYSTEM_PATH.append(CLUSTER_PASSIVATION_STORE, name).toModelNode());
        operations.add(operation);
    }

    protected void parseTimerService(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        ModelNode fileDataStoreAdd = null;
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);
        address.add(SERVICE, TIMER_SERVICE);
        final ModelNode timerServiceAdd = new ModelNode();
        timerServiceAdd.get(OP).set(ADD);
        timerServiceAdd.get(OP_ADDR).set(address);

        ModelNode dataStorePath = null;
        ModelNode dataStorePathRelativeTo = null;

        final int attCount = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.THREAD_POOL_NAME);
        for (int i = 0; i < attCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case THREAD_POOL_NAME:
                    TimerServiceResourceDefinition.THREAD_POOL_NAME.parseAndSetParameter(value, timerServiceAdd, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
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
                                dataStorePath = FileDataStoreResourceDefinition.PATH.parse(value, reader);
                                break;
                            case RELATIVE_TO:
                                if (dataStorePathRelativeTo != null) {
                                    throw unexpectedAttribute(reader, i);
                                }
                                dataStorePathRelativeTo = FileDataStoreResourceDefinition.RELATIVE_TO.parse(value, reader);
                                break;
                            default:
                                throw unexpectedAttribute(reader, i);
                        }
                    }
                    if (dataStorePath == null) {
                        throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.PATH));
                    }
                    timerServiceAdd.get(DEFAULT_DATA_STORE).set("default-file-store");
                    fileDataStoreAdd = new ModelNode();
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
        if(fileDataStoreAdd != null) {
            operations.add(fileDataStoreAdd);
        }
    }

    private void parseThreadPools(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        // no attributes expected
        requireNoAttributes(reader);

        final ModelNode parentAddress = SUBSYSTEM_PATH.toModelNode();
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            EJB3SubsystemNamespace readerNS = EJB3SubsystemNamespace.forUri(reader.getNamespaceURI());
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case THREAD_POOL: {
                    ThreadsParser.getInstance().parseUnboundedQueueThreadPool(reader, readerNS.getUriString(),
                            Namespace.THREADS_1_1, parentAddress, operations, THREAD_POOL, null);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    protected EJB3SubsystemNamespace getExpectedNamespace() {
        return EJB3SubsystemNamespace.EJB3_1_2;
    }

    PathAddress getEJB3SubsystemAddress() {
        return SUBSYSTEM_PATH;
    }

    private void parseInVMRemoteInterfaceInvocation(final XMLExtendedStreamReader reader, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> missingRequiredAttributes = EnumSet.of(EJB3SubsystemXMLAttribute.PASS_BY_VALUE);
        //String passByValue = null;
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PASS_BY_VALUE:
                    EJB3SubsystemRootResourceDefinition.PASS_BY_VALUE.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    // found the mandatory attribute
                    missingRequiredAttributes.remove(EJB3SubsystemXMLAttribute.PASS_BY_VALUE);
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

}
