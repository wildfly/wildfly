/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld;

import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

class WeldSubsystem10Parser implements XMLElementReader<List<ModelNode>> {

    public static final String NAMESPACE = "urn:jboss:domain:weld:1.0";
    static final WeldSubsystem10Parser INSTANCE = new WeldSubsystem10Parser();

    private WeldSubsystem10Parser() {
    }

    /** {@inheritDoc} */
    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        // Require no attributes or content
        requireNoAttributes(reader);
        requireNoContent(reader);
        list.add(Util.createAddOperation(PathAddress.pathAddress(WeldExtension.PATH_SUBSYSTEM)));
    }
}