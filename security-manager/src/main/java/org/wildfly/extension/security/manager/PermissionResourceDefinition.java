/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.security.manager;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.wildfly.extension.security.manager.Constants.PERMISSION;
import static org.wildfly.extension.security.manager.Constants.PERMISSION_ACTIONS;
import static org.wildfly.extension.security.manager.Constants.PERMISSION_CLASS;
import static org.wildfly.extension.security.manager.Constants.PERMISSION_NAME;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimplePersistentResourceDefinition;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Defines a resource that represents a single security permission.
 *
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 */
class PermissionResourceDefinition extends SimplePersistentResourceDefinition {

    static final AttributeDefinition CLASS = new SimpleAttributeDefinitionBuilder(PERMISSION_CLASS, ModelType.STRING)
            .setAllowNull(false)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final AttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(PERMISSION_NAME, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final AttributeDefinition ACTIONS = new SimpleAttributeDefinitionBuilder(PERMISSION_ACTIONS, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = Arrays.asList(CLASS, NAME, ACTIONS);

    static final PermissionResourceDefinition INSTANCE = new PermissionResourceDefinition();

    private PermissionResourceDefinition() {
        super(PathElement.pathElement(PERMISSION), SecurityManagerExtension.getResolver(PERMISSION),
                new AbstractAddStepHandler(ATTRIBUTES), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    @Override
    public String getXmlElementName() {
        return Constants.PERMISSION;
    }

    /**
     * Override the {@code parse} method so we can create a unique name for this permission resource. The default parser
     * behavior relies on the {@code name} attribute of a resource to build a unique name for wildcard resources. The
     * permission name can't be used as a unique id because it is optional and multiple permissions can have the same
     * name (target). In this method, we generate the unique name by using all permission attributes (class, name, actions).
     *
     * @param reader        the {@link XMLExtendedStreamReader} to be used to parse the permission.
     * @param parentAddress the address of the parent resource (usually the permission set containing the permission).
     * @param list          the list of operations that results from parsing the resources.
     * @throws XMLStreamException if an error occurs while parsing the permission.
     */
    @Override
    public void parse(final XMLExtendedStreamReader reader, final PathAddress parentAddress, final List<ModelNode> list) throws XMLStreamException {

        if (getXmlWrapperElement() != null) {
            if (reader.getLocalName().equals(getXmlWrapperElement())) {
                if (reader.hasNext()) {
                    if (reader.nextTag() == END_ELEMENT) {
                        return;
                    }
                }
            } else {
                throw ParseUtils.unexpectedElement(reader);
            }

        }
        ModelNode op = Util.createAddOperation();
        Map<String, AttributeDefinition> definedAttributes = getAttributeMap();
        Map<String, String> parsedAttributes = new HashMap<String, String>();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attributeName = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            if (definedAttributes.containsKey(attributeName)) {
                parsedAttributes.put(attributeName, value);
                SimpleAttributeDefinition def = (SimpleAttributeDefinition) definedAttributes.get(attributeName);
                def.parseAndSetParameter(value, op, reader);
            } else {
                throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        // validate the permissions - at least the class must have been specified.
        if (parsedAttributes.get(PERMISSION_CLASS) == null)
            throw ParseUtils.missingRequired(reader, PERMISSION_CLASS);

        // create the unique name for this permission resource.
        String resourceName = parsedAttributes.get(PERMISSION_CLASS) + "|" + parsedAttributes.get(PERMISSION_NAME) +
                "|" + parsedAttributes.get(PERMISSION_ACTIONS);

        // set the PathAddress for the new permission resource and add it to the list of operations.
        PathElement path = PathElement.pathElement(getPathElement().getKey(), resourceName);
        PathAddress address = parentAddress.append(path);
        op.get(ADDRESS).set(address.toModelNode());
        list.add(op);
        parseChildren(reader, address, list);
        if (getXmlWrapperElement() != null) {
            ParseUtils.requireNoContent(reader);
        }
    }

    /**
     * Override the {@code persist} method to avoid writing two different {@code name} attributes in a {@code permission}
     * element. The original implementation always writes a {@code name} attribute containing the resource unique name.
     * We don't want that as the permission name is not used as the resource unique name.
     *
     * @param writer       the {@link XMLExtendedStreamWriter} to be used to write the permission.
     * @param model        the {@link ModelNode} containing the permission data.
     * @param namespaceURI the namespace URI.
     * @throws XMLStreamException if an error occurs while writing the permission.
     */
    @Override
    public void persist(XMLExtendedStreamWriter writer, ModelNode model, String namespaceURI) throws XMLStreamException {
        model = model.get(getPathElement().getKey());
        if (!model.isDefined() && !useValueAsElementName()) {
            return;
        }

        for (Property p : model.asPropertyList()) {
            if (namespaceURI != null) {
                writer.writeStartElement(namespaceURI, getXmlElementName());
            } else {
                writer.writeStartElement(getXmlElementName());
            }
            for (AttributeDefinition def : getAttributes()) {
                def.getAttributeMarshaller().marshallAsAttribute(def, p.getValue(), false, writer);
            }
            persistChildren(writer, p.getValue());
            writer.writeEndElement();
        }
    }
}
