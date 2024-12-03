/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.ee.concurrent.ConcurrencyImplementation;
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
                            ConcurrencyImplementation.INSTANCE.parseConcurrentElement20(reader, list, subsystemPathAddress);
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
