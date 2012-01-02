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

package org.jboss.as.controller.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for an {@link Extension}'s subsytem child resources.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ExtensionSubsystemResourceDefinition extends SimpleResourceDefinition {

    public static final ListAttributeDefinition XML_NAMESPACES =
            new ListAttributeDefinition("xml-namespaces", false,
                    new StringLengthValidator(1, Integer.MAX_VALUE, false, false), AttributeAccess.Flag.STORAGE_RUNTIME) {
        @Override
        protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
            setValueType(node);
        }

        @Override
        protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            setValueType(node);
        }

        @Override
        protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            setValueType(node);
        }

        @Override
        public boolean isMarshallable(ModelNode resourceModel) {
            return false;
        }

        @Override
        public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
            // no-op
        }

        private void setValueType(ModelNode node) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        }
    };

    public static final SimpleAttributeDefinition MAJOR_VERSION = new SimpleAttributeDefinitionBuilder(MANAGEMENT_MAJOR_VERSION, ModelType.INT, true)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();

    public static final SimpleAttributeDefinition MINOR_VERSION = new SimpleAttributeDefinitionBuilder(MANAGEMENT_MINOR_VERSION, ModelType.INT, true)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();

    ExtensionSubsystemResourceDefinition() {
        super(PathElement.pathElement(SUBSYSTEM), CommonDescriptions.getResourceDescriptionResolver(EXTENSION + "." + SUBSYSTEM));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(MAJOR_VERSION, null);
        resourceRegistration.registerReadOnlyAttribute(MINOR_VERSION, null);
        resourceRegistration.registerReadOnlyAttribute(XML_NAMESPACES, null);
    }
}
