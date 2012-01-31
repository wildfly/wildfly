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
package org.jboss.as.ejb3.subsystem;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public class CacheFactoryResourceDefinition extends SimpleResourceDefinition {

    public static final ListAttributeDefinition ALIASES = new StringListAttributeDefinition(EJB3SubsystemModel.ALIASES, EJB3SubsystemXMLAttribute.ALIASES.getLocalName(), true);
    public static final SimpleAttributeDefinition PASSIVATION_STORE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.PASSIVATION_STORE, ModelType.STRING, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.PASSIVATION_STORE_REF.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { ALIASES, PASSIVATION_STORE };
    private static final CacheFactoryAdd ADD_HANDLER = new CacheFactoryAdd(ATTRIBUTES);
    private static final CacheFactoryRemove REMOVE_HANDLER = new CacheFactoryRemove(ADD_HANDLER);

    public static final CacheFactoryResourceDefinition INSTANCE = new CacheFactoryResourceDefinition();

    private CacheFactoryResourceDefinition() {
        super(PathElement.pathElement(EJB3SubsystemModel.CACHE),
                EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.CACHE),
                ADD_HANDLER, REMOVE_HANDLER,
                OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute: ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attribute,  handler);
        }
    }

    private static class StringListAttributeDefinition extends ListAttributeDefinition {

        /** {@inheritDoc} */
        StringListAttributeDefinition(String name, String xmlName, boolean allowNull) {
            super(name, xmlName, allowNull, 0, Integer.MAX_VALUE, new StringLengthValidator(1));
        }

        /**
         * {@inheritDoc}
         * @see org.jboss.as.controller.ListAttributeDefinition#addValueTypeDescription(org.jboss.dmr.ModelNode, java.util.ResourceBundle)
         */
        @Override
        protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
            this.setValueType(node);
        }

        /**
         * {@inheritDoc}
         * @see org.jboss.as.controller.ListAttributeDefinition#addAttributeValueTypeDescription(org.jboss.dmr.ModelNode, org.jboss.as.controller.descriptions.ResourceDescriptionResolver, java.util.Locale, java.util.ResourceBundle)
         */
        @Override
        protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            this.setValueType(node);
        }

        /**
         * {@inheritDoc}
         * @see org.jboss.as.controller.ListAttributeDefinition#addOperationParameterValueTypeDescription(org.jboss.dmr.ModelNode, java.lang.String, org.jboss.as.controller.descriptions.ResourceDescriptionResolver, java.util.Locale, java.util.ResourceBundle)
         */
        @Override
        protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            this.setValueType(node);
        }

        private void setValueType(ModelNode node) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        }

        /**
         * {@inheritDoc}
         * @see org.jboss.as.controller.AttributeDefinition#marshallAsElement(org.jboss.dmr.ModelNode, javax.xml.stream.XMLStreamWriter)
         */
        @Override
        public void marshallAsElement(ModelNode model, XMLStreamWriter writer) throws XMLStreamException {
            if (model.hasDefined(this.getName())) {
                StringBuilder builder = new StringBuilder();
                for (ModelNode alias: model.get(this.getName()).asList()) {
                    if (builder.length() > 0) {
                        builder.append(' ');
                    }
                    builder.append(alias.asString());
                }
                writer.writeAttribute(this.getXmlName(), builder.toString());
            }
        }
    }
}
