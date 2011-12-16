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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT_UNIT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.MAX_POOL_SIZE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.NAME;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.PATH;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.RELATIVE_TO;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVICE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.THREAD_POOL_NAME;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.TIMER_SERVICE;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * User: Jaikiran Pai
 */
public class EJB3Subsystem11Parser implements XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    public static final EJB3Subsystem11Parser INSTANCE = new EJB3Subsystem11Parser();

    private EJB3Subsystem11Parser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {

        context.startSubsystemElement(EJB3Extension.NAMESPACE_1_1, false);

        ModelNode model = context.getModelNode();
        if (model.hasDefined(EJB3SubsystemModel.LITE)) {
            writer.writeAttribute(EJB3SubsystemModel.LITE, model.get(EJB3SubsystemModel.LITE).asString());
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
        // write the session-bean element
        if (model.hasDefined(EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL)) {
            // <session-bean>
            writer.writeStartElement(EJB3SubsystemXMLElement.SESSION_BEAN.getLocalName());
            // <stateless>
            writer.writeStartElement(EJB3SubsystemXMLElement.STATELESS.getLocalName());
            // write out the <bean-instance-pool-ref>
            this.writeDefaultSLSBPool(writer, model);
            // </stateless>
            writer.writeEndElement();
            // </session-bean>
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
        // timer-service
        if (model.hasDefined(SERVICE) && model.get(SERVICE).hasDefined(TIMER_SERVICE)) {
            // <timer-service>
            writer.writeStartElement(EJB3SubsystemXMLElement.TIMER_SERVICE.getLocalName());
            final ModelNode timerServiceModel = model.get(SERVICE, TIMER_SERVICE);
            this.writeTimerService(writer, timerServiceModel);
            // </timer-service>
            writer.writeEndElement();
        }

        // write the subsystem end element
        writer.writeEndElement();
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
     * Writes out the <session-bean> element and its nested elements
     *
     * @param writer XML writer
     * @param model  The <session-bean> element {@link org.jboss.dmr.ModelNode}
     * @throws javax.xml.stream.XMLStreamException
     *
     */
    private void writeSessionBean(final XMLExtendedStreamWriter writer, final ModelNode model) throws XMLStreamException {
        if (model.hasDefined(EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL)) {
            // <stateless>
            writer.writeStartElement(EJB3SubsystemXMLElement.STATELESS.getLocalName());
            // contents of <stateless> element
            final ModelNode statelessBeanModeNode = model.get(EJB3SubsystemXMLElement.STATELESS.getLocalName());
            this.writeDefaultSLSBPool(writer, statelessBeanModeNode);
            // </stateless>
            writer.writeEndElement();
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

    private void writePools(final XMLExtendedStreamWriter writer, final ModelNode poolsModelNode) throws XMLStreamException {
        if (poolsModelNode.hasDefined(EJB3SubsystemXMLElement.BEAN_INSTANCE_POOLS.getLocalName())) {
            // <bean-instance-pools>
            writer.writeStartElement(EJB3SubsystemXMLElement.BEAN_INSTANCE_POOLS.getLocalName());
            // write contents of bean-instance-pools
            final ModelNode beanInstancePoolsModelNode = poolsModelNode.get(EJB3SubsystemXMLElement.BEAN_INSTANCE_POOLS.getLocalName());
            this.writeBeanInstancePools(writer, beanInstancePoolsModelNode);
            // </bean-instance-pools>
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

    private void writeTimerService(final XMLExtendedStreamWriter writer, final ModelNode timerServiceModel) throws XMLStreamException {

        writer.writeAttribute(THREAD_POOL_NAME, "default");

        // <data-store>
        if (TimerServiceResourceDefinition.PATH.isMarshallable(timerServiceModel)
                || TimerServiceResourceDefinition.RELATIVE_TO.isMarshallable(timerServiceModel)) {

            writer.writeEmptyElement(EJB3SubsystemXMLElement.DATA_STORE.getLocalName());
            TimerServiceResourceDefinition.PATH.marshallAsAttribute(timerServiceModel, writer);
            TimerServiceResourceDefinition.RELATIVE_TO.marshallAsAttribute(timerServiceModel, writer);
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

    private void parseTimerService(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        requireNoAttributes(reader);

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);
        address.add(SERVICE, TIMER_SERVICE);
        final ModelNode timerServiceAdd = new ModelNode();
        timerServiceAdd.get(OP).set(ADD);
        timerServiceAdd.get(OP_ADDR).set(address);

        Integer coreThreads = null;
        Integer maxThreads = null;
        String dataStorePath = null;
        String dataStorePathRelativeTo = null;
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case THREAD_POOL: {
                    final int count = reader.getAttributeCount();
                    for (int i = 0; i < count; i++) {
                        requireNoNamespaceAttribute(reader, i);
                        final String value = reader.getAttributeValue(i);
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

    private ModelNode createSetDefaultWriteAttributeOperation(final SimpleAttributeDefinition attributeDefinition,
                                                              final String rawValue, final XMLStreamReader reader) throws XMLStreamException {

        final ModelNode parsed = attributeDefinition.parse(rawValue, reader);
        // create /subsystem=ejb3:write-attribute(name=default-mdb-instance-pool,value=poolName) operation
        final ModelNode setDefaultSLSBPoolOperation = new ModelNode();
        setDefaultSLSBPoolOperation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        // set the address for this operation
        setDefaultSLSBPoolOperation.get(OP_ADDR).set(this.getEJB3SubsystemAddress().toModelNode());
        // set the params for the operation
        setDefaultSLSBPoolOperation.get(NAME).set(attributeDefinition.getName());
        setDefaultSLSBPoolOperation.get(VALUE).set(parsed);

        return setDefaultSLSBPoolOperation;
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
        PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME));
        return addr;
    }
}
