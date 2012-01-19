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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.threads.Namespace;
import org.jboss.as.threads.ThreadsParser;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.ALIASES;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.ASYNC;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.BEAN_CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CACHE_CONTAINER;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CLIENT_MAPPINGS_CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CLUSTER_PASSIVATION_STORE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_CLUSTERED_SFSB_CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SFSB_CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.FILE_PASSIVATION_STORE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.GROUPS_PATH;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.IDLE_TIMEOUT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.IDLE_TIMEOUT_UNIT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.IIOP;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT_UNIT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.MAX_POOL_SIZE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.MAX_SIZE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.PASSIVATE_EVENTS_ON_REPLICATE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.PASSIVATION_STORE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.PATH;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.RELATIVE_TO;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.REMOTE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVICE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SESSIONS_PATH;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SUBDIRECTORY_COUNT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.THREAD_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.THREAD_POOL_NAME;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.TIMER_SERVICE;

/**
 * @author Jaikiran Pai
 */
public class EJB3Subsystem12Parser implements XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    public static final EJB3Subsystem12Parser INSTANCE = new EJB3Subsystem12Parser();

    private EJB3Subsystem12Parser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {

        context.startSubsystemElement(EJB3Extension.NAMESPACE_1_2, false);

        ModelNode model = context.getModelNode();

        // write the session-bean element
        if (model.hasDefined(EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL) || model.hasDefined(EJB3SubsystemModel.DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT)
                || model.hasDefined(EJB3SubsystemModel.DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT)) {
            // <session-bean>
            writer.writeStartElement(EJB3SubsystemXMLElement.SESSION_BEAN.getLocalName());
        }
        // <stateless> element
        if (model.hasDefined(EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL)) {
            // <stateless>
            writer.writeStartElement(EJB3SubsystemXMLElement.STATELESS.getLocalName());
            // write out the <bean-instance-pool-ref>
            this.writeDefaultSLSBPool(writer, model);
            // </stateless>
            writer.writeEndElement();
        }
        // <stateful> element
        if (model.hasDefined(EJB3SubsystemModel.DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT)
                || model.hasDefined(EJB3SubsystemModel.DEFAULT_SFSB_CACHE)
                || model.hasDefined(EJB3SubsystemModel.DEFAULT_CLUSTERED_SFSB_CACHE)) {
            // <stateful>
            writer.writeStartElement(EJB3SubsystemXMLElement.STATEFUL.getLocalName());
            // write out the <stateful> element contents
            this.writeStatefulBean(writer, model);
            // </stateful>
            writer.writeEndElement();
        }
        // <singleton> element
        if (model.hasDefined(EJB3SubsystemModel.DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT)) {
            // <singleton>
            writer.writeStartElement(EJB3SubsystemXMLElement.SINGLETON.getLocalName());
            // write out the <singleton> element contents
            this.writeSingletonBean(writer, model);
            // </singleton>
            writer.writeEndElement();
        }
        // write out the </session-bean> end element
        if (model.hasDefined(EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL) || model.hasDefined(EJB3SubsystemModel.DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT)
                || model.hasDefined(EJB3SubsystemModel.DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT)) {
            // </session-bean>
            writer.writeEndElement();
        }

        // write the mdb element
        if (model.hasDefined(EJB3SubsystemModel.DEFAULT_MDB_INSTANCE_POOL) || model.hasDefined(EJB3SubsystemModel.DEFAULT_RESOURCE_ADAPTER_NAME)) {
            // <mdb>
            writer.writeStartElement(EJB3SubsystemXMLElement.MDB.getLocalName());
            // write out the mdb element contents
            this.writeMDB(writer, model);
            // </mdb>
            writer.writeEndElement();
        }

        // write the entity bean element
        if (model.hasDefined(EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_INSTANCE_POOL) || model.hasDefined(EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING)) {
            // <entity-bean>
            writer.writeStartElement(EJB3SubsystemXMLElement.ENTITY_BEAN.getLocalName());
            // write out the mdb element contents
            this.writeEntityBean(writer, model);
            // </entity-bean>
            writer.writeEndElement();
        }
        // write the pools element
        if (model.hasDefined(EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL)) {
            // <pools>
            writer.writeStartElement(EJB3SubsystemXMLElement.POOLS.getLocalName());
            // <bean-instance-pools>
            writer.writeStartElement(EJB3SubsystemXMLElement.BEAN_INSTANCE_POOLS.getLocalName());
            // write the bean instance pools
            this.writeBeanInstancePools(writer, model);
            // </bean-instance-pools>
            writer.writeEndElement();
            // </pools>
            writer.writeEndElement();
        }

        // write the caches element
        if (model.hasDefined(EJB3SubsystemModel.CACHE)) {
            // <caches>
            writer.writeStartElement(EJB3SubsystemXMLElement.CACHES.getLocalName());
            // write the caches
            this.writeCaches(writer, model);
            // </caches>
            writer.writeEndElement();
        }
        // write the passivation-stores element
        if (model.hasDefined(EJB3SubsystemModel.CLUSTER_PASSIVATION_STORE)
                || model.hasDefined(EJB3SubsystemModel.FILE_PASSIVATION_STORE)) {
            // <passivation-stores>
            writer.writeStartElement(EJB3SubsystemXMLElement.PASSIVATION_STORES.getLocalName());
            // write the caches
            this.writeFilePassivationStores(writer, model);
            this.writeClusterPassivationStores(writer, model);
            // </passivation-stores>
            writer.writeEndElement();
        }

        // write the async element
        if (model.hasDefined(SERVICE) && model.get(SERVICE).hasDefined(ASYNC)) {
            writer.writeStartElement(EJB3SubsystemXMLElement.ASYNC.getLocalName());
            writeAsync(writer, model.get(SERVICE, ASYNC));
            writer.writeEndElement();
        }
        // timer-service
        if (model.hasDefined(SERVICE) && model.get(SERVICE).hasDefined(TIMER_SERVICE)) {
            // <timer-service>
            writer.writeStartElement(EJB3SubsystemXMLElement.TIMER_SERVICE.getLocalName());
            final ModelNode timerServiceModel = model.get(SERVICE, TIMER_SERVICE);
            this.writeTimerService(writer, timerServiceModel);
            // </timer-service>
            writer.writeEndElement();
        }

        // write the remote element
        if (model.hasDefined(SERVICE) && model.get(SERVICE).hasDefined(REMOTE)) {
            writer.writeStartElement(EJB3SubsystemXMLElement.REMOTE.getLocalName());
            writeRemote(writer, model.get(SERVICE, REMOTE));
            writer.writeEndElement();
        }

        // thread-pools
        if (model.hasDefined(THREAD_POOL)) {
            // <timer-service>
            writer.writeStartElement(EJB3SubsystemXMLElement.THREAD_POOLS.getLocalName());
            final ModelNode threadsModel = model.get(THREAD_POOL);
            this.writeThreadPools(writer, threadsModel);
            // </timer-service>
            writer.writeEndElement();
        }

        // iiop
        // write the remote element
        if (model.hasDefined(SERVICE) && model.get(SERVICE).hasDefined(IIOP)) {
            writer.writeStartElement(EJB3SubsystemXMLElement.IIOP.getLocalName());
            writeIIOP(writer, model.get(SERVICE, IIOP));
            writer.writeEndElement();
        }


        // write the subsystem end element
        writer.writeEndElement();
    }

    private void writeIIOP(final XMLExtendedStreamWriter writer, final ModelNode model) throws XMLStreamException {
        writer.writeAttribute(EJB3SubsystemXMLAttribute.USE_QUALIFIED_NAME.getLocalName(), model.require(EJB3SubsystemModel.USE_QUALIFIED_NAME).asString());
        writer.writeAttribute(EJB3SubsystemXMLAttribute.ENABLE_BY_DEFAULT.getLocalName(), model.require(EJB3SubsystemModel.ENABLE_BY_DEFAULT).asString());
    }

    private void writeThreadPools(final XMLExtendedStreamWriter writer, final ModelNode threadPoolsModel) throws XMLStreamException {
        for (Property threadPool : threadPoolsModel.asPropertyList()) {
            ThreadsParser.getInstance().writeUnboundedQueueThreadPool(writer, threadPool.getValue(), EJB3SubsystemXMLElement.THREAD_POOL.getLocalName(), true);
        }
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
                case EJB3_1_2: {
                    final EJB3SubsystemXMLElement element = EJB3SubsystemXMLElement.forName(reader.getLocalName());
                    if (!encountered.add(element)) {
                        throw unexpectedElement(reader);
                    }
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

    private void writeRemote(final XMLExtendedStreamWriter writer, final ModelNode model) throws XMLStreamException {
        writer.writeAttribute(EJB3SubsystemXMLAttribute.CONNECTOR_REF.getLocalName(), model.require(EJB3SubsystemModel.CONNECTOR_REF).asString());
        writer.writeAttribute(EJB3SubsystemXMLAttribute.THREAD_POOL_NAME.getLocalName(), model.require(EJB3SubsystemModel.THREAD_POOL_NAME).asString());
    }

    private void writeAsync(final XMLExtendedStreamWriter writer, final ModelNode model) throws XMLStreamException {
        writer.writeAttribute(EJB3SubsystemXMLAttribute.THREAD_POOL_NAME.getLocalName(), model.require(EJB3SubsystemModel.THREAD_POOL_NAME).asString());
    }

    /**
     * Writes out the <mdb> element and its nested elements
     *
     * @param writer       XML writer
     * @param mdbModelNode The <mdb> element {@link org.jboss.dmr.ModelNode}
     * @throws javax.xml.stream.XMLStreamException
     *
     */
    private void writeMDB(final XMLExtendedStreamWriter writer, final ModelNode mdbModelNode) throws XMLStreamException {
        if (mdbModelNode.hasDefined(EJB3SubsystemModel.DEFAULT_RESOURCE_ADAPTER_NAME)) {
            // <resource-adapter-ref>
            writer.writeStartElement(EJB3SubsystemXMLElement.RESOURCE_ADAPTER_REF.getLocalName());
            final String resourceAdapterName = mdbModelNode.get(EJB3SubsystemModel.DEFAULT_RESOURCE_ADAPTER_NAME).asString();
            // write the value
            writer.writeAttribute(EJB3SubsystemXMLAttribute.RESOURCE_ADAPTER_NAME.getLocalName(), resourceAdapterName);
            // </resource-adapter-ref>
            writer.writeEndElement();
        }
        if (mdbModelNode.hasDefined(EJB3SubsystemModel.DEFAULT_MDB_INSTANCE_POOL)) {
            // <bean-instance-pool-ref>
            writer.writeStartElement(EJB3SubsystemXMLElement.BEAN_INSTANCE_POOL_REF.getLocalName());
            final String poolRefName = mdbModelNode.get(EJB3SubsystemModel.DEFAULT_MDB_INSTANCE_POOL).asString();
            // write the value
            writer.writeAttribute(EJB3SubsystemXMLAttribute.POOL_NAME.getLocalName(), poolRefName);
            // </bean-instance-pool-ref>
            writer.writeEndElement();
        }
    }
    /**
     * Writes out the <entity-bean> element and its nested elements
     *
     * @param writer       XML writer
     * @param entityModelNode The <mdb> element {@link org.jboss.dmr.ModelNode}
     * @throws javax.xml.stream.XMLStreamException
     *
     */
    private void writeEntityBean(final XMLExtendedStreamWriter writer, final ModelNode entityModelNode) throws XMLStreamException {
        if (entityModelNode.hasDefined(EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_INSTANCE_POOL)) {
            // <bean-instance-pool-ref>
            writer.writeStartElement(EJB3SubsystemXMLElement.BEAN_INSTANCE_POOL_REF.getLocalName());
            final String poolRefName = entityModelNode.get(EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_INSTANCE_POOL).asString();
            // write the value
            writer.writeAttribute(EJB3SubsystemXMLAttribute.POOL_NAME.getLocalName(), poolRefName);
            // </bean-instance-pool-ref>
            writer.writeEndElement();
        }
        if (entityModelNode.hasDefined(EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING)) {
            // <optimistic-locking>
            writer.writeStartElement(EJB3SubsystemXMLElement.OPTIMISTIC_LOCKING.getLocalName());
            final Boolean locking = entityModelNode.get(EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING).asBoolean();
            // write the value
            writer.writeAttribute(EJB3SubsystemXMLAttribute.ENABLED.getLocalName(), locking.toString());
            // <optimistic-locking>
            writer.writeEndElement();
        }
    }

    private void writeSingletonBean(final XMLExtendedStreamWriter writer, final ModelNode singletonBeanModel) throws XMLStreamException {
        final String defaultAccessTimeout = singletonBeanModel.get(DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT).asString();
        writer.writeAttribute(EJB3SubsystemXMLAttribute.DEFAULT_ACCESS_TIMEOUT.getLocalName(), defaultAccessTimeout);
    }

    private void writeStatefulBean(final XMLExtendedStreamWriter writer, final ModelNode statefulBeanModel) throws XMLStreamException {
        if (statefulBeanModel.hasDefined(DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT)) {
            String defaultAccessTimeout = statefulBeanModel.get(DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT).asString();
            writer.writeAttribute(EJB3SubsystemXMLAttribute.DEFAULT_ACCESS_TIMEOUT.getLocalName(), defaultAccessTimeout);
        }
        if (statefulBeanModel.hasDefined(DEFAULT_SFSB_CACHE)) {
            String cache = statefulBeanModel.get(DEFAULT_SFSB_CACHE).asString();
            writer.writeAttribute(EJB3SubsystemXMLAttribute.CACHE_REF.getLocalName(), cache);
        }
        if (statefulBeanModel.hasDefined(DEFAULT_CLUSTERED_SFSB_CACHE)) {
            String cache = statefulBeanModel.get(DEFAULT_CLUSTERED_SFSB_CACHE).asString();
            writer.writeAttribute(EJB3SubsystemXMLAttribute.CLUSTERED_CACHE_REF.getLocalName(), cache);
        }
    }

    private void writeDefaultSLSBPool(final XMLExtendedStreamWriter writer, final ModelNode model) throws XMLStreamException {

        if (model.hasDefined(EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL)) {
            // <bean-instance-pool-ref>
            writer.writeStartElement(EJB3SubsystemXMLElement.BEAN_INSTANCE_POOL_REF.getLocalName());
            // contents of pool-ref
            final String poolRefName = model.get(EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL).asString();
            writer.writeAttribute(EJB3SubsystemXMLAttribute.POOL_NAME.getLocalName(), poolRefName);
            // </bean-instance-pool-ref>
            writer.writeEndElement();
        }

    }

    private void writeBeanInstancePools(final XMLExtendedStreamWriter writer, final ModelNode beanInstancePoolModelNode) throws XMLStreamException {
        if (beanInstancePoolModelNode.hasDefined(EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL)) {
            final List<Property> strictMaxPools = beanInstancePoolModelNode.get(EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL).asPropertyList();
            for (Property property : strictMaxPools) {
                // <strict-max-pool>
                writer.writeStartElement(EJB3SubsystemXMLElement.STRICT_MAX_POOL.getLocalName());
                // contents of strict-max-pool
                final ModelNode strictMaxPoolModelNode = property.getValue();
                this.writeStrictMaxPoolConfig(writer, strictMaxPoolModelNode);
                // </strict-max-pool>
                writer.writeEndElement();
            }
        }
    }

    private void writeStrictMaxPoolConfig(final XMLExtendedStreamWriter writer, final ModelNode strictMaxPoolModelNode) throws XMLStreamException {
        // write the "name" attribute of the pool
        final String poolName = strictMaxPoolModelNode.get(EJB3SubsystemModel.NAME).asString();
        writer.writeAttribute(EJB3SubsystemXMLAttribute.NAME.getLocalName(), poolName);

        StrictMaxPoolResourceDefinition.MAX_POOL_SIZE.marshallAsAttribute(strictMaxPoolModelNode, writer);
        StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT.marshallAsAttribute(strictMaxPoolModelNode, writer);
        StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT_UNIT.marshallAsAttribute(strictMaxPoolModelNode, writer);
    }

    private void writeCaches(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
        List<Property> caches = model.get(EJB3SubsystemModel.CACHE).asPropertyList();
        for (Property property : caches) {
            // <strict-max-pool>
            writer.writeStartElement(EJB3SubsystemXMLElement.CACHE.getLocalName());
            ModelNode cache = property.getValue();
            writer.writeAttribute(EJB3SubsystemXMLAttribute.NAME.getLocalName(), cache.get(EJB3SubsystemModel.NAME).asString());
            CacheFactoryResourceDefinition.PASSIVATION_STORE.marshallAsAttribute(cache, writer);
            CacheFactoryResourceDefinition.ALIASES.marshallAsElement(cache, writer);
            writer.writeEndElement();
        }
    }

    private void writeClusterPassivationStores(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
        if (model.hasDefined(EJB3SubsystemModel.CLUSTER_PASSIVATION_STORE)) {
            List<Property> caches = model.get(EJB3SubsystemModel.CLUSTER_PASSIVATION_STORE).asPropertyList();
            for (Property property : caches) {
                // <strict-max-pool>
                writer.writeStartElement(EJB3SubsystemXMLElement.CLUSTER_PASSIVATION_STORE.getLocalName());
                ModelNode store = property.getValue();
                writer.writeAttribute(EJB3SubsystemXMLAttribute.NAME.getLocalName(), store.get(EJB3SubsystemModel.NAME).asString());
                PassivationStoreResourceDefinition.IDLE_TIMEOUT.marshallAsAttribute(store, writer);
                PassivationStoreResourceDefinition.IDLE_TIMEOUT_UNIT.marshallAsAttribute(store, writer);
                ClusterPassivationStoreResourceDefinition.MAX_SIZE.marshallAsAttribute(store, writer);
                ClusterPassivationStoreResourceDefinition.CACHE_CONTAINER.marshallAsAttribute(store, writer);
                ClusterPassivationStoreResourceDefinition.BEAN_CACHE.marshallAsAttribute(store, writer);
                ClusterPassivationStoreResourceDefinition.CLIENT_MAPPINGS_CACHE.marshallAsAttribute(store, writer);
                ClusterPassivationStoreResourceDefinition.PASSIVATE_EVENTS_ON_REPLICATE.marshallAsAttribute(store, writer);
                writer.writeEndElement();
            }
        }
    }

    private void writeFilePassivationStores(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
        if (model.hasDefined(EJB3SubsystemModel.FILE_PASSIVATION_STORE)) {
            List<Property> caches = model.get(EJB3SubsystemModel.FILE_PASSIVATION_STORE).asPropertyList();
            for (Property property : caches) {
                // <strict-max-pool>
                writer.writeStartElement(EJB3SubsystemXMLElement.FILE_PASSIVATION_STORE.getLocalName());
                ModelNode store = property.getValue();
                writer.writeAttribute(EJB3SubsystemXMLAttribute.NAME.getLocalName(), store.get(EJB3SubsystemModel.NAME).asString());
                PassivationStoreResourceDefinition.IDLE_TIMEOUT.marshallAsAttribute(store, writer);
                PassivationStoreResourceDefinition.IDLE_TIMEOUT_UNIT.marshallAsAttribute(store, writer);
                FilePassivationStoreResourceDefinition.MAX_SIZE.marshallAsAttribute(store, writer);
                FilePassivationStoreResourceDefinition.RELATIVE_TO.marshallAsAttribute(store, writer);
                FilePassivationStoreResourceDefinition.GROUPS_PATH.marshallAsAttribute(store, writer);
                FilePassivationStoreResourceDefinition.SESSIONS_PATH.marshallAsAttribute(store, writer);
                FilePassivationStoreResourceDefinition.SUBDIRECTORY_COUNT.marshallAsAttribute(store, writer);
                writer.writeEndElement();
            }
        }
    }

    private void writeTimerService(final XMLExtendedStreamWriter writer, final ModelNode timerServiceModel) throws XMLStreamException {

        TimerServiceResourceDefinition.THREAD_POOL_NAME.marshallAsAttribute(timerServiceModel, writer);

        // <data-store>
        if (TimerServiceResourceDefinition.PATH.isMarshallable(timerServiceModel)
                || TimerServiceResourceDefinition.RELATIVE_TO.isMarshallable(timerServiceModel)) {

            writer.writeEmptyElement(EJB3SubsystemXMLElement.DATA_STORE.getLocalName());
            TimerServiceResourceDefinition.PATH.marshallAsAttribute(timerServiceModel, writer);
            TimerServiceResourceDefinition.RELATIVE_TO.marshallAsAttribute(timerServiceModel, writer);
        }

    }


    private void parseRemote(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String connectorName = null;
        String threadPoolName = null;
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.CONNECTOR_REF, EJB3SubsystemXMLAttribute.THREAD_POOL_NAME);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case CONNECTOR_REF:
                    connectorName = value;
                    break;
                case THREAD_POOL_NAME:
                    threadPoolName = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        requireNoContent(reader);
        operations.add(EJB3RemoteServiceAdd.create(connectorName, threadPoolName));
    }

    private void parseAsync(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String threadPoolName = null;
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.THREAD_POOL_NAME);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case THREAD_POOL_NAME:
                    threadPoolName = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        requireNoContent(reader);
        operations.add(EJB3AsyncServiceAdd.create(threadPoolName));
    }

    private void parseIIOP(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        boolean enableByDefault = true;
        boolean useQualifiedName = true;
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.ENABLE_BY_DEFAULT, EJB3SubsystemXMLAttribute.USE_QUALIFIED_NAME);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case ENABLE_BY_DEFAULT:
                    enableByDefault = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                case USE_QUALIFIED_NAME:
                    useQualifiedName = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        requireNoContent(reader);
        operations.add(EJB3IIOPAdd.create(enableByDefault, useQualifiedName));
    }

    private ModelNode parseMDB(final XMLExtendedStreamReader reader, List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
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

    private ModelNode parseEntityBean(final XMLExtendedStreamReader reader, List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        ModelNode mdbModelNode = new ModelNode();
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

    private void parseStatefulBean(final XMLExtendedStreamReader reader, final List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> missingRequiredAttributes = EnumSet.of(EJB3SubsystemXMLAttribute.DEFAULT_ACCESS_TIMEOUT, EJB3SubsystemXMLAttribute.CACHE_REF);
        String defaultAccessTimeout = null;
        String cache = null;
        String clusteredCache = null;
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT_ACCESS_TIMEOUT: {
                    defaultAccessTimeout = EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT.parse(value, reader).asString();
                    break;
                }
                case CACHE_REF: {
                    cache = EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_CACHE.parse(value, reader).asString();
                    break;
                }
                case CLUSTERED_CACHE_REF: {
                    clusteredCache = EJB3SubsystemRootResourceDefinition.DEFAULT_CLUSTERED_SFSB_CACHE.parse(value, reader).asString();
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
        EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT.parseAndSetParameter(defaultAccessTimeout, ejb3SubsystemAddOperation, reader);
        EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_CACHE.parseAndSetParameter(cache, ejb3SubsystemAddOperation, reader);
        EJB3SubsystemRootResourceDefinition.DEFAULT_CLUSTERED_SFSB_CACHE.parseAndSetParameter(clusteredCache, ejb3SubsystemAddOperation, reader);
    }

    private void parseSingletonBean(final XMLExtendedStreamReader reader, final List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> missingRequiredAttributes = EnumSet.of(EJB3SubsystemXMLAttribute.DEFAULT_ACCESS_TIMEOUT);
        String defaultAccessTimeout = null;
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT_ACCESS_TIMEOUT:
                    defaultAccessTimeout = EJB3SubsystemRootResourceDefinition.DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT.parse(value, reader).asString();
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
        EJB3SubsystemRootResourceDefinition.DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT.parseAndSetParameter(defaultAccessTimeout, ejb3SubsystemAddOperation, reader);
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

    private void parseStrictMaxPool(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
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
                case INSTANCE_AQUISITION_TIMEOUT:
                    timeout = StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT.parse(value, reader).asLong();
                    break;
                case INSTANCE_AQUISITION_TIMEOUT_UNIT:
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
        String passivationStore = null;
        Set<String> aliases = new LinkedHashSet<String>();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            switch (EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i))) {
                case NAME: {
                    name = value;
                    break;
                }
                case PASSIVATION_STORE_REF: {
                    passivationStore = CacheFactoryResourceDefinition.PASSIVATION_STORE.parse(value, reader).asString();
                    break;
                }
                case ALIASES: {
                    for (String alias: reader.getListAttributeValue(i)) {
                        aliases.add(CacheFactoryResourceDefinition.ALIASES.parse(alias, reader).asString());
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
        // create and add the operation
        operations.add(this.createAddStatefulCacheOperation(name, aliases, passivationStore));
    }

    private void parsePassivationStores(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
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

    private void parseFilePassivationStore(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        String name = null;
        Integer maxSize = null;
        Long timeout = null;
        String unit = null;
        String relativeTo = null;
        String groupsPath = null;
        String sessionsPath = null;
        Integer subdirectoryCount = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            switch (EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i))) {
                case NAME: {
                    name = value;
                    break;
                }
                case MAX_SIZE: {
                    maxSize = FilePassivationStoreResourceDefinition.MAX_SIZE.parse(value, reader).asInt();
                    break;
                }
                case IDLE_TIMEOUT: {
                    timeout = PassivationStoreResourceDefinition.IDLE_TIMEOUT.parse(value, reader).asLong();
                    break;
                }
                case IDLE_TIMEOUT_UNIT: {
                    unit = PassivationStoreResourceDefinition.IDLE_TIMEOUT_UNIT.parse(value, reader).asString();
                    break;
                }
                case RELATIVE_TO: {
                    relativeTo = FilePassivationStoreResourceDefinition.RELATIVE_TO.parse(value, reader).asString();
                    break;
                }
                case GROUPS_PATH: {
                    groupsPath = FilePassivationStoreResourceDefinition.GROUPS_PATH.parse(value, reader).asString();
                    break;
                }
                case SESSIONS_PATH: {
                    sessionsPath = FilePassivationStoreResourceDefinition.SESSIONS_PATH.parse(value, reader).asString();
                    break;
                }
                case SUBDIRECTORY_COUNT: {
                    subdirectoryCount = FilePassivationStoreResourceDefinition.SUBDIRECTORY_COUNT.parse(value, reader).asInt();
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
        operations.add(this.createAddFilePassivationStoreOperation(name, maxSize, timeout, unit, relativeTo, groupsPath, sessionsPath, subdirectoryCount));
    }

    private void parseClusterPassivationStore(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        String name = null;
        Integer maxSize = null;
        Long timeout = null;
        String unit = null;
        String cacheContainer = null;
        String beanCache = null;
        String clientMappingsCache = null;
        Boolean passivateEventsOnReplicate = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            switch (EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i))) {
                case NAME: {
                    name = value;
                    break;
                }
                case MAX_SIZE: {
                    maxSize = ClusterPassivationStoreResourceDefinition.MAX_SIZE.parse(value, reader).asInt();
                    break;
                }
                case IDLE_TIMEOUT: {
                    timeout = PassivationStoreResourceDefinition.IDLE_TIMEOUT.parse(value, reader).asLong();
                    break;
                }
                case IDLE_TIMEOUT_UNIT: {
                    unit = PassivationStoreResourceDefinition.IDLE_TIMEOUT_UNIT.parse(value, reader).asString();
                    break;
                }
                case CACHE_CONTAINER: {
                    cacheContainer = ClusterPassivationStoreResourceDefinition.CACHE_CONTAINER.parse(value, reader).asString();
                    break;
                }
                case BEAN_CACHE: {
                    beanCache = ClusterPassivationStoreResourceDefinition.BEAN_CACHE.parse(value, reader).asString();
                    break;
                }
                case CLIENT_MAPPINGS_CACHE: {
                    clientMappingsCache = ClusterPassivationStoreResourceDefinition.CLIENT_MAPPINGS_CACHE.parse(value, reader).asString();
                    break;
                }
                case PASSIVATE_EVENTS_ON_REPLICATE: {
                    passivateEventsOnReplicate = ClusterPassivationStoreResourceDefinition.PASSIVATE_EVENTS_ON_REPLICATE.parse(value, reader).asBoolean();
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
        operations.add(this.createAddClusterPassivationStoreOperation(name, maxSize, timeout, unit, cacheContainer, beanCache, clientMappingsCache, passivateEventsOnReplicate));
    }

    private void parseTimerService(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);
        address.add(SERVICE, TIMER_SERVICE);
        final ModelNode timerServiceAdd = new ModelNode();
        timerServiceAdd.get(OP).set(ADD);
        timerServiceAdd.get(OP_ADDR).set(address);

        String dataStorePath = null;
        String dataStorePathRelativeTo = null;

        final int attCount = reader.getAttributeCount();
        String threadPoolName = null;
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.THREAD_POOL_NAME);
        for (int i = 0; i < attCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case THREAD_POOL_NAME:
                    threadPoolName = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        timerServiceAdd.get(THREAD_POOL_NAME).set(threadPoolName);

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
                                dataStorePath = TimerServiceResourceDefinition.PATH.parse(value, reader).asString();
                                break;
                            case RELATIVE_TO:
                                if (dataStorePathRelativeTo != null) {
                                    throw unexpectedAttribute(reader, i);
                                }
                                dataStorePathRelativeTo = TimerServiceResourceDefinition.RELATIVE_TO.parse(value, reader).asString();
                                break;
                            default:
                                throw unexpectedAttribute(reader, i);
                        }
                    }
                    if (dataStorePath == null) {
                        throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.PATH));
                    }
                    timerServiceAdd.get(PATH).set(dataStorePath);
                    if (dataStorePathRelativeTo != null) {
                        timerServiceAdd.get(RELATIVE_TO).set(dataStorePathRelativeTo);
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

    private void parseThreadPools(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        // no attributes expected
        requireNoAttributes(reader);


        final ModelNode parentAddress = new ModelNode();
        parentAddress.add(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);

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

    /**
     * <p>
     * Parses all attributes from the current element and sets them in the specified {@code ModelNode}.
     * </p>
     *
     * @param reader             the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param node               the {@code ModelNode} that will hold the parsed attributes.
     * @param expectedAttributes an {@code EnumSet} containing all expected attributes. If the parsed attribute is not
     *                           one of the expected attributes, an exception is thrown.
     * @param requiredAttributes an {@code EnumSet} containing all required attributes. If a required attribute is not
     *                           found, an exception is thrown.
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML, if an attribute is not one of the expected
     *          attributes or if one of the required attributes is not parsed.
     */
    private void parseAttributes(XMLExtendedStreamReader reader, ModelNode node, EnumSet<EJB3SubsystemXMLAttribute> expectedAttributes,
                                 EnumSet<EJB3SubsystemXMLAttribute> requiredAttributes) throws XMLStreamException {

        EnumSet<EJB3SubsystemXMLAttribute> parsedAttributes = EnumSet.noneOf(EJB3SubsystemXMLAttribute.class);
        if (requiredAttributes == null) {
            requiredAttributes = EnumSet.noneOf(EJB3SubsystemXMLAttribute.class);
        }

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(reader, i);
            final String attrValue = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            // check for unexpected attributes.
            if (!expectedAttributes.contains(attribute))
                throw unexpectedAttribute(reader, i);
            // check for duplicate attributes.
            if (!parsedAttributes.add(attribute)) {
                throw duplicateAttribute(reader, attribute.getLocalName());
            }
            requiredAttributes.remove(attribute);
            node.get(attribute.getLocalName()).set(attrValue);
        }

        // throw an exception if a required attribute wasn't found.
        if (!requiredAttributes.isEmpty()) {
            throw missingRequired(reader, requiredAttributes);
        }
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

    private ModelNode createAddStatefulCacheOperation(String name, Set<String> aliases, String passivationStoreRef) {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        // set the address for this operation
        final PathAddress address = this.getEJB3SubsystemAddress().append(PathElement.pathElement(CACHE, name));
        operation.get(OP_ADDR).set(address.toModelNode());
        // set the params for the operation
        if (passivationStoreRef != null) {
            operation.get(PASSIVATION_STORE).set(passivationStoreRef);
        }
        if (!aliases.isEmpty()) {
            ModelNode aliasModel = operation.get(ALIASES).setEmptyList();
            for (String alias: aliases) {
                aliasModel.add(alias);
            }
        }
        return operation;
    }

    private ModelNode createAddPassivationStoreOperation(String operationName, String name, Integer maxSize, Long idleTimeout, String idleTimeoutUnit) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        PathAddress address = this.getEJB3SubsystemAddress().append(PathElement.pathElement(operationName, name));
        operation.get(OP_ADDR).set(address.toModelNode());
        if (idleTimeout != null) {
            operation.get(IDLE_TIMEOUT).set(idleTimeout);
        }
        if (idleTimeoutUnit != null) {
            operation.get(IDLE_TIMEOUT_UNIT).set(idleTimeoutUnit);
        }
        if (maxSize != null) {
            operation.get(MAX_SIZE).set(maxSize);
        }
        return operation;
    }

    private ModelNode createAddFilePassivationStoreOperation(String name, Integer maxSize, Long idleTimeout, String idleTimeoutUnit, String relativeTo, String groupsPath, String sessionsPath, Integer subdirectoryCount) {
        ModelNode operation = this.createAddPassivationStoreOperation(FILE_PASSIVATION_STORE, name, maxSize, idleTimeout, idleTimeoutUnit);
        if (relativeTo != null) {
            operation.get(RELATIVE_TO).set(relativeTo);
        }
        if (groupsPath != null) {
            operation.get(GROUPS_PATH).set(groupsPath);
        }
        if (sessionsPath != null) {
            operation.get(SESSIONS_PATH).set(sessionsPath);
        }
        if (subdirectoryCount != null) {
            operation.get(SUBDIRECTORY_COUNT).set(subdirectoryCount);
        }
        return operation;
    }

    private ModelNode createAddClusterPassivationStoreOperation(String name, Integer maxSize, Long idleTimeout, String idleTimeoutUnit, String cacheContainer, String beanCache, String clientMappingsCache, Boolean passivateEventsOnReplicate) {
        ModelNode operation = this.createAddPassivationStoreOperation(CLUSTER_PASSIVATION_STORE, name, maxSize, idleTimeout, idleTimeoutUnit);
        if (cacheContainer != null) {
            operation.get(CACHE_CONTAINER).set(cacheContainer);
        }
        if (beanCache != null) {
            operation.get(BEAN_CACHE).set(beanCache);
        }
        if (clientMappingsCache != null) {
            operation.get(CLIENT_MAPPINGS_CACHE).set(clientMappingsCache);
        }
        if (passivateEventsOnReplicate != null) {
            operation.get(PASSIVATE_EVENTS_ON_REPLICATE).set(passivateEventsOnReplicate);
        }
        return operation;
    }

    private PathAddress getEJB3SubsystemAddress() {
        return PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME));
    }
}
