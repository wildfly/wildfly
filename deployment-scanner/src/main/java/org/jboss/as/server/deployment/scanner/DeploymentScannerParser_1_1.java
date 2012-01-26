package org.jboss.as.server.deployment.scanner;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Collections;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

/**
 */
class DeploymentScannerParser_1_1 implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    public static final DeploymentScannerParser_1_1 INSTANCE = new DeploymentScannerParser_1_1();

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode scanners = context.getModelNode();
        for (final Property list : scanners.asPropertyList()) {
            final ModelNode node = list.getValue();

            for (final Property scanner : node.asPropertyList()) {

                writer.writeEmptyElement(Element.DEPLOYMENT_SCANNER.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), scanner.getName());
                ModelNode configuration = scanner.getValue();
                if (configuration.hasDefined(CommonAttributes.PATH)) {
                    writer.writeAttribute(Attribute.PATH.getLocalName(), configuration.get(CommonAttributes.PATH)
                            .asString());
                }
                if (configuration.hasDefined(CommonAttributes.SCAN_ENABLED)) {
                    writer.writeAttribute(Attribute.SCAN_ENABLED.getLocalName(),
                            configuration.get(CommonAttributes.SCAN_ENABLED).asString());
                }
                if (configuration.hasDefined(CommonAttributes.SCAN_INTERVAL)) {
                    writer.writeAttribute(Attribute.SCAN_INTERVAL.getLocalName(),
                            configuration.get(CommonAttributes.SCAN_INTERVAL).asString());
                }
                if (configuration.hasDefined(CommonAttributes.RELATIVE_TO)) {
                    writer.writeAttribute(Attribute.RELATIVE_TO.getLocalName(),
                            configuration.get(CommonAttributes.RELATIVE_TO).asString());
                }
                if (configuration.hasDefined(CommonAttributes.AUTO_DEPLOY_ZIPPED)) {
                    if (!configuration.get(CommonAttributes.AUTO_DEPLOY_ZIPPED).asBoolean()) {
                        writer.writeAttribute(Attribute.AUTO_DEPLOY_ZIPPED.getLocalName(), Boolean.FALSE.toString());
                    }
                }
                if (configuration.hasDefined(CommonAttributes.AUTO_DEPLOY_EXPLODED)) {
                    if (configuration.get(CommonAttributes.AUTO_DEPLOY_EXPLODED).asBoolean()) {
                        writer.writeAttribute(Attribute.AUTO_DEPLOY_EXPLODED.getLocalName(), Boolean.TRUE.toString());
                    }
                }
                if (configuration.hasDefined(CommonAttributes.AUTO_DEPLOY_XML)) {
                    if (configuration.get(CommonAttributes.AUTO_DEPLOY_XML).asBoolean()) {
                        writer.writeAttribute(Attribute.AUTO_DEPLOY_XML.getLocalName(), Boolean.TRUE.toString());
                    }
                }
                if (configuration.hasDefined(CommonAttributes.DEPLOYMENT_TIMEOUT)) {
                    writer.writeAttribute(Attribute.DEPLOYMENT_TIMEOUT.getLocalName(), configuration.get(CommonAttributes.DEPLOYMENT_TIMEOUT).asString());
                }
            }
            writer.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        // no attributes
        requireNoAttributes(reader);

        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, DeploymentScannerExtension.SUBSYSTEM_NAME);
        address.protect();

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);
        list.add(subsystem);

        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DEPLOYMENT_SCANNER_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case DEPLOYMENT_SCANNER: {
                            //noinspection unchecked
                            parseScanner(reader, address, list);
                            break;
                        }
                        default:
                            throw unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    void parseScanner(XMLExtendedStreamReader reader, final ModelNode address, List<ModelNode> list) throws XMLStreamException {
        // Handle attributes

        String name = DeploymentScannerExtension.DEFAULT_SCANNER_NAME;
        String path = null;
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        final int attrCount = reader.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PATH: {
                    path = value;
                    DeploymentScannerDefinition.PATH.parseAndSetParameter(value,operation,reader);
                    break;
                }
                case NAME: {
                    name = DeploymentScannerDefinition.NAME.parse(value,reader).asString();
                    break;
                }
                case RELATIVE_TO: {
                    DeploymentScannerDefinition.RELATIVE_TO.parseAndSetParameter(value,operation,reader);
                    break;
                }
                case SCAN_INTERVAL: {
                    DeploymentScannerDefinition.SCAN_INTERVAL.parseAndSetParameter(value,operation,reader);
                    break;
                }
                case SCAN_ENABLED: {
                    DeploymentScannerDefinition.SCAN_ENABLED.parseAndSetParameter(value,operation,reader);
                    break;
                }
                case AUTO_DEPLOY_ZIPPED: {
                    DeploymentScannerDefinition.AUTO_DEPLOY_ZIPPED.parseAndSetParameter(value,operation,reader);
                    break;
                }
                case AUTO_DEPLOY_EXPLODED: {
                    DeploymentScannerDefinition.AUTO_DEPLOY_EXPLODED.parseAndSetParameter(value,operation,reader);
                    break;
                }
                case AUTO_DEPLOY_XML: {
                    DeploymentScannerDefinition.AUTO_DEPLOY_XML.parseAndSetParameter(value,operation,reader);
                    break;
                }
                case DEPLOYMENT_TIMEOUT: {
                    DeploymentScannerDefinition.DEPLOYMENT_TIMEOUT.parseAndSetParameter(value,operation,reader);
                    break;
                }
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton(CommonAttributes.NAME));
        }
        if (path == null) {
            ParseUtils.missingRequired(reader, Collections.singleton(CommonAttributes.PATH));
        }
        requireNoContent(reader);
        operation.get(OP_ADDR).set(address).add(CommonAttributes.SCANNER, name);
        list.add(operation);
    }

}
