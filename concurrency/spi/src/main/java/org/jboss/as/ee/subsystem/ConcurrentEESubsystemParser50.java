/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;
import java.util.EnumSet;
import java.util.List;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

/**
 * The parser for the 'concurrent' XML element EE Subsystem 5.0 configuration.
 * @author emartins
 */
public class ConcurrentEESubsystemParser50 {
    private ConcurrentEESubsystemParser50() {
    }
    public static void parseConcurrent(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final ConcurrentElement element = ConcurrentElement.forName(reader.getLocalName());
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
            switch (ConcurrentElement.forName(reader.getLocalName())) {
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
        if (empty) {
            throw missingRequired(reader, EnumSet.of(ConcurrentElement.CONTEXT_SERVICE));
        }
    }

    static void parseContextService(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        final ModelNode addOperation = Util.createAddOperation();
        final int count = reader.getAttributeCount();
        String name = null;
        final EnumSet<ConcurrentAttribute> required = EnumSet.of(ConcurrentAttribute.NAME, ConcurrentAttribute.JNDI_NAME);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final ConcurrentAttribute attribute = ConcurrentAttribute.forName(reader.getAttributeLocalName(i));
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

    static void parseManagedExecutorServices(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        requireNoAttributes(reader);
        boolean empty = true;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (ConcurrentElement.forName(reader.getLocalName())) {
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
        if (empty) {
            throw missingRequired(reader, EnumSet.of(ConcurrentElement.MANAGED_EXECUTOR_SERVICE));
        }
    }

    static void parseManagedExecutorService(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        final ModelNode addOperation = Util.createAddOperation();
        final int count = reader.getAttributeCount();
        String name = null;
        final EnumSet<ConcurrentAttribute> required = EnumSet.of(ConcurrentAttribute.NAME, ConcurrentAttribute.JNDI_NAME);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final ConcurrentAttribute attribute = ConcurrentAttribute.forName(reader.getAttributeLocalName(i));
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
                    ManagedExecutorServiceResourceDefinition.THREAD_FACTORY_AD.parseAndSetParameter(value, addOperation, reader);
                    break;
                case THREAD_PRIORITY:
                    ManagedExecutorServiceResourceDefinition.THREAD_PRIORITY_AD.parseAndSetParameter(value, addOperation, reader);
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
            switch (ConcurrentElement.forName(reader.getLocalName())) {
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
        if (empty) {
            throw missingRequired(reader, EnumSet.of(ConcurrentElement.MANAGED_SCHEDULED_EXECUTOR_SERVICE));
        }
    }

    static void parseManagedScheduledExecutorService(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        final ModelNode addOperation = Util.createAddOperation();
        final int count = reader.getAttributeCount();
        String name = null;
        final EnumSet<ConcurrentAttribute> required = EnumSet.of(ConcurrentAttribute.NAME, ConcurrentAttribute.JNDI_NAME);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final ConcurrentAttribute attribute = ConcurrentAttribute.forName(reader.getAttributeLocalName(i));
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
                case THREAD_PRIORITY:
                    ManagedScheduledExecutorServiceResourceDefinition.THREAD_PRIORITY_AD.parseAndSetParameter(value, addOperation, reader);
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

    static void parseManagedThreadFactories(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        requireNoAttributes(reader);
        boolean empty = true;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (ConcurrentElement.forName(reader.getLocalName())) {
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
        if (empty) {
            throw missingRequired(reader, EnumSet.of(ConcurrentElement.MANAGED_THREAD_FACTORY));
        }
    }

    static void parseManagedThreadFactory(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        final ModelNode addOperation = Util.createAddOperation();
        final int count = reader.getAttributeCount();
        String name = null;
        final EnumSet<ConcurrentAttribute> required = EnumSet.of(ConcurrentAttribute.NAME, ConcurrentAttribute.JNDI_NAME);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final ConcurrentAttribute attribute = ConcurrentAttribute.forName(reader.getAttributeLocalName(i));
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
}
