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

package org.wildfly.extension.security.manager;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.security.manager.Constants.MAXIMUM_SET;
import static org.wildfly.extension.security.manager.Constants.MINIMUM_SET;
import static org.wildfly.extension.security.manager.Constants.PERMISSION;
import static org.wildfly.extension.security.manager.Constants.PERMISSION_ACTIONS;
import static org.wildfly.extension.security.manager.Constants.PERMISSION_CLASS;
import static org.wildfly.extension.security.manager.Constants.PERMISSION_MODULE;
import static org.wildfly.extension.security.manager.Constants.PERMISSION_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Defines a resource that represents the security permissions that can be assigned to deployments.
 *
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 */
class DeploymentPermissionsResourceDefinition extends PersistentResourceDefinition {


    static final SimpleAttributeDefinition CLASS = new SimpleAttributeDefinitionBuilder(PERMISSION_CLASS, ModelType.STRING)
            .setAllowNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(PERMISSION_NAME, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition ACTIONS = new SimpleAttributeDefinitionBuilder(PERMISSION_ACTIONS, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(PERMISSION_MODULE, ModelType.STRING)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    private static final ObjectTypeAttributeDefinition PERMISSIONS_VALUE_TYPE =
            ObjectTypeAttributeDefinition.Builder.of("--this name is ignored--", CLASS, NAME, ACTIONS, MODULE).build();

    private static final AttributeDefinition MAXIMUM_PERMISSIONS =
            ObjectListAttributeDefinition.Builder.of(Constants.MAXIMUM_PERMISSIONS, PERMISSIONS_VALUE_TYPE)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    private static final AttributeDefinition MINIMUM_PERMISSIONS =
            ObjectListAttributeDefinition.Builder.of(Constants.MINIMUM_PERMISSIONS, PERMISSIONS_VALUE_TYPE)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final PathElement DEPLOYMENT_PERMISSIONS_PATH = PathElement.pathElement(
            Constants.DEPLOYMENT_PERMISSIONS, Constants.DEFAULT_VALUE);

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] {MINIMUM_PERMISSIONS, MAXIMUM_PERMISSIONS};

    static final DeploymentPermissionsResourceDefinition INSTANCE = new DeploymentPermissionsResourceDefinition();

    private DeploymentPermissionsResourceDefinition() {
        super(DEPLOYMENT_PERMISSIONS_PATH, SecurityManagerExtension.getResolver(Constants.DEPLOYMENT_PERMISSIONS),
                new AbstractAddStepHandler(ATTRIBUTES), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.unmodifiableCollection(Arrays.asList(ATTRIBUTES));
    }

    /**
     * A {@link PersistentResourceXMLDescription} implementation that knows how to parse/write security permission elements.
     *
     * From what I (Kabir) can tell it isn't possible to parse a complex list attributes mapped by child xml elements, so I am doing it myself
     */
    static class DeploymentPermissionsResourceXMLDescription extends PersistentResourceXMLDescription {

        protected DeploymentPermissionsResourceXMLDescription(final PersistentResourceDefinition resourceDefinition, final String xmlElementName,
                                                              final String xmlWrapperElement, final LinkedHashMap<String, AttributeDefinition> attributes,
                                                              final List<PersistentResourceXMLDescription> children, final boolean useValueAsElementName,
                                                              final boolean noAddOperation, final AdditionalOperationsGenerator additionalOperationsGenerator) {
            super(resourceDefinition, xmlElementName, xmlWrapperElement, attributes, children, useValueAsElementName, noAddOperation, additionalOperationsGenerator, null);
        }

        @Override
        public void parse(final XMLExtendedStreamReader reader, final PathAddress parentAddress, final List<ModelNode> list) throws XMLStreamException {


            ModelNode add = Util.createAddOperation();
            add.get(OP_ADDR).set(parentAddress.append(resourceDefinition.getPathElement()).toModelNode());

            ParseUtils.requireNoAttributes(reader);

            list.add(add);

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (reader.getLocalName()) {
                    case MAXIMUM_SET: {
                        add.get(MAXIMUM_PERMISSIONS.getName()).set(parsePermissionSet(reader));
                        break;
                    }
                    case MINIMUM_SET: {
                        add.get(MINIMUM_PERMISSIONS.getName()).set(parsePermissionSet(reader));
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
        }

        private ModelNode parsePermissionSet(final XMLExtendedStreamReader reader) throws XMLStreamException {
            ParseUtils.requireNoAttributes(reader);

            ModelNode permissions = new ModelNode();
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (reader.getLocalName()) {
                    case PERMISSION: {
                        permissions.add(parsePermission(reader));
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
            return permissions;
        }

        private ModelNode parsePermission(final XMLExtendedStreamReader reader) throws XMLStreamException {
            ModelNode permission = new ModelNode();
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                switch (reader.getAttributeLocalName(i)) {
                    case PERMISSION_CLASS:
                        CLASS.parseAndSetParameter(value, permission, reader);
                        break;
                    case PERMISSION_ACTIONS: {
                        ACTIONS.parseAndSetParameter(value, permission, reader);
                        break;
                    }
                    case PERMISSION_MODULE: {
                        MODULE.parseAndSetParameter(value, permission, reader);
                        break;
                    }
                    case PERMISSION_NAME: {
                        NAME.parseAndSetParameter(value, permission, reader);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }

            if (!permission.hasDefined(PERMISSION_CLASS)) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(PERMISSION_CLASS));
            }
            ParseUtils.requireNoContent(reader);
            return permission;
        }

        @Override
        public void persist(final XMLExtendedStreamWriter writer, final ModelNode model, final String namespaceURI) throws XMLStreamException {
            if (!model.hasDefined(DEPLOYMENT_PERMISSIONS_PATH.getKey(), DEPLOYMENT_PERMISSIONS_PATH.getValue())) {
                return;
            }
            ModelNode deploymentPermissions = model.get(DEPLOYMENT_PERMISSIONS_PATH.getKey(), DEPLOYMENT_PERMISSIONS_PATH.getValue());
            writer.writeStartElement(xmlElementName);
            if (deploymentPermissions.hasDefined(MINIMUM_PERMISSIONS.getName())) {
                persistPermissionSet(writer, deploymentPermissions.get(MINIMUM_PERMISSIONS.getName()), MINIMUM_SET);
            }
            if (deploymentPermissions.hasDefined(MAXIMUM_PERMISSIONS.getName())) {
                persistPermissionSet(writer, deploymentPermissions.get(MAXIMUM_PERMISSIONS.getName()), MAXIMUM_SET);
            }
            writer.writeEndElement();
        }

        private void persistPermissionSet(final XMLExtendedStreamWriter writer, final ModelNode model, final String name) throws XMLStreamException {
            if (model.isDefined()) {
                writer.writeStartElement(name);
                for (ModelNode permission : model.asList()) {
                    writer.writeStartElement(PERMISSION);
                    CLASS.marshallAsAttribute(permission, writer);
                    NAME.marshallAsAttribute(permission, writer);
                    ACTIONS.marshallAsAttribute(permission, writer);
                    MODULE.marshallAsAttribute(permission, writer);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        }
    }

    /**
     * A {@link PersistentResourceXMLDescription.PersistentResourceXMLBuilder} that creates creates {@link DeploymentPermissionsResourceXMLDescription} instances.
     */
    static class DeploymentPermissionsResourceXMLBuilder extends PersistentResourceXMLDescription.PersistentResourceXMLBuilder {

        protected DeploymentPermissionsResourceXMLBuilder() {
            super(DeploymentPermissionsResourceDefinition.INSTANCE);
        }

        @Override
        public PersistentResourceXMLDescription build() {
            return new DeploymentPermissionsResourceXMLDescription(resourceDefinition, xmlElementName, xmlWrapperElement,
                    attributes, new ArrayList<PersistentResourceXMLDescription>(), useValueAsElementName, noAddOperation,
                    additionalOperationsGenerator);
        }
    }
}
