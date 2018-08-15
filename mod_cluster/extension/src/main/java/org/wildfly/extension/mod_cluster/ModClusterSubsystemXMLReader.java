/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.mod_cluster;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.wildfly.extension.mod_cluster.XMLAttribute.CLASS;
import static org.wildfly.extension.mod_cluster.XMLAttribute.TYPE;

import javax.xml.stream.XMLStreamException;
import java.util.List;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author Jean-Frederic Clere
 * @author Radoslav Husar
 */
final class ModClusterSubsystemXMLReader implements XMLElementReader<List<ModelNode>> {

    private final ModClusterSchema schema;

    ModClusterSubsystemXMLReader(ModClusterSchema schema) {
        this.schema = schema;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        ParseUtils.requireNoAttributes(reader);

        PathAddress subsystemAddress = PathAddress.pathAddress(ModClusterSubsystemResourceDefinition.PATH);
        ModelNode subsystemAddOp = Util.createAddOperation(subsystemAddress);
        list.add(subsystemAddOp);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case MOD_CLUSTER_CONFIG: {
                    if (!schema.since(ModClusterSchema.MODCLUSTER_4_0)) {
                        this.parseProxy(reader, list, subsystemAddress);
                        break;
                    }
                }
                case PROXY: {
                    if (schema.since(ModClusterSchema.MODCLUSTER_4_0)) {
                        this.parseProxy(reader, list, subsystemAddress);
                        break;
                    }
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void parseProxy(XMLExtendedStreamReader reader, List<ModelNode> list, PathAddress parent) throws XMLStreamException {
        String name = schema.since(ModClusterSchema.MODCLUSTER_4_0) ? require(reader, XMLAttribute.NAME) : "default";

        PathAddress address = parent.append(ProxyConfigurationResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        list.add(operation);

        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ADVERTISE_SOCKET: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.ADVERTISE_SOCKET);
                    break;
                }
                case PROXY_URL: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.PROXY_URL);
                    break;
                }
                case ADVERTISE: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.ADVERTISE);
                    break;
                }
                case ADVERTISE_SECURITY_KEY: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.ADVERTISE_SECURITY_KEY);
                    break;
                }
                case EXCLUDED_CONTEXTS: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.EXCLUDED_CONTEXTS);
                    break;
                }
                case AUTO_ENABLE_CONTEXTS: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.AUTO_ENABLE_CONTEXTS);
                    break;
                }
                case STOP_CONTEXT_TIMEOUT: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.STOP_CONTEXT_TIMEOUT);
                    break;
                }
                case SOCKET_TIMEOUT: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.SOCKET_TIMEOUT);
                    break;
                }
                case STICKY_SESSION: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.STICKY_SESSION);
                    break;
                }
                case STICKY_SESSION_REMOVE: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.STICKY_SESSION_REMOVE);
                    break;
                }
                case STICKY_SESSION_FORCE: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.STICKY_SESSION_FORCE);
                    break;
                }
                case WORKER_TIMEOUT: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.WORKER_TIMEOUT);
                    break;
                }
                case MAX_ATTEMPTS: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.MAX_ATTEMPTS);
                    break;
                }
                case FLUSH_PACKETS: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.FLUSH_PACKETS);
                    break;
                }
                case FLUSH_WAIT: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.FLUSH_WAIT);
                    break;
                }
                case PING: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.PING);
                    break;
                }
                case SMAX: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.SMAX);
                    break;
                }
                case TTL: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.TTL);
                    break;
                }
                case NODE_TIMEOUT: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.NODE_TIMEOUT);
                    break;
                }
                case BALANCER: {
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.BALANCER);
                    break;
                }
                case PROXY_LIST: {
                    // The legacy PROXY_LIST is here to support EAP 6.x slaves
                    readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.PROXY_LIST);
                    break;
                }
                // 1.0
                case DOMAIN: {
                    if (schema == ModClusterSchema.MODCLUSTER_1_0) {
                        readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.LOAD_BALANCING_GROUP);
                        break;
                    }
                }
                // 1.1
                case LOAD_BALANCING_GROUP: {
                    if (schema.since(ModClusterSchema.MODCLUSTER_1_1)) {
                        readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.LOAD_BALANCING_GROUP);
                        break;
                    }
                }
                case CONNECTOR: {
                    if (schema.since(ModClusterSchema.MODCLUSTER_1_1) && !schema.since(ModClusterSchema.MODCLUSTER_4_0)) {
                        readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.LISTENER);
                        break;
                    } else {
                        throw unexpectedAttribute(reader, i);
                    }
                }
                // 1.2
                case SESSION_DRAINING_STRATEGY: {
                    if (schema.since(ModClusterSchema.MODCLUSTER_1_2)) {
                        readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.SESSION_DRAINING_STRATEGY);
                        break;
                    }
                }
                // 2.0
                case STATUS_INTERVAL: {
                    if (schema.since(ModClusterSchema.MODCLUSTER_2_0)) {
                        readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.STATUS_INTERVAL);
                        break;
                    }
                }
                case PROXIES: {
                    if (schema.since(ModClusterSchema.MODCLUSTER_2_0)) {
                        readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.PROXIES);
                        break;
                    }
                }
                // 3.0
                case SSL_CONTEXT: {
                    if (schema.since(ModClusterSchema.MODCLUSTER_3_0)) {
                        readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.SSL_CONTEXT);
                        break;
                    }
                }
                // 4.0
                case NAME: {
                    if (schema.since(ModClusterSchema.MODCLUSTER_4_0)) {
                        // Ignore -- already parsed.
                        break;
                    }
                }
                case LISTENER: {
                    if (schema.since(ModClusterSchema.MODCLUSTER_4_0)) {
                        readAttribute(reader, i, operation, ProxyConfigurationResourceDefinition.Attribute.LISTENER);
                        break;
                    }
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }

        if (schema == ModClusterSchema.MODCLUSTER_1_0) {
            // This is a required attribute - so set it to something reasonable
            setAttribute(reader, "ajp", operation, ProxyConfigurationResourceDefinition.Attribute.LISTENER);
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case SIMPLE_LOAD_PROVIDER: {
                    this.parseSimpleLoadProvider(reader, list, address);
                    break;
                }
                case DYNAMIC_LOAD_PROVIDER: {
                    this.parseDynamicLoadProvider(reader, list, address);
                    break;
                }
                case SSL: {
                    this.parseSSL(reader, list, address);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void parseSSL(XMLExtendedStreamReader reader, List<ModelNode> list, PathAddress parent) throws XMLStreamException {
        PathAddress address = parent.append(SSLResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        list.add(operation);
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                // toto enum-ize
                case KEY_ALIAS: {
                    readAttribute(reader, i, operation, SSLResourceDefinition.Attribute.KEY_ALIAS);
                    break;
                }
                case PASSWORD: {
                    readAttribute(reader, i, operation, SSLResourceDefinition.Attribute.PASSWORD);
                    break;
                }
                case CERTIFICATE_KEY_FILE: {
                    readAttribute(reader, i, operation, SSLResourceDefinition.Attribute.CERTIFICATE_KEY_FILE);
                    break;
                }
                case CIPHER_SUITE: {
                    readAttribute(reader, i, operation, SSLResourceDefinition.Attribute.CIPHER_SUITE);
                    break;
                }
                case PROTOCOL: {
                    readAttribute(reader, i, operation, SSLResourceDefinition.Attribute.PROTOCOL);
                    break;
                }
                case CA_CERTIFICATE_FILE: {
                    readAttribute(reader, i, operation, SSLResourceDefinition.Attribute.CA_CERTIFICATE_FILE);
                    break;
                }
                case CA_REVOCATION_URL: {
                    readAttribute(reader, i, operation, SSLResourceDefinition.Attribute.CA_REVOCATION_URL);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseSimpleLoadProvider(XMLExtendedStreamReader reader, List<ModelNode> list, PathAddress parent) throws XMLStreamException {
        PathAddress address = parent.append(SimpleLoadProviderResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);

        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case FACTOR: {
                    readAttribute(reader, i, operation, SimpleLoadProviderResourceDefinition.Attribute.FACTOR);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);

        list.add(operation);
    }

    private void parseDynamicLoadProvider(XMLExtendedStreamReader reader, List<ModelNode> list, PathAddress parent) throws XMLStreamException {
        PathAddress address = parent.append(DynamicLoadProviderResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);

        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case HISTORY: {
                    readAttribute(reader, i, operation, DynamicLoadProviderResourceDefinition.Attribute.HISTORY);
                    break;
                }
                case DECAY: {
                    readAttribute(reader, i, operation, DynamicLoadProviderResourceDefinition.Attribute.DECAY);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }

        list.add(operation);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case LOAD_METRIC: {
                    this.parseLoadMetric(reader, list, address);
                    break;
                }
                case CUSTOM_LOAD_METRIC: {
                    this.parseCustomLoadMetric(reader, list, address);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseLoadMetric(XMLExtendedStreamReader reader, List<ModelNode> list, PathAddress address) throws XMLStreamException {
        String type = require(reader, TYPE);
        PathAddress opAddress = address.append(LoadMetricResourceDefinition.pathElement(type));
        ModelNode operation = Util.createAddOperation(opAddress);

        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case TYPE: {
                    // TODO polish this being both path and required attribute
                    readAttribute(reader, i, operation, LoadMetricResourceDefinition.Attribute.TYPE);
                    break;
                }
                case CAPACITY: {
                    readAttribute(reader, i, operation, LoadMetricResourceDefinition.SharedAttribute.CAPACITY);
                    break;
                }
                case WEIGHT: {
                    readAttribute(reader, i, operation, LoadMetricResourceDefinition.SharedAttribute.WEIGHT);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }

        readProperties(reader, operation);
        list.add(operation);
    }

    private void parseCustomLoadMetric(XMLExtendedStreamReader reader, List<ModelNode> list, PathAddress address) throws XMLStreamException {
        String type = require(reader, CLASS);
        PathAddress opAddress = address.append(CustomLoadMetricResourceDefinition.pathElement(type));
        ModelNode operation = Util.createAddOperation(opAddress);

        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CLASS: {
                    // TODO polish this being both path and required attribute
                    readAttribute(reader, i, operation, CustomLoadMetricResourceDefinition.Attribute.CLASS);
                    break;
                }
                case CAPACITY: {
                    readAttribute(reader, i, operation, LoadMetricResourceDefinition.SharedAttribute.CAPACITY);
                    break;
                }
                case WEIGHT: {
                    readAttribute(reader, i, operation, LoadMetricResourceDefinition.SharedAttribute.WEIGHT);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }

        readProperties(reader, operation);
        list.add(operation);
    }

    private static String require(XMLExtendedStreamReader reader, XMLAttribute attribute) throws XMLStreamException {
        String value = reader.getAttributeValue(null, attribute.getLocalName());
        if (value == null) {
            throw ParseUtils.missingRequired(reader, attribute.getLocalName());
        }
        return value;
    }

    private static void readProperties(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY: {
                    Property property = ParseUtils.readProperty(reader, true);
                    operation.get(LoadMetricResourceDefinition.SharedAttribute.PROPERTY.getName()).get(property.getName()).set(property.getValue());
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void readAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation, Attribute attribute) throws XMLStreamException {
        setAttribute(reader, reader.getAttributeValue(index), operation, attribute);
    }

    private static void setAttribute(XMLExtendedStreamReader reader, String value, ModelNode operation, Attribute attribute) throws XMLStreamException {
        AttributeDefinition definition = attribute.getDefinition();
        definition.getParser().parseAndSetParameter(definition, value, operation, reader);
    }

}
