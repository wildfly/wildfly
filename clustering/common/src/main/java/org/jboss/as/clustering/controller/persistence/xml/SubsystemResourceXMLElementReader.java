/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.persistence.xml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * StAX reader for a subsystem.
 * @author Paul Ferraro
 */
public class SubsystemResourceXMLElementReader implements XMLElementReader<List<ModelNode>> {
    private final ResourceRegistrationXMLElement element;

    public SubsystemResourceXMLElementReader(ResourceRegistrationXMLElement element) {
        this.element = element;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        if (!reader.getName().equals(this.element.getName())) {
            throw ParseUtils.unexpectedElement(reader, Set.of(this.element.getName().getLocalPart()));
        }
        // An index of operations preserving insertion order
        Map<PathAddress, ModelNode> index = new LinkedHashMap<>();
        this.element.getReader().readElement(reader, Map.entry(PathAddress.EMPTY_ADDRESS, index));
        operations.addAll(index.values());
    }
}
