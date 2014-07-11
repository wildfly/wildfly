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
package org.wildfly.extension.mod_cluster;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

public class ModClusterSubsystemXMLReader_1_0 implements XMLElementReader<List<ModelNode>> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        ParseUtils.requireNoAttributes(reader);

        PathAddress address = PathAddress.pathAddress(ModClusterSubsystemResourceDefinition.PATH);

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address.toModelNode());
        list.add(subsystem);

        // Reads it
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case MOD_CLUSTER_CONFIG:
                    parseModClusterConfig(reader, list, address);
                    break;
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    void parseModClusterConfig(XMLExtendedStreamReader reader, List<ModelNode> list, PathAddress parent) throws XMLStreamException {
        PathAddress address = parent.append(ModClusterConfigResourceDefinition.PATH);
        final ModelNode config = Util.createAddOperation(address);
        list.add(config);

        // Parse the attributes.
        parsePropConf(reader, config);
        // Parse the elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SIMPLE_LOAD_PROVIDER:
                    parseSimpleLoadProvider(reader, config);
                    break;
                case DYNAMIC_LOAD_PROVIDER:
                    parseDynamicLoadProvider(reader, list, address);
                    break;
                case SSL:
                    parseSSL(reader, list, address);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    @SuppressWarnings("deprecation")
    void parsePropConf(XMLExtendedStreamReader reader, ModelNode conf) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ADVERTISE_SOCKET:
                case PROXY_LIST:
                case PROXY_URL:
                case ADVERTISE:
                case ADVERTISE_SECURITY_KEY:
                case EXCLUDED_CONTEXTS:
                case AUTO_ENABLE_CONTEXTS:
                case STOP_CONTEXT_TIMEOUT:
                case SOCKET_TIMEOUT:
                case STICKY_SESSION:
                case STICKY_SESSION_REMOVE:
                case STICKY_SESSION_FORCE:
                case WORKER_TIMEOUT:
                case MAX_ATTEMPTS:
                case FLUSH_PACKETS:
                case FLUSH_WAIT:
                case PING:
                case SMAX:
                case TTL:
                case NODE_TIMEOUT:
                case BALANCER:
                    ModClusterConfigResourceDefinition.ATTRIBUTES_BY_NAME.get(attribute.getLocalName()).parseAndSetParameter(value, conf, reader);
                    break;
                case DOMAIN:
                    ModClusterConfigResourceDefinition.LOAD_BALANCING_GROUP.parseAndSetParameter(value, conf, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        // This is a required attribute - so set it to something reasonable
        ModClusterConfigResourceDefinition.CONNECTOR.parseAndSetParameter("ajp", conf, reader);
    }

    void parseSSL(XMLExtendedStreamReader reader, List<ModelNode> list, PathAddress parent) throws XMLStreamException {
        PathAddress address = parent.append(ModClusterSSLResourceDefinition.PATH);
        final ModelNode ssl = Util.createAddOperation(address);
        list.add(ssl);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case KEY_ALIAS:
                case PASSWORD:
                case CERTIFICATE_KEY_FILE:
                case CIPHER_SUITE:
                case PROTOCOL:
                case CA_CERTIFICATE_FILE:
                case CA_REVOCATION_URL:
                    ModClusterSSLResourceDefinition.ATTRIBUTES_BY_NAME.get(attribute.getLocalName()).parseAndSetParameter(value, ssl, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    void parseSimpleLoadProvider(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case FACTOR:
                    ModClusterConfigResourceDefinition.SIMPLE_LOAD_PROVIDER.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        ParseUtils.requireNoContent(reader);

    }

    void parseDynamicLoadProvider(XMLExtendedStreamReader reader, List<ModelNode> list, PathAddress parent) throws XMLStreamException {
        PathAddress address = parent.append(DynamicLoadProviderDefinition.PATH);
        final ModelNode load = Util.createAddOperation(address);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case HISTORY:
                    DynamicLoadProviderDefinition.HISTORY.parseAndSetParameter(value, load, reader);
                    break;
                case DECAY:
                    DynamicLoadProviderDefinition.DECAY.parseAndSetParameter(value, load, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        list.add(load);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            // read the load-metric and the custom-load-metric
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LOAD_METRIC:
                    parseLoadMetric(reader, list, address);
                    break;
                case CUSTOM_LOAD_METRIC:
                    parseCustomLoadMetric(reader, list, address);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    void parseLoadMetric(XMLExtendedStreamReader reader, List<ModelNode> list, PathAddress address) throws XMLStreamException {

        final ModelNode metric = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case TYPE:
                    LoadMetricDefinition.TYPE.parseAndSetParameter(value, metric, reader);
                    break;
                case CAPACITY:
                    LoadMetricDefinition.CAPACITY.parseAndSetParameter(value, metric, reader);
                    break;
                case WEIGHT:
                    LoadMetricDefinition.WEIGHT.parseAndSetParameter(value, metric, reader);
                    break;

                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        PathElement pe = PathElement.pathElement(LoadMetricDefinition.PATH.getKey(), metric.get(CommonAttributes.TYPE).asString());
        metric.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        metric.get(ModelDescriptionConstants.OP_ADDR).set(address.append(pe).toModelNode());
        readProperties(reader, metric);
        list.add(metric);
    }

    static void readProperties(XMLExtendedStreamReader reader, ModelNode metric) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY:
                    final Property property = ParseUtils.readProperty(reader, true);
                    metric.get(CommonAttributes.PROPERTY).get(property.getName()).set(property.getValue());
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    void parseCustomLoadMetric(XMLExtendedStreamReader reader, List<ModelNode> list, PathAddress address) throws XMLStreamException {
        final ModelNode customMetric = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CAPACITY:
                    LoadMetricDefinition.CAPACITY.parseAndSetParameter(value, customMetric, reader);
                    break;
                case WEIGHT:
                    LoadMetricDefinition.WEIGHT.parseAndSetParameter(value, customMetric, reader);
                    break;
                case CLASS:
                    CustomLoadMetricDefinition.CLASS.parseAndSetParameter(value, customMetric, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        PathElement pe = PathElement.pathElement(CustomLoadMetricDefinition.PATH.getKey(), customMetric.get(CommonAttributes.CLASS).asString());
        customMetric.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        customMetric.get(ModelDescriptionConstants.OP_ADDR).set(address.append(pe).toModelNode());
        readProperties(reader, customMetric);
        list.add(customMetric);
    }

}
