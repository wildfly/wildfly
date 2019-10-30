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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.remoting.Attribute;
import org.jboss.as.threads.ThreadsParser;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

import java.util.List;

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.*;

/**
 * The {@link XMLElementWriter} that handles the EJB subsystem. As we only write out the most recent version of
 * a subsystem we only need to keep the latest version around.
 *
 * @author Stuart Douglas
 */
public class EJB3SubsystemXMLPersister implements XMLElementWriter<SubsystemMarshallingContext> {

    public static final EJB3SubsystemXMLPersister INSTANCE = new EJB3SubsystemXMLPersister();

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {

        context.startSubsystemElement(EJB3SubsystemNamespace.EJB3_5_0.getUriString(), false);
        writeElements(writer, context);
        // write the subsystem end element
        writer.writeEndElement();
    }

    protected void writeElements(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
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
                || model.hasDefined(EJB3SubsystemModel.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE)) {
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
        if (model.hasDefined(EJB3SubsystemModel.DEFAULT_MDB_INSTANCE_POOL) || model.hasDefined(EJB3SubsystemModel.DEFAULT_RESOURCE_ADAPTER_NAME)
            || model.hasDefined(EJB3SubsystemModel.MDB_DELIVERY_GROUP)) {
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
        if (model.hasDefined(EJB3SubsystemModel.PASSIVATION_STORE)
                || model.hasDefined(EJB3SubsystemModel.CLUSTER_PASSIVATION_STORE)
                || model.hasDefined(EJB3SubsystemModel.FILE_PASSIVATION_STORE)) {
            // <passivation-stores>
            writer.writeStartElement(EJB3SubsystemXMLElement.PASSIVATION_STORES.getLocalName());
            // write the caches
            this.writePassivationStores(writer, model);
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

            // profiles element
            if (model.hasDefined(REMOTING_PROFILE)) {
                writer.writeStartElement(EJB3SubsystemXMLElement.PROFILES.getLocalName());
                writeProfiles(writer, model);
                writer.writeEndElement();
            }

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

        // in-vm-remote-interface-invocation element
        if (model.hasDefined(IN_VM_REMOTE_INTERFACE_INVOCATION_PASS_BY_VALUE)) {
            writer.writeStartElement(EJB3SubsystemXMLElement.IN_VM_REMOTE_INTERFACE_INVOCATION.getLocalName());
            writer.writeAttribute(EJB3SubsystemXMLAttribute.PASS_BY_VALUE.getLocalName(), model.get(EJB3SubsystemModel.IN_VM_REMOTE_INTERFACE_INVOCATION_PASS_BY_VALUE).asString());
            writer.writeEndElement();
        }

        // default-distinct-name element
        if (model.hasDefined(DEFAULT_DISTINCT_NAME)) {
            writer.writeStartElement(EJB3SubsystemXMLElement.DEFAULT_DISTINCT_NAME.getLocalName());
            writer.writeAttribute(EJB3SubsystemXMLAttribute.VALUE.getLocalName(), model.get(EJB3SubsystemModel.DEFAULT_DISTINCT_NAME).asString());
            writer.writeEndElement();
        }

        if (model.hasDefined(DEFAULT_SECURITY_DOMAIN)) {
            // default-security-domain element
            writer.writeStartElement(EJB3SubsystemXMLElement.DEFAULT_SECURITY_DOMAIN.getLocalName());
            writer.writeAttribute(EJB3SubsystemXMLAttribute.VALUE.getLocalName(), model.get(DEFAULT_SECURITY_DOMAIN).asString());
            writer.writeEndElement();
        }

        // application-security-domains element
        if (model.hasDefined(APPLICATION_SECURITY_DOMAIN)) {
            writer.writeStartElement(EJB3SubsystemXMLElement.APPLICATION_SECURITY_DOMAINS.getLocalName());
            writeApplicationSecurityDomains(writer, model);
            writer.writeEndElement();
        }

        // identity element
        if (model.hasDefined(SERVICE) && model.get(SERVICE).hasDefined(IDENTITY) && model.get(SERVICE, IDENTITY).hasDefined(IdentityResourceDefinition.OUTFLOW_SECURITY_DOMAINS.getName())) {
            writer.writeStartElement(EJB3SubsystemXMLElement.IDENTITY.getLocalName());
            IdentityResourceDefinition.OUTFLOW_SECURITY_DOMAINS.getAttributeMarshaller().marshallAsAttribute(IdentityResourceDefinition.OUTFLOW_SECURITY_DOMAINS, model.get(SERVICE, IDENTITY), false, writer);
            writer.writeEndElement();
        }

        // default-missing-method-permissions-deny-access element
        if (model.hasDefined(DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS)) {
            writer.writeStartElement(EJB3SubsystemXMLElement.DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS.getLocalName());
            writer.writeAttribute(EJB3SubsystemXMLAttribute.VALUE.getLocalName(), model.get(DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS).asString());
            writer.writeEndElement();
        }


        // disable-default-ejb-permissions element
        if (model.hasDefined(DISABLE_DEFAULT_EJB_PERMISSIONS)) {
            writer.writeStartElement(EJB3SubsystemXMLElement.DISABLE_DEFAULT_EJB_PERMISSIONS.getLocalName());
            writer.writeAttribute(EJB3SubsystemXMLAttribute.VALUE.getLocalName(), model.get(DISABLE_DEFAULT_EJB_PERMISSIONS).asString());
            writer.writeEndElement();
        }

        // graceful txn shutdown
        if (model.hasDefined(ENABLE_GRACEFUL_TXN_SHUTDOWN)) {
            writer.writeStartElement(EJB3SubsystemXMLElement.ENABLE_GRACEFUL_TXN_SHUTDOWN.getLocalName());
            writer.writeAttribute(EJB3SubsystemXMLAttribute.VALUE.getLocalName(), model.get(EJB3SubsystemModel.ENABLE_GRACEFUL_TXN_SHUTDOWN).asString());
            writer.writeEndElement();
        }

        // statistics element
        if (model.hasDefined(STATISTICS_ENABLED)) {
            writer.writeStartElement(EJB3SubsystemXMLElement.STATISTICS.getLocalName());
            writer.writeAttribute(EJB3SubsystemXMLAttribute.ENABLED.getLocalName(), model.get(EJB3SubsystemModel.STATISTICS_ENABLED).asString());
            writer.writeEndElement();
        }

        if (model.hasDefined(LOG_SYSTEM_EXCEPTIONS)) {
            writer.writeStartElement(EJB3SubsystemXMLElement.LOG_SYSTEM_EXCEPTIONS.getLocalName());
            writer.writeAttribute(EJB3SubsystemXMLAttribute.VALUE.getLocalName(), model.get(EJB3SubsystemModel.LOG_SYSTEM_EXCEPTIONS).asString());
            writer.writeEndElement();
        }
        if (model.hasDefined(ALLOW_EJB_NAME_REGEX)) {
            writer.writeStartElement(EJB3SubsystemXMLElement.ALLOW_EJB_NAME_REGEX.getLocalName());
            writer.writeAttribute(EJB3SubsystemXMLAttribute.VALUE.getLocalName(), model.get(EJB3SubsystemModel.ALLOW_EJB_NAME_REGEX).asString());
            writer.writeEndElement();
        }
    }

    private void writeIIOP(final XMLExtendedStreamWriter writer, final ModelNode model) throws XMLStreamException {
        EJB3IIOPResourceDefinition.ENABLE_BY_DEFAULT.marshallAsAttribute(model, writer);
        EJB3IIOPResourceDefinition.USE_QUALIFIED_NAME.marshallAsAttribute(model, writer);
    }

    private void writeThreadPools(final XMLExtendedStreamWriter writer, final ModelNode threadPoolsModel) throws XMLStreamException {
        for (Property threadPool : threadPoolsModel.asPropertyList()) {
            ThreadsParser.getInstance().writeUnboundedQueueThreadPool(writer, threadPool, EJB3SubsystemXMLElement.THREAD_POOL.getLocalName(), true);
        }
    }


    protected void writeRemote(final XMLExtendedStreamWriter writer, final ModelNode model) throws XMLStreamException {
        // only write if non-default value?
        if (model.hasDefined(EJB3SubsystemModel.CLIENT_MAPPINGS_CLUSTER_NAME)) {
            writer.writeAttribute(EJB3SubsystemXMLAttribute.CLIENT_MAPPINGS_CLUSTER_NAME.getLocalName(), model.require(EJB3SubsystemModel.CLIENT_MAPPINGS_CLUSTER_NAME).asString());
        }
        writer.writeAttribute(EJB3SubsystemXMLAttribute.CONNECTOR_REF.getLocalName(), model.require(EJB3SubsystemModel.CONNECTOR_REF).asString());
        writer.writeAttribute(EJB3SubsystemXMLAttribute.THREAD_POOL_NAME.getLocalName(), model.require(EJB3SubsystemModel.THREAD_POOL_NAME).asString());

        EJB3RemoteResourceDefinition.EXECUTE_IN_WORKER.marshallAsAttribute(model, writer);
        // write out any channel creation options
        if (model.hasDefined(CHANNEL_CREATION_OPTIONS)) {
            writeChannelCreationOptions(writer, model.get(CHANNEL_CREATION_OPTIONS));
        }
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
        if (mdbModelNode.hasDefined(EJB3SubsystemModel.MDB_DELIVERY_GROUP)) {
            //<delivery-groups>
            writer.writeStartElement(EJB3SubsystemXMLElement.DELIVERY_GROUPS.getLocalName());
            for (Property property : mdbModelNode.get(EJB3SubsystemModel.MDB_DELIVERY_GROUP).asPropertyList()) {
                // <delivery-group
                writer.writeStartElement(EJB3SubsystemXMLElement.DELIVERY_GROUP.getLocalName());
                // name=
                writer.writeAttribute(EJB3SubsystemXMLAttribute.NAME.getLocalName(), property.getName());
                // active=
                MdbDeliveryGroupResourceDefinition.ACTIVE.marshallAsAttribute(mdbModelNode.get(EJB3SubsystemModel.MDB_DELIVERY_GROUP, property.getName()), writer);
                // />
                writer.writeEndElement();
            }
            //</delivery-groups>
            writer.writeEndElement();
        }
    }



    /**
     * Writes out the <entity-bean> element and its nested elements
     *
     * @param writer          XML writer
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
        EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE.marshallAsAttribute(statefulBeanModel, writer);
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
                this.writeStrictMaxPoolConfig(writer, property);
                // </strict-max-pool>
                writer.writeEndElement();
            }
        }
    }

    private void writeStrictMaxPoolConfig(final XMLExtendedStreamWriter writer, final Property strictMaxPoolModel) throws XMLStreamException {
        // write the "name" attribute of the pool
        final ModelNode strictMaxPoolModelNode = strictMaxPoolModel.getValue();

        writer.writeAttribute(EJB3SubsystemXMLAttribute.NAME.getLocalName(), strictMaxPoolModel.getName());

        StrictMaxPoolResourceDefinition.MAX_POOL_SIZE.marshallAsAttribute(strictMaxPoolModelNode, writer);
        StrictMaxPoolResourceDefinition.DERIVE_SIZE.marshallAsAttribute(strictMaxPoolModelNode, writer);
        StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT.marshallAsAttribute(strictMaxPoolModelNode, writer);
        StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT_UNIT.marshallAsAttribute(strictMaxPoolModelNode, writer);
    }

    private void writeCaches(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
        List<Property> caches = model.get(EJB3SubsystemModel.CACHE).asPropertyList();
        for (Property property : caches) {
            writer.writeStartElement(EJB3SubsystemXMLElement.CACHE.getLocalName());
            ModelNode cache = property.getValue();
            writer.writeAttribute(EJB3SubsystemXMLAttribute.NAME.getLocalName(), property.getName());
            CacheFactoryResourceDefinition.PASSIVATION_STORE.marshallAsAttribute(cache, writer);
            writeAttribute(writer, cache, CacheFactoryResourceDefinition.ALIASES);
            writer.writeEndElement();
        }
    }

    private void writePassivationStores(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
        if (model.hasDefined(EJB3SubsystemModel.PASSIVATION_STORE)) {
            List<Property> caches = model.get(EJB3SubsystemModel.PASSIVATION_STORE).asPropertyList();
            for (Property property : caches) {
                writer.writeStartElement(EJB3SubsystemXMLElement.PASSIVATION_STORE.getLocalName());
                ModelNode store = property.getValue();
                writer.writeAttribute(EJB3SubsystemXMLAttribute.NAME.getLocalName(), property.getName());
                PassivationStoreResourceDefinition.CACHE_CONTAINER.marshallAsAttribute(store, writer);
                PassivationStoreResourceDefinition.BEAN_CACHE.marshallAsAttribute(store, writer);
                PassivationStoreResourceDefinition.MAX_SIZE.marshallAsAttribute(store, writer);
                writer.writeEndElement();
            }
        }
    }

    /**
     * Persist as a passivation-store using relevant attributes
     */
    private void writeClusterPassivationStores(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
        if (model.hasDefined(EJB3SubsystemModel.CLUSTER_PASSIVATION_STORE)) {
            List<Property> caches = model.get(EJB3SubsystemModel.CLUSTER_PASSIVATION_STORE).asPropertyList();
            for (Property property : caches) {
                // <strict-max-pool>
                writer.writeStartElement(EJB3SubsystemXMLElement.CLUSTER_PASSIVATION_STORE.getLocalName());
                ModelNode store = property.getValue();
                writer.writeAttribute(EJB3SubsystemXMLAttribute.NAME.getLocalName(), property.getName());
                LegacyPassivationStoreResourceDefinition.IDLE_TIMEOUT.marshallAsAttribute(store, writer);
                LegacyPassivationStoreResourceDefinition.IDLE_TIMEOUT_UNIT.marshallAsAttribute(store, writer);
                ClusterPassivationStoreResourceDefinition.MAX_SIZE.marshallAsAttribute(store, writer);
                ClusterPassivationStoreResourceDefinition.CACHE_CONTAINER.marshallAsAttribute(store, writer);
                ClusterPassivationStoreResourceDefinition.BEAN_CACHE.marshallAsAttribute(store, writer);
                ClusterPassivationStoreResourceDefinition.CLIENT_MAPPINGS_CACHE.marshallAsAttribute(store, writer);
                ClusterPassivationStoreResourceDefinition.PASSIVATE_EVENTS_ON_REPLICATE.marshallAsAttribute(store, writer);
                writer.writeEndElement();
            }
        }
    }

    /**
     * Persist as a passivation-store using relevant attributes
     */
    private void writeFilePassivationStores(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
        if (model.hasDefined(EJB3SubsystemModel.FILE_PASSIVATION_STORE)) {
            List<Property> caches = model.get(EJB3SubsystemModel.FILE_PASSIVATION_STORE).asPropertyList();
            for (Property property : caches) {
                // <strict-max-pool>
                writer.writeStartElement(EJB3SubsystemXMLElement.FILE_PASSIVATION_STORE.getLocalName());
                ModelNode store = property.getValue();
                writer.writeAttribute(EJB3SubsystemXMLAttribute.NAME.getLocalName(), property.getName());
                LegacyPassivationStoreResourceDefinition.IDLE_TIMEOUT.marshallAsAttribute(store, writer);
                LegacyPassivationStoreResourceDefinition.IDLE_TIMEOUT_UNIT.marshallAsAttribute(store, writer);
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
        TimerServiceResourceDefinition.DEFAULT_DATA_STORE.marshallAsAttribute(timerServiceModel, writer);

        writer.writeStartElement(EJB3SubsystemXMLElement.DATA_STORES.getLocalName());
        writeFileDataStores(writer, timerServiceModel);
        writeDatabaseDataStores(writer, timerServiceModel);
        writer.writeEndElement();

    }

    private void writeDatabaseDataStores(final XMLExtendedStreamWriter writer, final ModelNode timerServiceModel) throws XMLStreamException {
        if (timerServiceModel.hasDefined(EJB3SubsystemModel.DATABASE_DATA_STORE)) {
            List<Property> stores = timerServiceModel.get(EJB3SubsystemModel.DATABASE_DATA_STORE).asPropertyList();
            for (Property property : stores) {
                writer.writeStartElement(EJB3SubsystemXMLElement.DATABASE_DATA_STORE.getLocalName());
                ModelNode store = property.getValue();
                writer.writeAttribute(EJB3SubsystemXMLAttribute.NAME.getLocalName(), property.getName());
                DatabaseDataStoreResourceDefinition.DATASOURCE_JNDI_NAME.marshallAsAttribute(store, writer);
                DatabaseDataStoreResourceDefinition.DATABASE.marshallAsAttribute(store, writer);
                DatabaseDataStoreResourceDefinition.PARTITION.marshallAsAttribute(store, writer);
                DatabaseDataStoreResourceDefinition.REFRESH_INTERVAL.marshallAsAttribute(store, writer);
                DatabaseDataStoreResourceDefinition.ALLOW_EXECUTION.marshallAsAttribute(store, writer);
                writer.writeEndElement();
            }
        }
    }

    private void writeFileDataStores(final XMLExtendedStreamWriter writer, final ModelNode timerServiceModel) throws XMLStreamException {
        if (timerServiceModel.hasDefined(EJB3SubsystemModel.FILE_DATA_STORE)) {
            List<Property> stores = timerServiceModel.get(EJB3SubsystemModel.FILE_DATA_STORE).asPropertyList();
            for (Property property : stores) {
                writer.writeStartElement(EJB3SubsystemXMLElement.FILE_DATA_STORE.getLocalName());
                ModelNode store = property.getValue();
                writer.writeAttribute(EJB3SubsystemXMLAttribute.NAME.getLocalName(), property.getName());
                FileDataStoreResourceDefinition.PATH.marshallAsAttribute(store, writer);
                FileDataStoreResourceDefinition.RELATIVE_TO.marshallAsAttribute(store, writer);
                writer.writeEndElement();
            }
        }
    }

    private void writeChannelCreationOptions(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(EJB3SubsystemXMLElement.CHANNEL_CREATION_OPTIONS.getLocalName());
        for (final Property optionPropertyModelNode : node.asPropertyList()) {
            writer.writeStartElement(EJB3SubsystemXMLElement.OPTION.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), optionPropertyModelNode.getName());
            final ModelNode propertyValueModelNode = optionPropertyModelNode.getValue();
            RemoteConnectorChannelCreationOptionResource.CHANNEL_CREATION_OPTION_VALUE.marshallAsAttribute(propertyValueModelNode, writer);
            RemoteConnectorChannelCreationOptionResource.CHANNEL_CREATION_OPTION_TYPE.marshallAsAttribute(propertyValueModelNode, writer);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeProfiles(final XMLExtendedStreamWriter writer, final ModelNode model) throws XMLStreamException {
        final List<Property> profiles = model.get(REMOTING_PROFILE).asPropertyList();
        for (final Property property : profiles) {
            writer.writeStartElement(PROFILE);
            writer.writeAttribute(EJB3SubsystemXMLAttribute.NAME.getLocalName(), property.getName());
            final ModelNode profileNode = property.getValue();
            RemotingProfileResourceDefinition.EXCLUDE_LOCAL_RECEIVER.marshallAsAttribute(profileNode, writer);
            RemotingProfileResourceDefinition.LOCAL_RECEIVER_PASS_BY_VALUE.marshallAsAttribute(profileNode, writer);
            if(profileNode.hasDefined(REMOTING_EJB_RECEIVER)){
                writeRemotingEjbReceivers(writer, profileNode);
            }
            StaticEJBDiscoveryDefinition.INSTANCE.marshallAsElement(profileNode, writer);
            writer.writeEndElement();
        }
    }

    private void writeRemotingEjbReceivers(final XMLExtendedStreamWriter writer, final ModelNode profileNode)
            throws XMLStreamException {
        final List<Property> receivers = profileNode.get(REMOTING_EJB_RECEIVER).asPropertyList();
        for (final Property property : receivers) {
            writer.writeStartElement(REMOTING_EJB_RECEIVER);
            writer.writeAttribute(EJB3SubsystemXMLAttribute.NAME.getLocalName(), property.getName());
            final ModelNode receiverNode = property.getValue();
            RemotingEjbReceiverDefinition.OUTBOUND_CONNECTION_REF.marshallAsAttribute(receiverNode, writer);
            RemotingEjbReceiverDefinition.CONNECT_TIMEOUT.marshallAsAttribute(receiverNode, writer);
            if (receiverNode.hasDefined(CHANNEL_CREATION_OPTIONS)) {
                writeChannelCreationOptions(writer, receiverNode.get(CHANNEL_CREATION_OPTIONS));
            }
            writer.writeEndElement();
        }
    }

    private void writeApplicationSecurityDomains(final XMLExtendedStreamWriter writer, final ModelNode model) throws XMLStreamException {
        List<Property> applicationSecurityDomains = model.get(APPLICATION_SECURITY_DOMAIN).asPropertyList();
        for (Property property : applicationSecurityDomains) {
            writeApplicationSecurityDomain(writer, property);
        }
    }

    private void writeApplicationSecurityDomain(final XMLExtendedStreamWriter writer, final Property property) throws XMLStreamException {
        writer.writeStartElement(APPLICATION_SECURITY_DOMAIN);
        writer.writeAttribute(EJB3SubsystemXMLAttribute.NAME.getLocalName(), property.getName());
        ApplicationSecurityDomainDefinition.SECURITY_DOMAIN.marshallAsAttribute(property.getValue(), writer);
        ApplicationSecurityDomainDefinition.ENABLE_JACC.marshallAsAttribute(property.getValue(), writer);
        writer.writeEndElement();
    }

    private static void writeAttribute(XMLExtendedStreamWriter writer, ModelNode model, AttributeDefinition attribute) throws XMLStreamException {
        attribute.getAttributeMarshaller().marshallAsAttribute(attribute, model, true, writer);
    }
}
