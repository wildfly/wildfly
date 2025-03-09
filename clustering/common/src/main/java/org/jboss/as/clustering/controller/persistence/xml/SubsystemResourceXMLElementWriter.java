/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.persistence.xml;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * StAX writer for a subsystem.
 * @author Paul Ferraro
 */
public class SubsystemResourceXMLElementWriter implements XMLElementWriter<SubsystemMarshallingContext>{
    private final SubsystemResourceRegistrationXMLElement element;

    public SubsystemResourceXMLElementWriter(SubsystemResourceRegistrationXMLElement element) {
        this.element = element;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        ModelNode subsystemModel = context.getModelNode();
        ModelNode model = new ModelNode();
        model.get(this.element.getPathElement().getKeyValuePair()).set(subsystemModel);
        this.element.getWriter().writeContent(writer, model);
    }
}
