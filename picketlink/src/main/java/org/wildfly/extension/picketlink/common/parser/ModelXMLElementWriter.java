/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.common.parser;

import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.XMLElement;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.wildfly.extension.picketlink.common.model.AbstractResourceDefinition.getAttributeDefinition;
import static org.wildfly.extension.picketlink.common.model.AbstractResourceDefinition.getChildResourceDefinitions;

/**
 * <p> A generic XML Writer for all {@link ModelElement} definitions. </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 9, 2012
 */
public class ModelXMLElementWriter {

    private final Map<String, ModelXMLElementWriter> register;
    private final ModelElement modelElement;
    private XMLElement parentElement;
    private String nameAttribute;

    public ModelXMLElementWriter(ModelElement element, Map<String, ModelXMLElementWriter> register) {
        this.modelElement = element;
        this.register = Collections.unmodifiableMap(register);
    }

    public ModelXMLElementWriter(ModelElement element, XMLElement parentElement, Map<String, ModelXMLElementWriter> register) {
        this(element, register);
        this.parentElement = parentElement;
    }

    public ModelXMLElementWriter(ModelElement element, String nameAttribute, Map<String, ModelXMLElementWriter> register) {
        this(element, register);
        this.nameAttribute = nameAttribute;
    }

    public ModelXMLElementWriter(ModelElement element, String nameAttribute, XMLElement parentElement, Map<String, ModelXMLElementWriter> register) {
        this(element, nameAttribute, register);
        this.parentElement = parentElement;
    }

    public void write(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        String nodeName = modelNode.asProperty().getName();

        if (nodeName.equals(this.modelElement.getName())) {
            List<ModelNode> modelNodes = modelNode.asProperty().getValue().asList();
            boolean writeParrent = this.parentElement != null && !modelNodes.isEmpty();

            if (writeParrent) {
                writer.writeStartElement(this.parentElement.getName());
            }

            for (ModelNode valueNode : modelNodes) {
                writer.writeStartElement(this.modelElement.getName());

                if (this.nameAttribute != null) {
                    writer.writeAttribute(this.nameAttribute, valueNode.keys().iterator().next());
                }

                ModelNode value = valueNode.asProperty().getValue();

                if (value.isDefined()) {
                    writeAttributes(writer, value);

                    for (ModelNode propertyIdentity : value.asList()) {
                        List<ResourceDefinition> children = getChildResourceDefinitions().get(this.modelElement);

                        if (children != null) {
                            for (ResourceDefinition child : children) {
                                get(child.getPathElement().getKey()).write(writer, propertyIdentity);
                            }
                        }
                    }
                }

                writer.writeEndElement();
            }

            if (writeParrent) {
                writer.writeEndElement();
            }
        }
    }

    private ModelXMLElementWriter get(String writerKey) {
        return this.register.get(writerKey);
    }

    private void writeAttributes(XMLStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        for (SimpleAttributeDefinition simpleAttributeDefinition : getAttributeDefinition(this.modelElement)) {
            simpleAttributeDefinition.marshallAsAttribute(modelNode, writer);
        }
    }
}
