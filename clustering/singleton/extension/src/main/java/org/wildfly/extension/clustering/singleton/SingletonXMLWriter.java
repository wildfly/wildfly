/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Marshals singleton deployer subsystem configuration to XML.
 * @author Paul Ferraro
 */
public class SingletonXMLWriter implements XMLElementWriter<SubsystemMarshallingContext> {

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(SingletonSubsystemSchema.CURRENT.getNamespace().getUri(), false);
        writeSingletonPolicies(writer, context.getModelNode());
        writer.writeEndElement();
    }

    private static void writeSingletonPolicies(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
        writer.writeStartElement(XMLElement.SINGLETON_POLICIES.getLocalName());

        writeAttributes(writer, model, SingletonResourceDefinition.Attribute.class);

        for (Property property : model.get(SingletonPolicyResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
            writeSingletonPolicy(writer, property.getName(), property.getValue());
        }

        writer.writeEndElement();
    }

    private static void writeSingletonPolicy(XMLExtendedStreamWriter writer, String name, ModelNode policy) throws XMLStreamException {
        writer.writeStartElement(XMLElement.SINGLETON_POLICY.getLocalName());
        writer.writeAttribute(XMLAttribute.NAME.getLocalName(), name);

        writeAttributes(writer, policy, List.of(SingletonPolicyResourceDefinition.CACHE_ATTRIBUTE_GROUP::getContainerAttribute, SingletonPolicyResourceDefinition.CACHE_ATTRIBUTE_GROUP::getCacheAttribute));
        writeAttributes(writer, policy, SingletonPolicyResourceDefinition.Attribute.class);

        if (policy.hasDefined(ElectionPolicyResourceDefinition.WILDCARD_PATH.getKey())) {
            Property property = policy.get(ElectionPolicyResourceDefinition.WILDCARD_PATH.getKey()).asProperty();
            writeElectionPolicy(writer, property.getName(), property.getValue());
        }

        writer.writeEndElement();
    }

    private static void writeElectionPolicy(XMLExtendedStreamWriter writer, String name, ModelNode policy) throws XMLStreamException {
        switch (name) {
            case RandomElectionPolicyResourceDefinition.PATH_VALUE: {
                writer.writeStartElement(XMLElement.RANDOM_ELECTION_POLICY.getLocalName());

                break;
            }
            case SimpleElectionPolicyResourceDefinition.PATH_VALUE: {
                writer.writeStartElement(XMLElement.SIMPLE_ELECTION_POLICY.getLocalName());

                writeAttributes(writer, policy, SimpleElectionPolicyResourceDefinition.Attribute.class);

                break;
            }
            default: {
                throw new IllegalArgumentException(name);
            }
        }

        writeElements(writer, policy, ElectionPolicyResourceDefinition.Attribute.class);

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
        attribute.getDefinition().getMarshaller().marshallAsAttribute(attribute.getDefinition(), model, false, writer);
    }

    private static <A extends Enum<A> & Attribute> void writeElements(XMLExtendedStreamWriter writer, ModelNode model, Class<A> attributeClass) throws XMLStreamException {
        writeElements(writer, model, EnumSet.allOf(attributeClass));
    }

    private static void writeElements(XMLExtendedStreamWriter writer, ModelNode model, Iterable<? extends Attribute> attributes) throws XMLStreamException {
        for (Attribute attribute : attributes) {
            writeElement(writer, model, attribute);
        }
    }

    private static void writeElement(XMLExtendedStreamWriter writer, ModelNode model, Attribute attribute) throws XMLStreamException {
        attribute.getDefinition().getMarshaller().marshallAsElement(attribute.getDefinition(), model, false, writer);
    }
}
