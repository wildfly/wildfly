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
package org.jboss.as.modcluster;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
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
import static org.jboss.as.modcluster.CommonAttributes.WEIGHT;
import static org.jboss.as.modcluster.CommonAttributes.WORKER_TIMEOUT;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

public class ModClusterSubsystemXMLReader_1_0 implements XMLElementReader<List<ModelNode>> {

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
        }
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
                    throw unexpectedElement(reader);
            }
        }
        return config;
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
                    conf.get(PROXY_LIST).set(ParseUtils.parsePossibleExpression(value));
                    break;
                case PROXY_URL:
                    conf.get(PROXY_URL).set(ParseUtils.parsePossibleExpression(value));
                    break;
                case ADVERTISE:
                    conf.get(ADVERTISE).set(value);
                    break;
                case ADVERTISE_SECURITY_KEY:
                    conf.get(ADVERTISE_SECURITY_KEY).set(ParseUtils.parsePossibleExpression(value));
                    break;
                case EXCLUDED_CONTEXTS:
                    conf.get(EXCLUDED_CONTEXTS).set(ParseUtils.parsePossibleExpression(value));
                    break;
                case AUTO_ENABLE_CONTEXTS:
                    conf.get(AUTO_ENABLE_CONTEXTS).set(Boolean.parseBoolean(value));
                    break;
                case STOP_CONTEXT_TIMEOUT:
                    conf.get(STOP_CONTEXT_TIMEOUT).set(Integer.parseInt(value));
                    break;
                case SOCKET_TIMEOUT:
                    conf.get(SOCKET_TIMEOUT).set(Integer.parseInt(value));
                    break;
                case STICKY_SESSION:
                    conf.get(STICKY_SESSION).set(Boolean.parseBoolean(value));
                    break;
                case STICKY_SESSION_REMOVE:
                    conf.get(STICKY_SESSION_REMOVE).set(Boolean.parseBoolean(value));
                    break;
                case STICKY_SESSION_FORCE:
                    conf.get(STICKY_SESSION_FORCE).set(Boolean.parseBoolean(value));
                    break;
                case WORKER_TIMEOUT:
                    conf.get(WORKER_TIMEOUT).set(Integer.parseInt(value));
                    break;
                 case MAX_ATTEMPTS:
                     conf.get(MAX_ATTEMPTS).set(Integer.parseInt(value));
                     break;
                case FLUSH_PACKETS:
                    conf.get(FLUSH_PACKETS).set(Boolean.parseBoolean(value));
                    break;
                case FLUSH_WAIT:
                    conf.get(FLUSH_WAIT).set(Integer.parseInt(value));
                    break;
                case PING:
                    conf.get(PING).set(Integer.parseInt(value));
                    break;
                case SMAX:
                    conf.get(SMAX).set(Integer.parseInt(value));
                    break;
                case TTL:
                    conf.get(TTL).set(Integer.parseInt(value));
                    break;
                case NODE_TIMEOUT:
                    conf.get(NODE_TIMEOUT).set(Integer.parseInt(value));
                    break;
                case BALANCER:
                    conf.get(BALANCER).set(value);
                    break;
                case DOMAIN:
                    conf.get(DOMAIN).set(ParseUtils.parsePossibleExpression(value));
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
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
                ssl.get(PASSWORD).set(ParseUtils.parsePossibleExpression(value));
                break;
            case CERTIFICATE_KEY_FILE:
                ssl.get(CERTIFICATE_KEY_FILE).set(ParseUtils.parsePossibleExpression(value));
                break;
            case CIPHER_SUITE:
                ssl.get(CIPHER_SUITE).set(value);
                break;
            case PROTOCOL:
                ssl.get(PROTOCOL).set(value);
                break;
             case CA_CERTIFICATE_FILE:
                ssl.get(CA_CERTIFICATE_FILE).set(ParseUtils.parsePossibleExpression(value));
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
                    throw unexpectedAttribute(reader, i);
            }
        }
        ParseUtils.requireNoContent(reader);
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
                    throw unexpectedAttribute(reader, i);
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
                    throw unexpectedElement(reader);
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
                    throw unexpectedAttribute(reader, i);
            }
        }
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY:
                    final Property property = parseProperty(reader);
                    load.get(CommonAttributes.PROPERTY).add(property.getName(), property.getValue());
                    break;
                default:
                    throw unexpectedElement(reader);
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
                    throw unexpectedAttribute(reader, i);
            }
        }
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY:
                    final Property property = parseProperty(reader);
                    load.get(CommonAttributes.PROPERTY).add(property.getName(), property.getValue());
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
        return load;
    }

    static Property parseProperty(XMLExtendedStreamReader reader) throws XMLStreamException {
        String name = null;
        String value = null;

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = reader.getAttributeValue(i);
                    break;
                }
                case VALUE: {
                    value = reader.getAttributeValue(i);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME.getLocalName()));
        }
        ParseUtils.requireNoContent(reader);
        return new Property(name, new ModelNode().set(value == null ? "" : value));
    }
}