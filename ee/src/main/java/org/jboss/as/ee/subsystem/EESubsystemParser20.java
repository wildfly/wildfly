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

package org.jboss.as.ee.subsystem;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.*;

/**
 */
class EESubsystemParser20 implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    EESubsystemParser20() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        // EE subsystem doesn't have any attributes, so make sure that the xml doesn't have any
        requireNoAttributes(reader);
        final PathAddress subsystemPathAddress = PathAddress.pathAddress(EeExtension.PATH_SUBSYSTEM);
        final ModelNode eeSubSystem = Util.createAddOperation(subsystemPathAddress);
        // add the subsystem to the ModelNode(s)
        list.add(eeSubSystem);

        // elements
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case EE_2_0:
                case EE_3_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (!encountered.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    switch (element) {
                        case GLOBAL_MODULES: {
                            final ModelNode model = parseGlobalModules(reader);
                            eeSubSystem.get(GlobalModulesDefinition.GLOBAL_MODULES).set(model);
                            break;
                        }
                        case EAR_SUBDEPLOYMENTS_ISOLATED: {
                            final String earSubDeploymentsIsolated = parseEarSubDeploymentsIsolatedElement(reader);
                            // set the ear subdeployment isolation on the subsystem operation
                            EeSubsystemRootResource.EAR_SUBDEPLOYMENTS_ISOLATED.parseAndSetParameter(earSubDeploymentsIsolated, eeSubSystem, reader);
                            break;
                        }
                        case SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT: {
                            final String enabled = parseSpecDescriptorPropertyReplacement(reader);
                            EeSubsystemRootResource.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT.parseAndSetParameter(enabled, eeSubSystem, reader);
                            break;
                        }
                        case JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT: {
                            final String enabled = parseJBossDescriptorPropertyReplacement(reader);
                            EeSubsystemRootResource.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT.parseAndSetParameter(enabled, eeSubSystem, reader);
                            break;
                        }
                        case ANNOTATION_PROPERTY_REPLACEMENT: {
                            final String enabled = parseEJBAnnotationPropertyReplacement(reader);
                            EeSubsystemRootResource.ANNOTATION_PROPERTY_REPLACEMENT.parseAndSetParameter(enabled, eeSubSystem, reader);
                            break;
                        }
                        case CONCURRENT: {
                            parseConcurrent(reader, list, subsystemPathAddress);
                            break;
                        }
                        case DEFAULT_BINDINGS: {
                            parseDefaultBindings(reader, list, subsystemPathAddress);
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

    static ModelNode parseGlobalModules(XMLExtendedStreamReader reader) throws XMLStreamException {

        ModelNode globalModules = new ModelNode();

        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Element.forName(reader.getLocalName())) {
                case MODULE: {
                    final ModelNode module = new ModelNode();
                    final int count = reader.getAttributeCount();
                    String name = null;
                    String slot = null;
                    String annotations = null;
                    String metaInf = null;
                    String services = null;
                    for (int i = 0; i < count; i++) {
                        requireNoNamespaceAttribute(reader, i);
                        final String value = reader.getAttributeValue(i);
                        final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                            case NAME:
                                if (name != null) {
                                    throw unexpectedAttribute(reader, i);
                                }
                                name = value;
                                GlobalModulesDefinition.NAME_AD.parseAndSetParameter(name, module, reader);
                                break;
                            case SLOT:
                                if (slot != null) {
                                    throw unexpectedAttribute(reader, i);
                                }
                                slot = value;
                                GlobalModulesDefinition.SLOT_AD.parseAndSetParameter(slot, module, reader);
                                break;
                            case ANNOTATIONS:
                                if (annotations != null) {
                                    throw unexpectedAttribute(reader, i);
                                }
                                annotations = value;
                                GlobalModulesDefinition.ANNOTATIONS_AD.parseAndSetParameter(annotations, module, reader);
                                break;

                            case SERVICES:
                                if (services != null) {
                                    throw unexpectedAttribute(reader, i);
                                }
                                services = value;
                                GlobalModulesDefinition.SERVICES_AD.parseAndSetParameter(services, module, reader);
                                break;

                            case META_INF:
                                if (metaInf != null) {
                                    throw unexpectedAttribute(reader, i);
                                }
                                metaInf = value;
                                GlobalModulesDefinition.META_INF_AD.parseAndSetParameter(metaInf, module, reader);
                                break;
                            default:
                                throw unexpectedAttribute(reader, i);
                        }
                    }
                    if (name == null) {
                        throw missingRequired(reader, Collections.singleton(NAME));
                    }

                    globalModules.add(module);

                    requireNoContent(reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return globalModules;
    }

    static String parseEarSubDeploymentsIsolatedElement(XMLExtendedStreamReader reader) throws XMLStreamException {

        // we don't expect any attributes for this element.
        requireNoAttributes(reader);

        final String value = reader.getElementText();
        if (value == null || value.trim().isEmpty()) {
            throw EeLogger.ROOT_LOGGER.invalidValue(value, Element.EAR_SUBDEPLOYMENTS_ISOLATED.getLocalName(), reader.getLocation());
        }
        return value.trim();
    }


    static String parseSpecDescriptorPropertyReplacement(XMLExtendedStreamReader reader) throws XMLStreamException {

        // we don't expect any attributes for this element.
        requireNoAttributes(reader);

        final String value = reader.getElementText();
        if (value == null || value.trim().isEmpty()) {
            throw EeLogger.ROOT_LOGGER.invalidValue(value, Element.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT.getLocalName(), reader.getLocation());
        }
        return value.trim();
    }


    static String parseJBossDescriptorPropertyReplacement(XMLExtendedStreamReader reader) throws XMLStreamException {

        // we don't expect any attributes for this element.
        requireNoAttributes(reader);

        final String value = reader.getElementText();
        if (value == null || value.trim().isEmpty()) {
            throw EeLogger.ROOT_LOGGER.invalidValue(value, Element.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT.getLocalName(), reader.getLocation());
        }
        return value.trim();
    }

    static String parseEJBAnnotationPropertyReplacement(XMLExtendedStreamReader reader) throws XMLStreamException {
        // we don't expect any attributes for this element.
        requireNoAttributes(reader);

        final String value = reader.getElementText();
        return value.trim();
    }

    static void parseConcurrent(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CONTEXT_SERVICES: {
                    parseContextServices(reader, operations, subsystemPathAddress);
                    break;
                }
                case MANAGED_THREAD_FACTORIES: {
                    parseManagedThreadFactories(reader, operations, subsystemPathAddress);
                    break;
                }
                case MANAGED_EXECUTOR_SERVICES: {
                    parseManagedExecutorServices(reader, operations, subsystemPathAddress);
                    break;
                }
                case MANAGED_SCHEDULED_EXECUTOR_SERVICES: {
                    parseManagedScheduledExecutorServices(reader, operations, subsystemPathAddress);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    static void parseContextServices(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        requireNoAttributes(reader);
        boolean empty = true;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Element.forName(reader.getLocalName())) {
                case CONTEXT_SERVICE: {
                    empty = false;
                    parseContextService(reader, operations, subsystemPathAddress);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if(empty) {
            throw missingRequired(reader,  EnumSet.of(Element.CONTEXT_SERVICE));
        }
    }

    static void parseContextService(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        final ModelNode addOperation = Util.createAddOperation();
        final int count = reader.getAttributeCount();
        String name = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.JNDI_NAME);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = value.trim();
                    break;
                case JNDI_NAME:
                    ContextServiceResourceDefinition.JNDI_NAME_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case USE_TRANSACTION_SETUP_PROVIDER:
                    ContextServiceResourceDefinition.USE_TRANSACTION_SETUP_PROVIDER_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        requireNoContent(reader);
        final PathAddress address = subsystemPathAddress.append(EESubsystemModel.CONTEXT_SERVICE, name);
        addOperation.get(OP_ADDR).set(address.toModelNode());
        operations.add(addOperation);
    }

    static void parseManagedThreadFactories(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        requireNoAttributes(reader);
        boolean empty = true;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Element.forName(reader.getLocalName())) {
                case MANAGED_THREAD_FACTORY: {
                    empty = false;
                    parseManagedThreadFactory(reader, operations, subsystemPathAddress);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if(empty) {
            throw missingRequired(reader,  EnumSet.of(Element.MANAGED_THREAD_FACTORY));
        }
    }

    static void parseManagedThreadFactory(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        final ModelNode addOperation = Util.createAddOperation();
        final int count = reader.getAttributeCount();
        String name = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.JNDI_NAME);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = value.trim();
                    break;
                case JNDI_NAME:
                    ManagedThreadFactoryResourceDefinition.JNDI_NAME_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case CONTEXT_SERVICE:
                    ManagedThreadFactoryResourceDefinition.CONTEXT_SERVICE_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case PRIORITY:
                    ManagedThreadFactoryResourceDefinition.PRIORITY_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        requireNoContent(reader);
        final PathAddress address = subsystemPathAddress.append(EESubsystemModel.MANAGED_THREAD_FACTORY, name);
        addOperation.get(OP_ADDR).set(address.toModelNode());
        operations.add(addOperation);
    }

    static void parseManagedExecutorServices(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        requireNoAttributes(reader);
        boolean empty = true;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Element.forName(reader.getLocalName())) {
                case MANAGED_EXECUTOR_SERVICE: {
                    empty = false;
                    parseManagedExecutorService(reader, operations, subsystemPathAddress);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if(empty) {
            throw missingRequired(reader,  EnumSet.of(Element.MANAGED_EXECUTOR_SERVICE));
        }
    }

    static void parseManagedExecutorService(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        final ModelNode addOperation = Util.createAddOperation();
        final int count = reader.getAttributeCount();
        String name = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.JNDI_NAME, Attribute.CORE_THREADS);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = value.trim();
                    break;
                case JNDI_NAME:
                    ManagedExecutorServiceResourceDefinition.JNDI_NAME_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case CONTEXT_SERVICE:
                    ManagedExecutorServiceResourceDefinition.CONTEXT_SERVICE_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case THREAD_FACTORY:
                    ManagedScheduledExecutorServiceResourceDefinition.THREAD_FACTORY_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case HUNG_TASK_THRESHOLD:
                    ManagedExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case LONG_RUNNING_TASKS:
                    ManagedExecutorServiceResourceDefinition.LONG_RUNNING_TASKS_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case CORE_THREADS:
                    ManagedExecutorServiceResourceDefinition.CORE_THREADS_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case MAX_THREADS:
                    ManagedExecutorServiceResourceDefinition.MAX_THREADS_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case KEEPALIVE_TIME:
                    ManagedExecutorServiceResourceDefinition.KEEPALIVE_TIME_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case QUEUE_LENGTH:
                    ManagedExecutorServiceResourceDefinition.QUEUE_LENGTH_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case REJECT_POLICY:
                    ManagedExecutorServiceResourceDefinition.REJECT_POLICY_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        requireNoContent(reader);
        final PathAddress address = subsystemPathAddress.append(EESubsystemModel.MANAGED_EXECUTOR_SERVICE, name);
        addOperation.get(OP_ADDR).set(address.toModelNode());
        operations.add(addOperation);
    }

    static void parseManagedScheduledExecutorServices(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        requireNoAttributes(reader);
        boolean empty = true;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Element.forName(reader.getLocalName())) {
                case MANAGED_SCHEDULED_EXECUTOR_SERVICE: {
                    empty = false;
                    parseManagedScheduledExecutorService(reader, operations, subsystemPathAddress);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if(empty) {
            throw missingRequired(reader,  EnumSet.of(Element.MANAGED_SCHEDULED_EXECUTOR_SERVICE));
        }
    }

    static void parseManagedScheduledExecutorService(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        final ModelNode addOperation = Util.createAddOperation();
        final int count = reader.getAttributeCount();
        String name = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.JNDI_NAME, Attribute.CORE_THREADS);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = value.trim();
                    break;
                case JNDI_NAME:
                    ManagedScheduledExecutorServiceResourceDefinition.JNDI_NAME_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case CONTEXT_SERVICE:
                    ManagedScheduledExecutorServiceResourceDefinition.CONTEXT_SERVICE_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case THREAD_FACTORY:
                    ManagedScheduledExecutorServiceResourceDefinition.THREAD_FACTORY_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case HUNG_TASK_THRESHOLD:
                    ManagedScheduledExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case LONG_RUNNING_TASKS:
                    ManagedScheduledExecutorServiceResourceDefinition.LONG_RUNNING_TASKS_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case CORE_THREADS:
                    ManagedScheduledExecutorServiceResourceDefinition.CORE_THREADS_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case KEEPALIVE_TIME:
                    ManagedScheduledExecutorServiceResourceDefinition.KEEPALIVE_TIME_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case REJECT_POLICY:
                    ManagedScheduledExecutorServiceResourceDefinition.REJECT_POLICY_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        requireNoContent(reader);
        final PathAddress address = subsystemPathAddress.append(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE, name);
        addOperation.get(OP_ADDR).set(address.toModelNode());
        operations.add(addOperation);
    }

    static void parseDefaultBindings(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        final ModelNode addOperation = Util.createAddOperation();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CONTEXT_SERVICE:
                    DefaultBindingsResourceDefinition.CONTEXT_SERVICE_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case DATASOURCE:
                    DefaultBindingsResourceDefinition.DATASOURCE_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case JMS_CONNECTION_FACTORY:
                    DefaultBindingsResourceDefinition.JMS_CONNECTION_FACTORY_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case MANAGED_EXECUTOR_SERVICE:
                    DefaultBindingsResourceDefinition.MANAGED_EXECUTOR_SERVICE_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case MANAGED_SCHEDULED_EXECUTOR_SERVICE:
                    DefaultBindingsResourceDefinition.MANAGED_SCHEDULED_EXECUTOR_SERVICE_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case MANAGED_THREAD_FACTORY:
                    DefaultBindingsResourceDefinition.MANAGED_THREAD_FACTORY_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        final PathAddress address = subsystemPathAddress.append(EESubsystemModel.DEFAULT_BINDINGS_PATH);
        addOperation.get(OP_ADDR).set(address.toModelNode());
        operations.add(addOperation);
    }
}
