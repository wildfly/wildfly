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

package org.jboss.as.remoting;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.MapAttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Jaikiran Pai
 */
abstract class AbstractOutboundConnectionResourceDefinition extends SimpleResourceDefinition {

    public static final MapAttributeDefinition CONNECTION_CREATION_OPTIONS =
            new PropertiesAttributeDefinition(CommonAttributes.CONNECTION_CREATION_OPTIONS,
                    Element.CONNECTION_CREATION_OPTIONS.getLocalName(), true, null, null);

    protected AbstractOutboundConnectionResourceDefinition(final PathElement pathElement, final ResourceDescriptionResolver descriptionResolver,
                                                           final OperationStepHandler addHandler, final OperationStepHandler removeHandler) {
        super(pathElement, descriptionResolver, addHandler, removeHandler);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(CONNECTION_CREATION_OPTIONS, null, this.getWriteAttributeHandler(CONNECTION_CREATION_OPTIONS));
    }

    /**
     * Returns the write attribute handler for the <code>attribute</code>
     * @param attribute The attribute for which the write operation handler is being queried
     * @return the handler
     */
    protected abstract OperationStepHandler getWriteAttributeHandler(final AttributeDefinition attribute);

    /**
     * This is just a stop-gap solution for {@link ModelType#PROPERTY} type attributes.
     * This class needs to be moved to a better place and the marshallAsElement should be implemented.
     * For now, we don't use the {@link #marshallAsElement(org.jboss.dmr.ModelNode, javax.xml.stream.XMLStreamWriter)}
     */
    private static class PropertiesAttributeDefinition extends MapAttributeDefinition {

        PropertiesAttributeDefinition(final String name, final String xmlName, boolean allowNull,final String[] alternatives,
                                      final String[] requires, final AttributeAccess.Flag... flags) {
            super(name, xmlName, allowNull, 0, Integer.MAX_VALUE, new ModelTypeValidator(ModelType.STRING), alternatives, requires, flags);
        }

        @Override
        protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        }

        @Override
        protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        }

        @Override
        protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        }

        // TODO: We don't currently support marshalling. This entire attribute definition needs to be
        // moved at a better place and the subsystem writers/marshallers should start relying on these attribute
        // definitions for marshalling. For now, let the subsystem writers/marshallers handle the marshalling
        @Override
        public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
            throw new RuntimeException("marshallAsElement isn't supported for " + this.getClass().getName());
        }
    }
}
