/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.naming.subsystem.NamingExtension.SUBSYSTEM_PATH;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING_TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.EXTERNAL_CONTEXT;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.LOOKUP;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.OBJECT_FACTORY;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.SIMPLE;

import java.util.EnumSet;
import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author Eduardo Martins
 */
public class NamingSubsystem14Parser implements XMLElementReader<List<ModelNode>> {

    private final NamingSubsystemNamespace validNamespace;

    NamingSubsystem14Parser() {
        this.validNamespace = NamingSubsystemNamespace.NAMING_1_4;
    }

    NamingSubsystem14Parser(NamingSubsystemNamespace validNamespace) {
        this.validNamespace = validNamespace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {

        PathAddress address = PathAddress.pathAddress(SUBSYSTEM_PATH);
        final ModelNode ejb3SubsystemAddOperation = Util.createAddOperation(address);
        operations.add(ejb3SubsystemAddOperation);

        // elements
        final EnumSet<NamingSubsystemXMLElement> encountered = EnumSet.noneOf(NamingSubsystemXMLElement.class);
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (validNamespace == NamingSubsystemNamespace.forUri(reader.getNamespaceURI())) {
                final NamingSubsystemXMLElement element = NamingSubsystemXMLElement.forName(reader.getLocalName());
                if (!encountered.add(element)) {
                    throw unexpectedElement(reader);
                }
                switch (element) {
                    case BINDINGS: {
                        parseBindings(reader, operations, address);
                        break;
                    }
                    case REMOTE_NAMING: {
                        parseRemoteNaming(reader, operations, address);
                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);
                    }
                }
            }
            else {
                throw unexpectedElement(reader);
            }
        }
    }

    private void parseRemoteNaming(final XMLExtendedStreamReader reader, final List<ModelNode> operations, PathAddress parent) throws XMLStreamException {
        requireNoAttributes(reader);
        requireNoContent(reader);
        operations.add(Util.createAddOperation(parent.append(NamingSubsystemModel.SERVICE, NamingSubsystemModel.REMOTE_NAMING)));
    }


    private void parseBindings(final XMLExtendedStreamReader reader, final List<ModelNode> operations, PathAddress address) throws XMLStreamException {
        // no attributes expected
        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (NamingSubsystemXMLElement.forName(reader.getLocalName())) {
                case SIMPLE: {
                    this.parseSimpleBinding(reader, operations, address);
                    break;
                }
                case OBJECT_FACTORY: {
                    this.parseObjectFactoryBinding(reader, operations, address);
                    break;
                }
                case LOOKUP: {
                    this.parseLookupBinding(reader, operations, address);
                    break;
                }
                case EXTERNAL_CONTEXT: {
                    this.parseExternalContext(reader, operations, address);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }


    private void parseSimpleBinding(final XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress parentAddress) throws XMLStreamException {
        String name = null;
        final ModelNode bindingAdd = Util.createAddOperation();
        bindingAdd.get(BINDING_TYPE).set(SIMPLE);
        final EnumSet<NamingSubsystemXMLAttribute> required = EnumSet.of(NamingSubsystemXMLAttribute.NAME, NamingSubsystemXMLAttribute.VALUE);
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final NamingSubsystemXMLAttribute attribute = NamingSubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = value.trim();
                    break;
                case VALUE:
                    NamingBindingResourceDefinition.VALUE.parseAndSetParameter(value, bindingAdd, reader);
                    break;
                case TYPE:
                    NamingBindingResourceDefinition.TYPE.parseAndSetParameter(value, bindingAdd, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        requireNoContent(reader);
        final PathAddress address = parentAddress.append(BINDING, name);
        bindingAdd.get(OP_ADDR).set(address.toModelNode());
        operations.add(bindingAdd);
    }


    private void parseObjectFactoryBinding(final XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress parentAddress) throws XMLStreamException {
        final int attCount = reader.getAttributeCount();
        String name = null;
        final ModelNode bindingAdd = Util.createAddOperation();
        bindingAdd.get(BINDING_TYPE).set(OBJECT_FACTORY);
        final EnumSet<NamingSubsystemXMLAttribute> required = EnumSet.of(NamingSubsystemXMLAttribute.NAME, NamingSubsystemXMLAttribute.MODULE, NamingSubsystemXMLAttribute.CLASS);
        for (int i = 0; i < attCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final NamingSubsystemXMLAttribute attribute = NamingSubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = value.trim();
                    break;
                case MODULE:
                    NamingBindingResourceDefinition.MODULE.parseAndSetParameter(value, bindingAdd, reader);
                    break;
                case CLASS:
                    NamingBindingResourceDefinition.CLASS.parseAndSetParameter(value, bindingAdd, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        bindingAdd.get(OP_ADDR).set(parentAddress.append(BINDING, name).toModelNode());
        // if present, parse the optional environment
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (NamingSubsystemXMLElement.forName(reader.getLocalName())) {
                case ENVIRONMENT: {
                    parseObjectFactoryBindingEnvironment(reader, bindingAdd);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        operations.add(bindingAdd);
    }


    private void parseExternalContext(final XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress parentAddress) throws XMLStreamException {
        final int attCount = reader.getAttributeCount();
        String name = null;
        final ModelNode bindingAdd = Util.createAddOperation();
        bindingAdd.get(BINDING_TYPE).set(EXTERNAL_CONTEXT);
        final EnumSet<NamingSubsystemXMLAttribute> required = EnumSet.of(NamingSubsystemXMLAttribute.NAME, NamingSubsystemXMLAttribute.CLASS, NamingSubsystemXMLAttribute.MODULE);
        for (int i = 0; i < attCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final NamingSubsystemXMLAttribute attribute = NamingSubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = value.trim();
                    break;
                case MODULE:
                    NamingBindingResourceDefinition.MODULE.parseAndSetParameter(value, bindingAdd, reader);
                    break;
                case CLASS:
                    NamingBindingResourceDefinition.CLASS.parseAndSetParameter(value, bindingAdd, reader);
                    break;
                case CACHE:
                    NamingBindingResourceDefinition.CACHE.parseAndSetParameter(value, bindingAdd, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        bindingAdd.get(OP_ADDR).set(parentAddress.append(BINDING, name).toModelNode());
        // if present, parse the optional environment
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (NamingSubsystemXMLElement.forName(reader.getLocalName())) {
                case ENVIRONMENT: {
                    parseObjectFactoryBindingEnvironment(reader, bindingAdd);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        operations.add(bindingAdd);
    }

    /**
     * <p>
     * Parses the optional {@code ObjectFactory environment}.
     * </p>
     *
     * @param reader     the {@code XMLExtendedStreamReader} used to read the configuration XML.
     * @param bindingAdd where to add
     * @throws javax.xml.stream.XMLStreamException
     *          if an error occurs while parsing the XML.
     */
    private void parseObjectFactoryBindingEnvironment(XMLExtendedStreamReader reader, ModelNode bindingAdd) throws XMLStreamException {
        // no attributes expected
        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (NamingSubsystemXMLElement.forName(reader.getLocalName())) {
                case ENVIRONMENT_PROPERTY: {
                    final String[] array = requireAttributes(reader, org.jboss.as.controller.parsing.Attribute.NAME.getLocalName(), org.jboss.as.controller.parsing.Attribute.VALUE.getLocalName());
                    NamingBindingResourceDefinition.ENVIRONMENT.parseAndAddParameterElement(array[0], array[1], bindingAdd, reader);
                    requireNoContent(reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseLookupBinding(final XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress parentAddress) throws XMLStreamException {
        final int attCount = reader.getAttributeCount();
        String name = null;
        final ModelNode bindingAdd = Util.createAddOperation();
        bindingAdd.get(BINDING_TYPE).set(LOOKUP);
        final EnumSet<NamingSubsystemXMLAttribute> required = EnumSet.of(NamingSubsystemXMLAttribute.NAME, NamingSubsystemXMLAttribute.LOOKUP);
        for (int i = 0; i < attCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final NamingSubsystemXMLAttribute attribute = NamingSubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = value.trim();
                    break;
                case LOOKUP:
                    NamingBindingResourceDefinition.LOOKUP.parseAndSetParameter(value, bindingAdd, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        requireNoContent(reader);
        bindingAdd.get(OP_ADDR).set(parentAddress.append(BINDING, name).toModelNode());
        operations.add(bindingAdd);
    }
}
