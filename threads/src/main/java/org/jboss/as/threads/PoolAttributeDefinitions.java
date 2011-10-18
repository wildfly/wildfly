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


import static org.jboss.as.threads.CommonAttributes.TIME;
import static org.jboss.as.threads.CommonAttributes.UNIT;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PropagatingCorrector;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
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

    SimpleAttributeDefinition MAX_THREADS = new SimpleAttributeDefinitionBuilder(CommonAttributes.MAX_THREADS, ModelType.INT, false)
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, false, true)).setAllowExpression(true).build();

    SimpleAttributeDefinition KEEPALIVE_TIME = new SimpleAttributeDefinition(CommonAttributes.KEEPALIVE_TIME, ModelType.OBJECT, true,
            PropagatingCorrector.INSTANCE, new ParameterValidator(){
                @Override
                public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
                    if(value.getType() == ModelType.UNDEFINED) {
                        return;
                    }
                    if(value.getType() != ModelType.OBJECT) {
                        throw new OperationFailedException("Attribute " + parameterName +
                                " expects values of type OBJECT but got " + value + " of type " + value.getType());
                    }
                    final Set<String> keys = value.keys();
                    if(keys.size() != 2) {
                        throw new OperationFailedException("Attribute " + parameterName +
                                " expects values consisting of '" + TIME +
                                "' and '" + UNIT + "' but the new value consists of " + keys);
                    }
                    if (!keys.contains(TIME)) {
                        throw new OperationFailedException("Missing '" + TIME + "' for '" + parameterName + "'");
                    }
                    if (!keys.contains(UNIT)) {
                        throw new OperationFailedException("Missing '" + UNIT + "' for '" + parameterName + "'");
                    }
                }
                @Override
                public void validateResolvedParameter(String parameterName, ModelNode value) throws OperationFailedException {
                    validateParameter(parameterName, value);
                }});

    SimpleAttributeDefinition CORE_THREADS = new SimpleAttributeDefinitionBuilder(CommonAttributes.CORE_THREADS, ModelType.INT, true)
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, true)).setAllowExpression(true).build();

    SimpleAttributeDefinition HANDOFF_EXECUTOR = new SimpleAttributeDefinition(CommonAttributes.HANDOFF_EXECUTOR, ModelType.STRING, true);

    SimpleAttributeDefinition QUEUE_LENGTH = new SimpleAttributeDefinitionBuilder(CommonAttributes.QUEUE_LENGTH, ModelType.INT, false)
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, false, true)).setAllowExpression(true).build();

    SimpleAttributeDefinition BLOCKING = new SimpleAttributeDefinitionBuilder(CommonAttributes.BLOCKING, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false)).build();

    SimpleAttributeDefinition ALLOW_CORE_TIMEOUT = new SimpleAttributeDefinitionBuilder(CommonAttributes.ALLOW_CORE_TIMEOUT, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false)).build();

    SimpleAttributeDefinition GROUP_NAME = new SimpleAttributeDefinition(CommonAttributes.GROUP_NAME, ModelType.STRING, true);

    SimpleAttributeDefinition THREAD_NAME_PATTERN = new SimpleAttributeDefinition(CommonAttributes.THREAD_NAME_PATTERN, ModelType.STRING, true);

    SimpleAttributeDefinition PRIORITY = new SimpleAttributeDefinition(CommonAttributes.PRIORITY, CommonAttributes.PRIORITY, new ModelNode().set(-1),
            ModelType.INT, true, true, MeasurementUnit.NONE, new IntRangeValidator(-1, 10, true, true));

    AttributeDefinition[] THREAD_FACTORY_ATTRIBUTES = new AttributeDefinition[]{
            NAME, PROPERTIES, GROUP_NAME, THREAD_NAME_PATTERN, PoolAttributeDefinitions.PRIORITY
    };
}
