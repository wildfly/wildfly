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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

import static org.wildfly.extension.mod_cluster.CommonAttributes.CUSTOM_LOAD_METRIC;
import static org.wildfly.extension.mod_cluster.CommonAttributes.DECAY;
import static org.wildfly.extension.mod_cluster.CommonAttributes.HISTORY;
import static org.wildfly.extension.mod_cluster.CommonAttributes.LOAD_METRIC;
import static org.wildfly.extension.mod_cluster.CommonAttributes.NAME;
import static org.wildfly.extension.mod_cluster.CommonAttributes.SIMPLE_LOAD_PROVIDER_FACTOR;
import static org.wildfly.extension.mod_cluster.CommonAttributes.VALUE;

public class ModClusterSubsystemXMLWriter implements XMLElementWriter<SubsystemMarshallingContext> {

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context)
            throws XMLStreamException {
        context.startSubsystemElement(ModClusterSchema.CURRENT.getNamespaceUri(), false);

        ModelNode node = context.getModelNode();
        if (node.get(ModClusterConfigResourceDefinition.PATH.getKeyValuePair()).isDefined()) {
            writeModClusterConfig(writer, node.get(ModClusterConfigResourceDefinition.PATH.getKeyValuePair()));
        } else {
            writeModClusterConfig(writer, node);
        }
        writer.writeEndElement();
    }

    static void writeModClusterConfig(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.MOD_CLUSTER_CONFIG.getLocalName());

        // write attributes
        writeConfiguration(writer, config);

        // write elements
        if (config.hasDefined(SIMPLE_LOAD_PROVIDER_FACTOR)) {
            writeSimpleLoadProvider(writer, config);
        }
        if (config.get(DynamicLoadProviderDefinition.PATH.getKeyValuePair()).isDefined()) {
            writeDynamicLoadProvider(writer, config.get(DynamicLoadProviderDefinition.PATH.getKeyValuePair()));
        }
        if (config.get(ModClusterSSLResourceDefinition.PATH.getKeyValuePair()).isDefined()) {
            writeSSL(writer, config.get(ModClusterSSLResourceDefinition.PATH.getKeyValuePair()));
        }
        writer.writeEndElement();
    }

    static void writeConfiguration(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        for (AttributeDefinition def : ModClusterConfigResourceDefinition.ATTRIBUTES) {
            def.getAttributeMarshaller().marshallAsAttribute(def, config, true, writer);
        }
    }

    static void writeSSL(XMLExtendedStreamWriter writer, ModelNode sslConfig) throws XMLStreamException {
        writer.writeStartElement(Element.SSL.getLocalName());
        for (SimpleAttributeDefinition def : ModClusterSSLResourceDefinition.ATTRIBUTES) {
            def.marshallAsAttribute(sslConfig, false, writer);
        }
        writer.writeEndElement();
    }

    /* Simple Load provider */
    static void writeSimpleLoadProvider(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.SIMPLE_LOAD_PROVIDER.getLocalName());
        ModClusterConfigResourceDefinition.SIMPLE_LOAD_PROVIDER.marshallAsAttribute(config, false, writer);
        writer.writeEndElement();
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

    /* Load Metric parsing logic */
    static void writeLoadMetric(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        for (Property prop : config.asPropertyList()) {
            ModelNode node = prop.getValue();
            writer.writeStartElement(Element.LOAD_METRIC.getLocalName());
            LoadMetricDefinition.TYPE.marshallAsAttribute(node, false, writer);
            LoadMetricDefinition.WEIGHT.marshallAsAttribute(node, false, writer);
            LoadMetricDefinition.CAPACITY.marshallAsAttribute(node, false, writer);
            if (node.get(CommonAttributes.PROPERTY).isDefined()) {
                for (Property property : node.get(CommonAttributes.PROPERTY).asPropertyList()) {
                    writeProperty(writer, property);
                }
            }
            writer.writeEndElement();
        }
    }

    /* Custom Load Metric parsing logic */
    static void writeCustomLoadMetric(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        for (Property prop : config.asPropertyList()) {
            ModelNode node = prop.getValue();
            writer.writeStartElement(Element.CUSTOM_LOAD_METRIC.getLocalName());
            CustomLoadMetricDefinition.CLASS.marshallAsAttribute(node, false, writer);
            LoadMetricDefinition.WEIGHT.marshallAsAttribute(node, false, writer);
            LoadMetricDefinition.CAPACITY.marshallAsAttribute(node, false, writer);
            if (node.get(CommonAttributes.PROPERTY).isDefined()) {
                for (Property property : node.get(CommonAttributes.PROPERTY).asPropertyList()) {
                    writeProperty(writer, property);
                }
            }
            writer.writeEndElement();
        }
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
}
