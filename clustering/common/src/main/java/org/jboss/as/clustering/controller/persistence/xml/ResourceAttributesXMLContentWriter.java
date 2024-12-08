/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.xml.XMLContentWriter;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Writes XML content from a resource model.
 */
public class ResourceAttributesXMLContentWriter implements XMLContentWriter<ModelNode> {

    private final List<Map.Entry<AttributeDefinition, AttributeMarshaller>> attributes;

    ResourceAttributesXMLContentWriter(Collection<AttributeDefinition> attributes, Function<AttributeDefinition, AttributeMarshaller> marshallers) {
        this.attributes = attributes.isEmpty() ? List.of() : new ArrayList<>(attributes.size());
        for (AttributeDefinition attribute : attributes) {
            AttributeMarshaller marshaller = marshallers.apply(attribute);
            if (!marshaller.isMarshallableAsElement()) {
                this.attributes.add(Map.entry(attribute, marshaller));
            }
        }
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
        for (Map.Entry<AttributeDefinition, AttributeMarshaller> entry : this.attributes) {
            entry.getValue().marshallAsAttribute(entry.getKey(), model, true, writer);
        }
    }

    @Override
    public boolean isEmpty(ModelNode model) {
        return this.attributes.stream().map(Map.Entry::getKey).map(AttributeDefinition::getName).noneMatch(model::hasDefined);
    }
}
