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

package org.jboss.as.host.controller.ignored;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.host.controller.descriptions.HostRootDescription;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for an ignored domain resource type.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class IgnoredDomainTypeResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition WILDCARD = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.WILDCARD, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false)).setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    public static final ListAttributeDefinition NAMES = new ListAttributeDefinition(ModelDescriptionConstants.NAMES, true, new StringLengthValidator(1), AttributeAccess.Flag.RESTART_ALL_SERVICES) {
        @Override
        protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
            this.setValueType(node);
        }

        @Override
        protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            this.setValueType(node);
        }

        @Override
        protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            this.setValueType(node);
        }

        @Override
        public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
            throw new UnsupportedOperationException();
        }

        private void setValueType(ModelNode node) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        }
    };

    IgnoredDomainTypeResourceDefinition() {
        super(PathElement.pathElement(IGNORED_RESOURCE_TYPE), HostRootDescription.getResourceDescriptionResolver(IGNORED_RESOURCE_TYPE),
                new IgnoredDomainTypeAddHandler(),
                new IgnoredDomainTypeRemoveHandler(),
                OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler writeHandler = new IgnoredDomainTypeWriteAttributeHandler();
        resourceRegistration.registerReadWriteAttribute(WILDCARD, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(NAMES, null, writeHandler);
    }
}
