package org.jboss.as.modcluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.readProperty;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.modcluster.CommonAttributes.ADVERTISE;
import static org.jboss.as.modcluster.CommonAttributes.ADVERTISE_SECURITY_KEY;
import static org.jboss.as.modcluster.CommonAttributes.ADVERTISE_SOCKET;
import static org.jboss.as.modcluster.CommonAttributes.AUTO_ENABLE_CONTEXTS;
import static org.jboss.as.modcluster.CommonAttributes.CAPACITY;
import static org.jboss.as.modcluster.CommonAttributes.CLASS;
import static org.jboss.as.modcluster.CommonAttributes.CUSTOM_LOAD_METRIC;
import static org.jboss.as.modcluster.CommonAttributes.DECAY;
import static org.jboss.as.modcluster.CommonAttributes.DYNAMIC_LOAD_PROVIDER;
import static org.jboss.as.modcluster.CommonAttributes.EXCLUDED_CONTEXTS;
import static org.jboss.as.modcluster.CommonAttributes.FACTOR;
import static org.jboss.as.modcluster.CommonAttributes.HISTORY;
import static org.jboss.as.modcluster.CommonAttributes.LOAD_METRIC;
import static org.jboss.as.modcluster.CommonAttributes.LOAD_PROVIDER;
import static org.jboss.as.modcluster.CommonAttributes.MOD_CLUSTER_CONFIG;
import static org.jboss.as.modcluster.CommonAttributes.PROXY_LIST;
import static org.jboss.as.modcluster.CommonAttributes.PROXY_URL;
import static org.jboss.as.modcluster.CommonAttributes.SIMPLE_LOAD_PROVIDER;
import static org.jboss.as.modcluster.CommonAttributes.SOCKET_TIMEOUT;
import static org.jboss.as.modcluster.CommonAttributes.SSL;
import static org.jboss.as.modcluster.CommonAttributes.STOP_CONTEXT_TIMEOUT;
import static org.jboss.as.modcluster.CommonAttributes.TYPE;
import static org.jboss.as.modcluster.CommonAttributes.WEIGHT;

import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import org.jboss.dmr.Property;

public class ModClusterSubsystemElementParser implements XMLElementReader<List<ModelNode>>,
XMLElementWriter<SubsystemMarshallingContext>, XMLStreamConstants {

    /** {@inheritDoc} */
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        ParseUtils.requireNoAttributes(reader);

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, ModClusterExtension.SUBSYSTEM_NAME);
        address.protect();

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);
        list.add(subsystem);

        // Reads it
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case MODCLUSTER: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case MOD_CLUSTER_CONFIG:
                            final ModelNode config  = parseModClusterConfig(reader);
                            subsystem.get(MOD_CLUSTER_CONFIG).set(config);
                            break;
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                } default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context)
    throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = context.getModelNode();
        writeModClusterConfig(writer, node);
        writer.writeEndElement();
    }

    static void writeModClusterConfig(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.MOD_CLUSTER_CONFIG.getLocalName());
        // write Attributes
        writePropConf(writer, config);

        // write the elements.
        if (config.hasDefined(SIMPLE_LOAD_PROVIDER)) {
            writeSimpleLoadProvider(writer, config.get(SIMPLE_LOAD_PROVIDER));
        }
        if (config.hasDefined(DYNAMIC_LOAD_PROVIDER)) {
            writeDynamicLoadProvider(writer, config.get(LOAD_PROVIDER));
        }
        if (config.hasDefined(SSL)) {
            writeSSL(writer, config.get(SSL));
        }
        writer.writeEndElement();
    }

    static ModelNode parseModClusterConfig(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode config = new ModelNode();
        // Parse the attributes.
        parsePropConf(reader, config);
        // Parse the elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SIMPLE_LOAD_PROVIDER:
                    final ModelNode load = parseSimpleLoadProvider(reader);
                    config.get(SIMPLE_LOAD_PROVIDER).set(load);
                    break;
                case DYNAMIC_LOAD_PROVIDER:
                    final ModelNode dynload = parseDynamicLoadProvider(reader);
                    config.get(DYNAMIC_LOAD_PROVIDER).set(dynload);
                    break;
                case SSL:
                    final ModelNode ssl = parseSSL(reader);
                    config.get(SSL).set(ssl);
                    break;
                default:
                    unexpectedElement(reader);
            }
        }
        return config;
    }

    /* prop-confType */
    static void writePropConf(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writeAttribute(writer, ADVERTISE_SOCKET, config);
        writeAttribute(writer, PROXY_LIST, config);
        writeAttribute(writer, PROXY_URL, config);
        writeAttribute(writer, ADVERTISE, config);
        writeAttribute(writer, ADVERTISE_SECURITY_KEY, config);
        writeAttribute(writer, EXCLUDED_CONTEXTS, config);
        writeAttribute(writer, AUTO_ENABLE_CONTEXTS, config);
        writeAttribute(writer, STOP_CONTEXT_TIMEOUT, config);
        writeAttribute(writer, SOCKET_TIMEOUT, config);
    }
    static void parsePropConf(XMLExtendedStreamReader reader, ModelNode conf) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {

                case ADVERTISE_SOCKET:
                    conf.get(ADVERTISE_SOCKET).set(value);
                    break;
                case PROXY_LIST:
                    conf.get(PROXY_LIST).set(value);
                    break;
                case PROXY_URL:
                    conf.get(PROXY_URL).set(value);
                    break;
                case ADVERTISE:
                    conf.get(ADVERTISE).set(value);
                    break;
                case ADVERTISE_SECURITY_KEY:
                    conf.get(ADVERTISE_SECURITY_KEY).set(value);
                    break;
                case EXCLUDED_CONTEXTS:
                    conf.get(EXCLUDED_CONTEXTS).set(value);
                    break;
                case AUTO_ENABLE_CONTEXTS:
                    conf.get(AUTO_ENABLE_CONTEXTS).set(value);
                    break;
                case STOP_CONTEXT_TIMEOUT:
                    conf.get(STOP_CONTEXT_TIMEOUT).set(value);
                    break;
                case SOCKET_TIMEOUT:
                    conf.get(SOCKET_TIMEOUT).set(value);
                    break;
                default:
                    unexpectedAttribute(reader, i);
            }
        }
    }

    static void writeSSL(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.SSL.getLocalName());
        writer.writeEndElement();
    }
    static ModelNode parseSSL(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode conf = new ModelNode();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            //TODO: Just read it...
        }
        return conf;
    }

    /* Simple Load provider */
    static void writeSimpleLoadProvider(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.SIMPLE_LOAD_PROVIDER.getLocalName());
        // TODO need something...
        writer.writeEndElement();
    }

    static ModelNode parseSimpleLoadProvider(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode load = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case FACTOR:
                    load.get(FACTOR).set(value);
                    break;
                default:
                    unexpectedAttribute(reader, i);
            }
        }
        ParseUtils.requireNoContent(reader);
        return load;
    }

    /* Dynamic load provider */
    static void writeDynamicLoadProvider(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.SIMPLE_LOAD_PROVIDER.getLocalName());
        // TODO need something...
        writer.writeEndElement();
    }

    static ModelNode parseDynamicLoadProvider(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode load = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case HISTORY:
                    load.get(HISTORY).set(value);
                    break;
                case DECAY:
                    load.get(DECAY).set(value);
                    break;
                default:
                    unexpectedAttribute(reader, i);
            }
        }
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            // read the load-metric and the custom-load-metric
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LOAD_METRIC:
                    final ModelNode loadmetric = parseLoadMetric(reader);
                    load.get(LOAD_METRIC).add(loadmetric);
                    break;
                case CUSTOM_LOAD_METRIC:
                    final ModelNode customloadmetric = parseCustomLoadMetric(reader);
                    load.get(CUSTOM_LOAD_METRIC).add(customloadmetric);
                    break;
                default:
                    unexpectedElement(reader);
            }
        }

        return load;
    }
    static ModelNode parseLoadMetric(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode load = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case TYPE:
                    load.get(TYPE).set(value);
                    break;
                case CAPACITY:
                    load.get(CAPACITY).set(value);
                    break;
                case WEIGHT:
                    load.get(WEIGHT).set(value);
                    break;

                default:
                    unexpectedAttribute(reader, i);
            }
        }
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY:
                    final Property property = readProperty(reader);
                    load.set(property);
                    break;
                default:
                    unexpectedElement(reader);
            }
        }
        return load;
    }
    static ModelNode parseCustomLoadMetric(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode load = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CAPACITY:
                    load.get(CAPACITY).set(value);
                    break;
                case WEIGHT:
                    load.get(WEIGHT).set(value);
                    break;
                case CLASS:
                    load.get(CLASS).set(value);
                    break;
                default:
                    unexpectedAttribute(reader, i);
            }
        }
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY:
                    final Property property = readProperty(reader);
                    load.set(property);
                    break;
                default:
                    unexpectedElement(reader);
            }
        }
        return load;
    }

    static void writeAttribute(final XMLExtendedStreamWriter writer, final String name, ModelNode node) throws XMLStreamException {
        if(node.hasDefined(name)) {
            writer.writeAttribute(name, node.get(name).asString());
        }
    }
}
