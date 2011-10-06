package org.jboss.as.modcluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.modcluster.CommonAttributes.ADVERTISE;
import static org.jboss.as.modcluster.CommonAttributes.ADVERTISE_SECURITY_KEY;
import static org.jboss.as.modcluster.CommonAttributes.ADVERTISE_SOCKET;
import static org.jboss.as.modcluster.CommonAttributes.AUTO_ENABLE_CONTEXTS;
import static org.jboss.as.modcluster.CommonAttributes.BALANCER;
import static org.jboss.as.modcluster.CommonAttributes.CAPACITY;
import static org.jboss.as.modcluster.CommonAttributes.CA_CERTIFICATE_FILE;
import static org.jboss.as.modcluster.CommonAttributes.CA_REVOCATION_URL;
import static org.jboss.as.modcluster.CommonAttributes.CERTIFICATE_KEY_FILE;
import static org.jboss.as.modcluster.CommonAttributes.CIPHER_SUITE;
import static org.jboss.as.modcluster.CommonAttributes.CLASS;
import static org.jboss.as.modcluster.CommonAttributes.CONFIGURATION;
import static org.jboss.as.modcluster.CommonAttributes.CUSTOM_LOAD_METRIC;
import static org.jboss.as.modcluster.CommonAttributes.DECAY;
import static org.jboss.as.modcluster.CommonAttributes.DOMAIN;
import static org.jboss.as.modcluster.CommonAttributes.DYNAMIC_LOAD_PROVIDER;
import static org.jboss.as.modcluster.CommonAttributes.EXCLUDED_CONTEXTS;
import static org.jboss.as.modcluster.CommonAttributes.FACTOR;
import static org.jboss.as.modcluster.CommonAttributes.FLUSH_PACKETS;
import static org.jboss.as.modcluster.CommonAttributes.FLUSH_WAIT;
import static org.jboss.as.modcluster.CommonAttributes.HISTORY;
import static org.jboss.as.modcluster.CommonAttributes.KEY_ALIAS;
import static org.jboss.as.modcluster.CommonAttributes.LOAD_METRIC;
import static org.jboss.as.modcluster.CommonAttributes.MAX_ATTEMPTS;
import static org.jboss.as.modcluster.CommonAttributes.MOD_CLUSTER_CONFIG;
import static org.jboss.as.modcluster.CommonAttributes.NAME;
import static org.jboss.as.modcluster.CommonAttributes.NODE_TIMEOUT;
import static org.jboss.as.modcluster.CommonAttributes.PASSWORD;
import static org.jboss.as.modcluster.CommonAttributes.PING;
import static org.jboss.as.modcluster.CommonAttributes.PROTOCOL;
import static org.jboss.as.modcluster.CommonAttributes.PROXY_LIST;
import static org.jboss.as.modcluster.CommonAttributes.PROXY_URL;
import static org.jboss.as.modcluster.CommonAttributes.SIMPLE_LOAD_PROVIDER;
import static org.jboss.as.modcluster.CommonAttributes.SMAX;
import static org.jboss.as.modcluster.CommonAttributes.SOCKET_TIMEOUT;
import static org.jboss.as.modcluster.CommonAttributes.SSL;
import static org.jboss.as.modcluster.CommonAttributes.STICKY_SESSION;
import static org.jboss.as.modcluster.CommonAttributes.STICKY_SESSION_FORCE;
import static org.jboss.as.modcluster.CommonAttributes.STICKY_SESSION_REMOVE;
import static org.jboss.as.modcluster.CommonAttributes.STOP_CONTEXT_TIMEOUT;
import static org.jboss.as.modcluster.CommonAttributes.TTL;
import static org.jboss.as.modcluster.CommonAttributes.TYPE;
import static org.jboss.as.modcluster.CommonAttributes.VALUE;
import static org.jboss.as.modcluster.CommonAttributes.WEIGHT;
import static org.jboss.as.modcluster.CommonAttributes.WORKER_TIMEOUT;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

public class ModClusterSubsystemElementParser implements XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext>, XMLStreamConstants {

    /** {@inheritDoc} */
    @Override
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
            writeDynamicLoadProvider(writer, config.get(DYNAMIC_LOAD_PROVIDER));
        }
        if (config.get(SSL).isDefined() && config.get(SSL).has(CONFIGURATION)) {
            writeSSL(writer, config.get(SSL).get(CONFIGURATION));
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

        writeAttribute(writer, STICKY_SESSION, config);
        writeAttribute(writer, STICKY_SESSION_REMOVE, config);
        writeAttribute(writer, STICKY_SESSION_FORCE, config);
        writeAttribute(writer, WORKER_TIMEOUT, config);
        writeAttribute(writer, MAX_ATTEMPTS, config);
        writeAttribute(writer, FLUSH_PACKETS, config);
        writeAttribute(writer, FLUSH_WAIT, config);
        writeAttribute(writer, PING, config);
        writeAttribute(writer, SMAX, config);
        writeAttribute(writer, TTL, config);
        writeAttribute(writer, NODE_TIMEOUT, config);
        writeAttribute(writer, BALANCER, config);
        writeAttribute(writer, DOMAIN, config);
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
                case STICKY_SESSION:
                    conf.get(STICKY_SESSION).set(value);
                    break;
                case STICKY_SESSION_REMOVE:
                    conf.get(STICKY_SESSION_REMOVE).set(value);
                    break;
                case STICKY_SESSION_FORCE:
                    conf.get(STICKY_SESSION_FORCE).set(value);
                    break;
                case WORKER_TIMEOUT:
                    conf.get(WORKER_TIMEOUT).set(value);
                    break;
                 case MAX_ATTEMPTS:
                     conf.get(MAX_ATTEMPTS).set(value);
                     break;
                case FLUSH_PACKETS:
                    conf.get(FLUSH_PACKETS).set(value);
                    break;
                case FLUSH_WAIT:
                    conf.get(FLUSH_WAIT).set(value);
                    break;
                case PING:
                    conf.get(PING).set(value);
                    break;
                case SMAX:
                    conf.get(SMAX).set(value);
                    break;
                case TTL:
                    conf.get(TTL).set(value);
                    break;
                case NODE_TIMEOUT:
                    conf.get(NODE_TIMEOUT).set(value);
                    break;
                case BALANCER:
                    conf.get(BALANCER).set(value);
                    break;
                case DOMAIN:
                    conf.get(DOMAIN).set(value);
                    break;
                default:
                    unexpectedAttribute(reader, i);
            }
        }
    }

    static void writeSSL(XMLExtendedStreamWriter writer, ModelNode sslConfig) throws XMLStreamException {
        writer.writeStartElement(Element.SSL.getLocalName());
        writeAttribute(writer, Attribute.KEY_ALIAS.getLocalName(), sslConfig);
        writeAttribute(writer, Attribute.PASSWORD.getLocalName(), sslConfig);
        writeAttribute(writer, Attribute.CERTIFICATE_KEY_FILE.getLocalName(), sslConfig);
        writeAttribute(writer, Attribute.CIPHER_SUITE.getLocalName(), sslConfig);
        writeAttribute(writer, Attribute.PROTOCOL.getLocalName(), sslConfig);
        writeAttribute(writer, Attribute.CA_CERTIFICATE_FILE.getLocalName(), sslConfig);
        writeAttribute(writer, Attribute.CA_REVOCATION_URL.getLocalName(), sslConfig);
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
        writer.writeStartElement(Element.DYNAMIC_LOAD_PROVIDER.getLocalName());
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
        final List<ModelNode> array = config.asList();
        Iterator<ModelNode> it = array.iterator();
        while (it.hasNext()) {
            final ModelNode node = it.next();
            writer.writeStartElement(Element.LOAD_METRIC.getLocalName());
            writeAttribute(writer, TYPE, node);
            writeAttribute(writer, WEIGHT, node);
            writeAttribute(writer, CAPACITY, node);
            if (node.get("property").isDefined()) {
                for (Property property : node.get("property").asPropertyList()) {
                    writeProperty(writer, property);
                }
            }
            writer.writeEndElement();
        }
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
                    load.get("property").add(property.getName(), property.getValue());
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
            final ModelNode node = it.next();
            writer.writeStartElement(Element.CUSTOM_LOAD_METRIC.getLocalName());
            writeAttribute(writer, CAPACITY, node);
            writeAttribute(writer, WEIGHT, node);
            writeAttribute(writer, CLASS, node);
            if (node.get("property").isDefined()) {
                for (Property property : node.get("property").asPropertyList()) {
                    writeProperty(writer, property);
                }
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
                    load.get("property").add(property.getName(), property.getValue());
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
        writer.writeAttribute(VALUE, property.getValue().asString());
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