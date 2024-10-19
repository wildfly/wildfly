/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLContentReader;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Reads XML content into a resource operation.
 */
public class ResourceOperationXMLContentReader implements XMLContentReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> {

    private final XMLContentReader<ModelNode> reader;

    ResourceOperationXMLContentReader(XMLContentReader<ModelNode> reader) {
        this.reader = reader;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, Map.Entry<PathAddress, Map<PathAddress, ModelNode>> context) throws XMLStreamException {
        PathAddress operationKey = context.getKey();
        Map<PathAddress, ModelNode> operations = context.getValue();
        ModelNode operation = operations.get(operationKey);
        this.reader.readElement(reader, operation);
    }

    @Override
    public XMLCardinality getCardinality() {
        return this.reader.getCardinality();
    }
}
