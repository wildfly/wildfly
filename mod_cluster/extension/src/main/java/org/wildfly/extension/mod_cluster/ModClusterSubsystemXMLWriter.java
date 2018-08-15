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

import javax.xml.stream.XMLStreamException;
import java.util.EnumSet;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Radoslav Husar
 */
public final class ModClusterSubsystemXMLWriter implements XMLElementWriter<SubsystemMarshallingContext> {

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(ModClusterSchema.CURRENT.getNamespaceUri(), false);

        ModelNode subsystemModel = context.getModelNode();

        if (subsystemModel.hasDefined(ProxyConfigurationResourceDefinition.WILDCARD_PATH.getKey())) {
            for (Property property : subsystemModel.get(ProxyConfigurationResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                String name = property.getName();
                ModelNode proxy = property.getValue();
                writeProxy(writer, name, proxy);
            }
        }

        writer.writeEndElement();
    }

    @SuppressWarnings("deprecation")
    private static void writeProxy(XMLExtendedStreamWriter writer, String name, ModelNode model) throws XMLStreamException {
        writer.writeStartElement(XMLElement.PROXY.getLocalName());

        writer.writeAttribute(XMLAttribute.NAME.getLocalName(), name);

        writeAttributes(writer, model, ProxyConfigurationResourceDefinition.Attribute.class);

        if (model.get(SimpleLoadProviderResourceDefinition.PATH.getKeyValuePair()).isDefined()) {
            ModelNode loadProviderModel = model.get(SimpleLoadProviderResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(XMLElement.SIMPLE_LOAD_PROVIDER.getLocalName());
            writeAttributes(writer, loadProviderModel, SimpleLoadProviderResourceDefinition.Attribute.class);
            writer.writeEndElement();
        }
        if (model.get(DynamicLoadProviderResourceDefinition.PATH.getKeyValuePair()).isDefined()) {
            ModelNode loadProviderModel = model.get(DynamicLoadProviderResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(XMLElement.DYNAMIC_LOAD_PROVIDER.getLocalName());
            writeAttributes(writer, loadProviderModel, DynamicLoadProviderResourceDefinition.Attribute.class);
            if (loadProviderModel.hasDefined(LoadMetricResourceDefinition.WILDCARD_PATH.getKey())) {
                for (Property prop : loadProviderModel.get(LoadMetricResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                    ModelNode metricModel = prop.getValue();
                    writer.writeStartElement(XMLElement.LOAD_METRIC.getLocalName());
                    writeAttributes(writer, metricModel, LoadMetricResourceDefinition.Attribute.class);
                    writeAttributes(writer, metricModel, LoadMetricResourceDefinition.SharedAttribute.class);
                    writer.writeEndElement();
                }
            }
            if (loadProviderModel.hasDefined(CustomLoadMetricResourceDefinition.WILDCARD_PATH.getKey())) {
                for (Property prop : loadProviderModel.get(CustomLoadMetricResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                    ModelNode customMetricModel = prop.getValue();
                    writer.writeStartElement(XMLElement.CUSTOM_LOAD_METRIC.getLocalName());
                    writeAttributes(writer, customMetricModel, CustomLoadMetricResourceDefinition.Attribute.class);
                    writeAttributes(writer, customMetricModel, LoadMetricResourceDefinition.SharedAttribute.class);
                    writer.writeEndElement();
                }
            }
            writer.writeEndElement();
        }
        if (model.get(SSLResourceDefinition.PATH.getKeyValuePair()).isDefined()) {
            writer.writeStartElement(XMLElement.SSL.getLocalName());
            writeAttributes(writer, model.get(SSLResourceDefinition.PATH.getKeyValuePair()), EnumSet.allOf(SSLResourceDefinition.Attribute.class));
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private static <A extends Enum<A> & Attribute> void writeAttributes(XMLExtendedStreamWriter writer, ModelNode model, Class<A> attributeClass) throws XMLStreamException {
        writeAttributes(writer, model, EnumSet.allOf(attributeClass));
    }

    private static void writeAttributes(XMLExtendedStreamWriter writer, ModelNode model, Iterable<? extends Attribute> attributes) throws XMLStreamException {
        for (Attribute attribute : attributes) {
            writeAttribute(writer, model, attribute);
        }
    }

    private static void writeAttribute(XMLExtendedStreamWriter writer, ModelNode model, Attribute attribute) throws XMLStreamException {
        attribute.getDefinition().getMarshaller().marshallAsAttribute(attribute.getDefinition(), model, true, writer);
    }

}
