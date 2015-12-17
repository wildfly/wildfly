/*
 * Copyright 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.batch.jberet;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class AttributeMarshallers {
    public static final AttributeMarshaller NAMED = new AttributeMarshaller() {

        @Override
        public void marshallAsElement(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(attribute.getName())) {
                writer.writeStartElement(attribute.getName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), resourceModel.get(attribute.getName()).asString());
                writer.writeEndElement();
            }
        }
    };

    public static final AttributeMarshaller VALUE = new AttributeMarshaller() {

        @Override
        public void marshallAsElement(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(attribute.getName())) {
                writer.writeStartElement(attribute.getName());
                writer.writeAttribute(Attribute.VALUE.getLocalName(), resourceModel.get(attribute.getName()).asString());
                writer.writeEndElement();
            }
        }
    };
}
