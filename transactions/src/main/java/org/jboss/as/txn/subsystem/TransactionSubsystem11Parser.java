package org.jboss.as.txn.subsystem;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateNamedElement;
import static org.jboss.as.controller.parsing.ParseUtils.missingOneOf;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

/**
 */
class TransactionSubsystem11Parser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    public static final TransactionSubsystem11Parser INSTANCE = new TransactionSubsystem11Parser();

    private TransactionSubsystem11Parser() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        // no attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }

        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, TransactionExtension.SUBSYSTEM_NAME);
        address.protect();

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);

        list.add(subsystem);

        // elements
        final EnumSet<Element> required = EnumSet.of(Element.RECOVERY_ENVIRONMENT, Element.CORE_ENVIRONMENT);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case TRANSACTIONS_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    required.remove(element);
                    if (!encountered.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    switch (element) {
                        case RECOVERY_ENVIRONMENT: {
                            parseRecoveryEnvironmentElement(reader, subsystem);
                            break;
                        }
                        case CORE_ENVIRONMENT: {
                            parseCoreEnvironmentElement(reader, subsystem);
                            break;
                        }
                        case COORDINATOR_ENVIRONMENT: {
                            parseCoordinatorEnvironmentElement(reader, subsystem);
                            break;
                        }
                        case OBJECT_STORE: {
                            parseObjectStoreEnvironmentElementAndEnrichOperation(reader, subsystem);
                            break;
                        }
                        case JTS: {
                            parseJts(reader, subsystem);
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
        if (!required.isEmpty()) {
            throw missingRequiredElement(reader, required);
        }
        final ModelNode logStoreAddress = address.clone();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        logStoreAddress.add(LogStoreConstans.LOG_STORE, LogStoreConstans.LOG_STORE);

        logStoreAddress.protect();

        operation.get(OP_ADDR).set(logStoreAddress);
        list.add(operation);
    }

    private void parseJts(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {
        operation.get(CommonAttributes.JTS).set(true);
        requireNoContent(reader);
    }

    static void parseObjectStoreEnvironmentElementAndEnrichOperation(final XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case RELATIVE_TO:
                    TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO.parseAndSetParameter(value, operation, reader);
                    break;
                case PATH:
                    TransactionSubsystemRootResourceDefinition.OBJECT_STORE_PATH.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        // Handle elements
        requireNoContent(reader);

    }

    static void parseCoordinatorEnvironmentElement(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLE_STATISTICS:
                    TransactionSubsystemRootResourceDefinition.ENABLE_STATISTICS.parseAndSetParameter(value, operation, reader);
                    break;
                case ENABLE_TSM_STATUS:
                    TransactionSubsystemRootResourceDefinition.ENABLE_TSM_STATUS.parseAndSetParameter(value, operation, reader);
                    break;
                case DEFAULT_TIMEOUT:
                    TransactionSubsystemRootResourceDefinition.DEFAULT_TIMEOUT.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        // Handle elements
        requireNoContent(reader);

    }

    /**
     * Handle the core-environment element and children
     *
     * @param reader
     * @return ModelNode for the core-environment
     * @throws javax.xml.stream.XMLStreamException
     *
     */
    static void parseCoreEnvironmentElement(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NODE_IDENTIFIER:
                    TransactionSubsystemRootResourceDefinition.NODE_IDENTIFIER.parseAndSetParameter(value, operation, reader);
                    break;
                case PATH:
                    TransactionSubsystemRootResourceDefinition.PATH.parseAndSetParameter(value, operation, reader);
                    break;
                case RELATIVE_TO:
                    TransactionSubsystemRootResourceDefinition.RELATIVE_TO.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        // elements
        final EnumSet<Element> required = EnumSet.of(Element.PROCESS_ID);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            required.remove(element);
            switch (element) {
                case PROCESS_ID: {
                    if (!encountered.add(element)) {
                        throw duplicateNamedElement(reader, reader.getLocalName());
                    }
                    parseProcessIdEnvironmentElement(reader, operation);
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequiredElement(reader, required);
        }
    }

    /**
     * Handle the process-id child elements
     *
     * @param reader
     * @param coreEnvironmentAdd
     * @return
     * @throws javax.xml.stream.XMLStreamException
     *
     */
    static void parseProcessIdEnvironmentElement(XMLExtendedStreamReader reader, ModelNode coreEnvironmentAdd) throws XMLStreamException {

        // elements
        boolean encountered = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case UUID:
                    if (encountered) {
                        throw unexpectedElement(reader);
                    }
                    encountered = true;
                    coreEnvironmentAdd.get(TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName()).set(true);
                    requireNoContent(reader);
                    break;
                case SOCKET: {
                    if (encountered) {
                        throw unexpectedElement(reader);
                    }
                    encountered = true;
                    parseSocketProcessIdElement(reader, coreEnvironmentAdd);
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }

        if (!encountered) {
            throw missingOneOf(reader, EnumSet.of(Element.UUID, Element.SOCKET));
        }
    }

    static void parseSocketProcessIdElement(XMLExtendedStreamReader reader, ModelNode coreEnvironmentAdd) throws XMLStreamException {

        final int count = reader.getAttributeCount();
        final EnumSet<Attribute> required = EnumSet.of(Attribute.BINDING);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case BINDING:
                    TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.parseAndSetParameter(value, coreEnvironmentAdd, reader);
                    break;
                case SOCKET_PROCESS_ID_MAX_PORTS:
                    TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.parseAndSetParameter(value, coreEnvironmentAdd, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        // Handle elements
        requireNoContent(reader);
    }

    static void parseRecoveryEnvironmentElement(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {

        Set<Attribute> required = EnumSet.of(Attribute.BINDING, Attribute.STATUS_BINDING);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case BINDING:
                    TransactionSubsystemRootResourceDefinition.BINDING.parseAndSetParameter(value, operation, reader);
                    break;
                case STATUS_BINDING:
                    TransactionSubsystemRootResourceDefinition.STATUS_BINDING.parseAndSetParameter(value, operation, reader);
                    break;
                case RECOVERY_LISTENER:
                    TransactionSubsystemRootResourceDefinition.RECOVERY_LISTENER.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    unexpectedAttribute(reader, i);
            }
        }

        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        // Handle elements
        requireNoContent(reader);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {

        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = context.getModelNode();


        writer.writeStartElement(Element.CORE_ENVIRONMENT.getLocalName());

        TransactionSubsystemRootResourceDefinition.NODE_IDENTIFIER.marshallAsAttribute(node, writer);
        TransactionSubsystemRootResourceDefinition.PATH.marshallAsAttribute(node, writer);
        TransactionSubsystemRootResourceDefinition.RELATIVE_TO.marshallAsAttribute(node, writer);

        writeProcessId(writer, node);

        writer.writeEndElement();

        if (TransactionSubsystemRootResourceDefinition.BINDING.isMarshallable(node) ||
                TransactionSubsystemRootResourceDefinition.STATUS_BINDING.isMarshallable(node) ||
                TransactionSubsystemRootResourceDefinition.RECOVERY_LISTENER.isMarshallable(node)) {
            writer.writeStartElement(Element.RECOVERY_ENVIRONMENT.getLocalName());
            TransactionSubsystemRootResourceDefinition.BINDING.marshallAsAttribute(node, writer);

            TransactionSubsystemRootResourceDefinition.STATUS_BINDING.marshallAsAttribute(node, writer);

            TransactionSubsystemRootResourceDefinition.RECOVERY_LISTENER.marshallAsAttribute(node, writer);

            writer.writeEndElement();
        }
        if (TransactionSubsystemRootResourceDefinition.ENABLE_STATISTICS.isMarshallable(node)
                || TransactionSubsystemRootResourceDefinition.ENABLE_TSM_STATUS.isMarshallable(node)
                || TransactionSubsystemRootResourceDefinition.DEFAULT_TIMEOUT.isMarshallable(node)) {

            writer.writeStartElement(Element.COORDINATOR_ENVIRONMENT.getLocalName());

            TransactionSubsystemRootResourceDefinition.ENABLE_STATISTICS.marshallAsAttribute(node, writer);
            TransactionSubsystemRootResourceDefinition.ENABLE_TSM_STATUS.marshallAsAttribute(node, writer);
            TransactionSubsystemRootResourceDefinition.DEFAULT_TIMEOUT.marshallAsAttribute(node, writer);

            writer.writeEndElement();
        }

        if (TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO.isMarshallable(node)
                || TransactionSubsystemRootResourceDefinition.OBJECT_STORE_PATH.isMarshallable(node)) {
            writer.writeStartElement(Element.OBJECT_STORE.getLocalName());
            TransactionSubsystemRootResourceDefinition.OBJECT_STORE_PATH.marshallAsAttribute(node, writer);
            TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO.marshallAsAttribute(node, writer);
            writer.writeEndElement();
        }

        if(node.hasDefined(CommonAttributes.JTS) && node.get(CommonAttributes.JTS).asBoolean()) {
            writer.writeStartElement(Element.JTS.getLocalName());
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    private void writeProcessId(final XMLExtendedStreamWriter writer, final ModelNode value) throws XMLStreamException {
        writer.writeStartElement(Element.PROCESS_ID.getLocalName());
        if (value.get(TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName()).asBoolean()) {
            writer.writeEmptyElement(Element.UUID.getLocalName());
        } else {
            writer.writeStartElement(Element.SOCKET.getLocalName());
            TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.marshallAsAttribute(value, writer);
            TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.marshallAsAttribute(value, writer);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }
}
