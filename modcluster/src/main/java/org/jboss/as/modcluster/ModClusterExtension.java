/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.modcluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
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
import static org.jboss.as.modcluster.CommonAttributes.CAPACITY;
import static org.jboss.as.modcluster.CommonAttributes.CLASS;
import static org.jboss.as.modcluster.CommonAttributes.CUSTOM_LOAD_METRIC;
import static org.jboss.as.modcluster.CommonAttributes.DECAY;
import static org.jboss.as.modcluster.CommonAttributes.DYNAMIC_LOAD_PROVIDER;
import static org.jboss.as.modcluster.CommonAttributes.EXCLUDED_CONTEXTS;
import static org.jboss.as.modcluster.CommonAttributes.FACTOR;
import static org.jboss.as.modcluster.CommonAttributes.HISTORY;
import static org.jboss.as.modcluster.CommonAttributes.HTTPD_CONF;
import static org.jboss.as.modcluster.CommonAttributes.LOAD_METRIC;
import static org.jboss.as.modcluster.CommonAttributes.LOAD_PROVIDER;
import static org.jboss.as.modcluster.CommonAttributes.MOD_CLUSTER_CONFIG;
import static org.jboss.as.modcluster.CommonAttributes.NODES_CONF;
import static org.jboss.as.modcluster.CommonAttributes.PROXY_CONF;
import static org.jboss.as.modcluster.CommonAttributes.PROXY_LIST;
import static org.jboss.as.modcluster.CommonAttributes.PROXY_URL;
import static org.jboss.as.modcluster.CommonAttributes.SIMPLE_LOAD_PROVIDER;
import static org.jboss.as.modcluster.CommonAttributes.SOCKET_TIMEOUT;
import static org.jboss.as.modcluster.CommonAttributes.SSL;
import static org.jboss.as.modcluster.CommonAttributes.STOP_CONTEXT_TIMEOUT;
import static org.jboss.as.modcluster.CommonAttributes.TYPE;
import static org.jboss.as.modcluster.CommonAttributes.WEIGHT;

import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Domain extension used to initialize the mod_cluster subsystem element handlers.
 *
 * @author Jean-Frederic Clere
 */
public class ModClusterExtension implements XMLStreamConstants, Extension {

    private static final Logger log = Logger.getLogger("org.jboss.as.modcluster");

    public static final String SUBSYSTEM_NAME = "modcluster";
    public static final String NAMESPACE = "urn:jboss:domain:modcluster:1.0";

    final ModClusterSubsystemElementParser parser = new ModClusterSubsystemElementParser();

    private static final DescriptionProvider DESCRIPTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }
    };

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {

        log.debugf("Activating Mod_cluster Extension");
        final SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration nodeRegistration = registration.registerSubsystemModel(DESCRIPTION);
        nodeRegistration.registerOperationHandler(ModelDescriptionConstants.ADD, ModClusterSubsystemAdd.INSTANCE, DESCRIPTION);
        nodeRegistration.registerOperationHandler(DESCRIBE, ModClusterSubsystemDescribe.INSTANCE, ModClusterSubsystemDescribe.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        registration.registerXMLElementWriter(parser);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(NAMESPACE, parser);
    }

    static class ModClusterSubsystemElementParser implements XMLElementReader<List<ModelNode>>,
            XMLElementWriter<SubsystemMarshallingContext> {

        /** {@inheritDoc} */
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            ParseUtils.requireNoAttributes(reader);

            final ModelNode address = new ModelNode();
            address.add(SUBSYSTEM, SUBSYSTEM_NAME);
            address.protect();


            // Reads it
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                   case MODCLUSTER: {
                       final Element element = Element.forName(reader.getLocalName());
                       switch (element) {
                          case MOD_CLUSTER_CONFIG:
                              final ModelNode config  = parseModClusterConfig(reader);
                              final ModelNode update = new ModelNode();
                              update.get(OP).set(ADD);
                              update.get(OP_ADDR).set(address);
                              update.get(MOD_CLUSTER_CONFIG).set(config);
                              list.add(update);
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
            context.startSubsystemElement(NAMESPACE, false);
            writer.writeEndElement();
        }

    }

    static ModelNode parseModClusterConfig(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode config = new ModelNode();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LOAD_PROVIDER:
                    final ModelNode load = parseLoadProvider(reader);
                    config.get(LOAD_PROVIDER).set(load);
                    break;
                case PROXY_CONF:
                    final ModelNode conf = parseProxyConf(reader);
                    config.get(PROXY_CONF).set(conf);
                    break;
                default:
                    unexpectedElement(reader);
            }
        }
        return config;
    }

    static ModelNode parseProxyConf(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode config = new ModelNode();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
            case HTTPD_CONF:
                final ModelNode proxy = parseHttpdConf(reader);
                config.get(HTTPD_CONF).set(proxy);
                break;
            case NODES_CONF:
                final ModelNode nodes = parseNodesConf(reader);
                config.get(NODES_CONF).set(nodes);
                break;
            default:
                unexpectedElement(reader);
            }
        }
        return config;
    }
    static ModelNode parseHttpdConf(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode conf = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {

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
            default:
                unexpectedAttribute(reader, i);
            }
        }
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADVERTISE_SOCKET:
                    final ModelNode socket = parseAdvertiseSocket(reader);
                    conf.get(ADVERTISE_SOCKET).set(socket);
                    break;
                case SSL:
                    final ModelNode ssl = parseSSL(reader);
                    conf.get(SSL).set(ssl);
                    break;
                default:
                    unexpectedElement(reader);
            }
        }
        return conf;
    }
    static ModelNode parseNodesConf(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode conf = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
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
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            //TODO: Just read it...
        }
        return conf;
    }
    static ModelNode parseAdvertiseSocket(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode conf = new ModelNode();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            //TODO: Just read it...
        }
        return conf;
    }
    static ModelNode parseSSL(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode conf = new ModelNode();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            //TODO: Just read it...
        }
        return conf;
    }

    static ModelNode parseLoadProvider(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode load = new ModelNode();
        // TODO that is elements... not attributes.
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            // read the load-metric and the custom-load-metric
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
            case SIMPLE_LOAD_PROVIDER:
                final ModelNode simple = parseSimpleLoadProvider(reader);
                load.get(SIMPLE_LOAD_PROVIDER).set(simple);
                break;
            case DYNAMIC_LOAD_PROVIDER:
                final ModelNode server = parseDynamicLoadProvider(reader);
                load.get(DYNAMIC_LOAD_PROVIDER).set(server);
                break;
            default:
                unexpectedElement(reader);
            }
        }
        return load;
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

        return load;
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
                case TYPE:
                    load.get(TYPE).set(value);
                    break;
                case CAPACITY:
                    load.get(CAPACITY).set(value);
                    break;
                case WEIGHT:
                    load.get(WEIGHT).set(value);
                    break;
                case CLASS:
                    load.get(CLASS).set(value);
                default:
                    unexpectedAttribute(reader, i);
            }
        }
        return load;
    }
}
