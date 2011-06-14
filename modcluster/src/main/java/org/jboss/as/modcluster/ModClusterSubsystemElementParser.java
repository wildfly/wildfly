package org.jboss.as.modcluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.readProperty;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.modcluster.CommonAttributes.*;

import java.util.Collections;
import java.util.Iterator;
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
                            final ModelNode config = parseModClusterConfig(reader);
                            subsystem.get(MOD_CLUSTER_CONFIG).set(config);
                            break;
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
        final ModelNode ssl = new ModelNode();
        ssl.setEmptyObject();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
            case KEY_ALIAS:
                ssl.get(KEY_ALIAS).set(value);
                break;
            case PASSWORD:
                ssl.get(PASSWORD).set(value);
                break;
            case CERTIFICATE_KEY_FILE:
                ssl.get(CERTIFICATE_KEY_FILE).set(value);
                break;
            case CIPHER_SUITE:
                ssl.get(CIPHER_SUITE).set(value);
                break;
            case PROTOCOL:
                ssl.get(PROTOCOL).set(value);
                break;
             case CA_CERTIFICATE_FILE:
                ssl.get(CA_CERTIFICATE_FILE).set(value);
                break;
            case CA_REVOCATION_URL:
                ssl.get(CA_REVOCATION_URL).set(value);
                break;
           default:
                throw unexpectedAttribute(reader, i);
            }
        }
        ParseUtils.requireNoContent(reader);
        return ssl;
    }

    /* Simple Load provider */
    static void writeSimpleLoadProvider(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.SIMPLE_LOAD_PROVIDER.getLocalName());
        writeAttribute(writer, FACTOR, config);
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
        writeAttribute(writer, HISTORY, config);
        writeAttribute(writer, DECAY, config);

        // write the elements.
        if (config.hasDefined(LOAD_METRIC)) {
            writeLoadMetric(writer, config.get(LOAD_METRIC));
        }
        if (config.hasDefined(CUSTOM_LOAD_METRIC)) {
            writeCustomLoadMetric(writer, config.get(CUSTOM_LOAD_METRIC));
        }
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

    /* Load Metric parsing logic */
    static void writeLoadMetric(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.LOAD_METRIC.getLocalName());
        final List<ModelNode> array = config.asList();
        Iterator<ModelNode> it = array.iterator();
        while (it.hasNext()) {
            final ModelNode node = (ModelNode) it.next();
            writer.writeStartElement(Element.CUSTOM_LOAD_METRIC.getLocalName());
            writeAttribute(writer, TYPE, node);
            writeAttribute(writer, WEIGHT, node);
            writeAttribute(writer, CLASS, node);
            for (Property property : node.get("property").asPropertyList()) {
                writeProperty(writer, property);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
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
                    final Property property = parseProperty(reader);
                    load.set(property);
                    break;
                default:
                    unexpectedElement(reader);
            }
        }
        return load;
    }

    /* Custom Load Metric parsing logic */
    static void writeCustomLoadMetric(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        final List<ModelNode> array = config.asList();
        Iterator<ModelNode> it = array.iterator();
        while (it.hasNext()) {
            final ModelNode node = (ModelNode) it.next();
            writer.writeStartElement(Element.CUSTOM_LOAD_METRIC.getLocalName());
            writeAttribute(writer, CAPACITY, node);
            writeAttribute(writer, WEIGHT, node);
            writeAttribute(writer, CLASS, node);
            for (Property property : node.get("property").asPropertyList()) {
                writeProperty(writer, property);
            }
            writer.writeEndElement();
        }
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
                    final Property property = parseProperty(reader);
                    load.set(property);
                    break;
                default:
                    unexpectedElement(reader);
            }
        }
        return load;
    }

    static void writeAttribute(final XMLExtendedStreamWriter writer, final String name, ModelNode node)
            throws XMLStreamException {
        if (node.hasDefined(name)) {
            writer.writeAttribute(name, node.get(name).asString());
        }
    }

    /* Property logic */
    static void writeProperty(final XMLExtendedStreamWriter writer, Property property) throws XMLStreamException {
        writer.writeStartElement(Element.PROPERTY.getLocalName());
        writer.writeAttribute(NAME, property.getName());
        writer.writeAttribute(VALUE, property.getValue().toString());
        writer.writeEndElement();
    }
    static Property parseProperty(XMLExtendedStreamReader reader) throws XMLStreamException {
        String name = null;
        String value = null;

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final String localName = reader.getAttributeLocalName(i);
            if (localName.equals("name")) {
                name = reader.getAttributeValue(i);
            } else if (localName.equals("value")) {
                value = reader.getAttributeValue(i);
            } else {
                throw unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }
        ParseUtils.requireNoContent(reader);
        return new Property(name, new ModelNode().set(value == null ? "" : value));
    }
}