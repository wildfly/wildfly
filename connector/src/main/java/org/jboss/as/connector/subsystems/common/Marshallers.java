/*
 * Copyright 2017 Red Hat, Inc.
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
package org.jboss.as.connector.subsystems.common;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.dmr.ModelNode;

/**
 * Class for adding attribute marshallers.
 *
 * @author Flavia Rainone
 */
public class Marshallers {
    /**
     * Marshaller that writes down the attribute name as an empty element only if attribute value is true.
     * This marshaller is for those attributes whose presence indicates the attribute is true, and whose absence indicates it is
     * false.
     */
    public static final AttributeMarshaller BOOLEAN_PRESENCE_TYPE_MARSHALLER = new AttributeMarshaller() {

        @Override
        public boolean isMarshallable(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault) {
            // will only marshall true attributes
            return resourceModel.hasDefined(attribute.getName()) && resourceModel.get(attribute.getName()).asBoolean();
        }

        @Override
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws
                XMLStreamException {
            if (resourceModel.hasDefined(attribute.getName()) && resourceModel.get(attribute.getName()).asBoolean()) {
                writer.writeEmptyElement(attribute.getXmlName());
            }
        }
    };
}
