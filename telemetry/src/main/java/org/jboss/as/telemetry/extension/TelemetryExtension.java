package org.jboss.as.telemetry.extension;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
//import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import java.util.List;

//import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;


/**
 * @author <a href="jkinlaw@redhat.com">Josh Kinlaw</a>
 */
public class TelemetryExtension implements Extension {

    protected static final String FREQUENCY = "frequency";
    protected static final String ENABLED = "enabled";
    protected static final String TYPE = "telemetryType";
    protected static final PathElement TYPE_PATH = PathElement.pathElement(TYPE);

    /**
     * The name space used for the {@code subsystem} element
     */
    public static final String NAMESPACE = "urn:org.jboss.as.telemetry:1.0";;

    /**
     * The name of our subsystem within the model.
     */
    public static final String SUBSYSTEM_NAME = "telemetry";

    /**
     * Version of the subsystem used for registering
     */
    private static final int MAJOR_VERSION = 1;

    /**
     * The parser used for parsing our subsystem
     */
    private final SubsystemParser parser = new SubsystemParser();

    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final String RESOURCE_NAME = TelemetryExtension.class.getPackage().getName() + ".LocalDescriptions";

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        String prefix = SUBSYSTEM_NAME + (keyPrefix == null ? "" : "." + keyPrefix);
        return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME, TelemetryExtension.class.getClassLoader(), true, true);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE, parser);
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, ModelVersion.create(MAJOR_VERSION));
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(TelemetrySubsystemDefinition.INSTANCE);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        subsystem.registerXMLElementWriter(parser);
    }

    /**
     * The subsystem parser, which uses stax to read and write to and from xml
     */
    private static class SubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            // Require no attributes
            ParseUtils.requireNoAttributes(reader);

            //Read the children
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                readDeploymentType(reader, list);
            }
        }

        private void readDeploymentType(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            if (!reader.getLocalName().equals(TYPE)) {
                throw ParseUtils.unexpectedElement(reader);
            }
            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ModelDescriptionConstants.ADD);

            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attr = reader.getAttributeLocalName(i);
                String value = reader.getAttributeValue(i);
                if (attr.equals(FREQUENCY)) {
                    TelemetrySubsystemDefinition.FREQUENCY.parseAndSetParameter(value, subsystem, reader);
                }
                else if (attr.equals(ENABLED)) {
                    TelemetrySubsystemDefinition.ENABLED.parseAndSetParameter(value, subsystem, reader);
                }
                else {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
            ParseUtils.requireNoContent(reader);

            //Add the 'add' operation
            PathAddress addr = PathAddress.pathAddress(SUBSYSTEM_PATH);
            subsystem.get(OP_ADDR).set(addr.toModelNode());
            list.add(subsystem);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(TelemetryExtension.NAMESPACE, false);
            ModelNode node = context.getModelNode();
            writer.writeStartElement(TYPE);
            TelemetrySubsystemDefinition.FREQUENCY.marshallAsAttribute(node, true, writer);
            TelemetrySubsystemDefinition.ENABLED.marshallAsAttribute(node, true, writer);
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }
}
