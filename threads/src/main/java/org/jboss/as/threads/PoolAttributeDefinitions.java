/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.threads;


import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.AbstractParameterValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 *
 * @author <a href="alex@jboss.org">Alexey Loubyansky</a>
 */
public interface PoolAttributeDefinitions {

    SimpleAttributeDefinition NAME = new SimpleAttributeDefinition(CommonAttributes.NAME, ModelType.STRING, true);

    SimpleAttributeDefinition THREAD_FACTORY = new SimpleAttributeDefinition(CommonAttributes.THREAD_FACTORY, ModelType.STRING, true);

    ListAttributeDefinition PROPERTIES = new ListAttributeDefinition(CommonAttributes.PROPERTIES, true, new ModelTypeValidator(ModelType.PROPERTY)){
        @Override
        protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
            setValueType(node);
        }
        @Override
        protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            setValueType(node);
        }
        @Override
        protected void addOperationParameterValueTypeDescription(
                ModelNode node, String operationName,
                ResourceDescriptionResolver resolver, Locale locale,
                ResourceBundle bundle) {
            setValueType(node);
        }

        private void setValueType(ModelNode node) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);
        }

        @Override
        public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(getName())) {
                List<ModelNode> list = resourceModel.get(getName()).asList();
                if (list.size() > 0) {
                    writer.writeStartElement(Element.PROPERTIES.getLocalName());
                    for (ModelNode child : list) {
                        final Property prop = child.asProperty();
                        writer.writeEmptyElement(Element.PROPERTY.getLocalName());
                        writer.writeAttribute(Attribute.NAME.getLocalName(), prop.getName());
                        writer.writeAttribute(Attribute.VALUE.getLocalName(), prop.getValue().asString());
                    }
                }
            }
        }};

    SimpleAttributeDefinition MAX_THREADS = new SimpleAttributeDefinition(CommonAttributes.MAX_THREADS, ModelType.OBJECT, false);

    SimpleAttributeDefinition KEEPALIVE_TIME = new SimpleAttributeDefinition(CommonAttributes.KEEPALIVE_TIME, ModelType.OBJECT, true);

    SimpleAttributeDefinition CORE_THREADS = new SimpleAttributeDefinition(CommonAttributes.CORE_THREADS, ModelType.OBJECT, true);

    SimpleAttributeDefinition HANDOFF_EXECUTOR = new SimpleAttributeDefinition(CommonAttributes.HANDOFF_EXECUTOR, ModelType.STRING, true);

    SimpleAttributeDefinition QUEUE_LENGTH = new SimpleAttributeDefinition(CommonAttributes.QUEUE_LENGTH, ModelType.OBJECT, false);

    SimpleAttributeDefinition BLOCKING = new SimpleAttributeDefinition(CommonAttributes.BLOCKING, ModelType.BOOLEAN, true);

    SimpleAttributeDefinition ALLOW_CORE_TIMEOUT = new SimpleAttributeDefinition(CommonAttributes.ALLOW_CORE_TIMEOUT, ModelType.BOOLEAN, true);

    SimpleAttributeDefinition GROUP_NAME = new SimpleAttributeDefinition(CommonAttributes.GROUP_NAME, ModelType.STRING, true);

    SimpleAttributeDefinition THREAD_NAME_PATTERN = new SimpleAttributeDefinition(CommonAttributes.THREAD_NAME_PATTERN, ModelType.STRING, true);

    SimpleAttributeDefinition PRIORITY = new SimpleAttributeDefinition(CommonAttributes.PRIORITY, CommonAttributes.PRIORITY, new ModelNode().set(-1),
            ModelType.INT, true, false, MeasurementUnit.NONE, new AbstractParameterValidator(){
                @Override
                public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
                    if (value.isDefined() /*&& value.getType() != EXPRESSION*/) {
                        final int priority = value.asInt();
                        if (priority != -1 && priority < 0 || priority > 10) {
                            throw new OperationFailedException(new ModelNode().set(PRIORITY + " is out of range " + priority)); //TODO i18n
                        }
                    }
                }});
}
