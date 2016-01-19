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
package org.wildfly.extension.picketlink.federation.model.parser;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.XMLElement;
import org.wildfly.extension.picketlink.federation.FederationExtension;
import org.wildfly.extension.picketlink.federation.Namespace;
import org.wildfly.extension.picketlink.federation.model.FederationResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.handlers.HandlerParameterResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.handlers.HandlerResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.idp.AttributeManagerResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.idp.IdentityProviderResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.idp.RoleGeneratorResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.idp.TrustDomainResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.keystore.KeyStoreProviderResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.saml.SAMLResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.sp.ServiceProviderResourceDefinition;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateNamedElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.wildfly.extension.picketlink.common.model.ModelElement.COMMON_HANDLER;
import static org.wildfly.extension.picketlink.common.model.ModelElement.COMMON_HANDLER_PARAMETER;
import static org.wildfly.extension.picketlink.common.model.ModelElement.COMMON_NAME;
import static org.wildfly.extension.picketlink.common.model.ModelElement.FEDERATION;
import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_PROVIDER;
import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_PROVIDER_ATTRIBUTE_MANAGER;
import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_PROVIDER_ROLE_GENERATOR;
import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_PROVIDER_TRUST_DOMAIN;
import static org.wildfly.extension.picketlink.common.model.ModelElement.KEY_STORE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.SAML;
import static org.wildfly.extension.picketlink.common.model.ModelElement.SERVICE_PROVIDER;

/**
 * @author Pedro Igor
 */
public abstract class AbstractFederationSubsystemReader implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> addOperations) throws XMLStreamException {
        requireNoAttributes(reader);

        Namespace nameSpace = Namespace.forUri(reader.getNamespaceURI());

        ModelNode subsystemNode = createSubsystemRoot();

        addOperations.add(subsystemNode);

        switch (nameSpace) {
            case PICKETLINK_FEDERATION_1_0:
                this.readElement(reader, subsystemNode, addOperations);
                break;
            case PICKETLINK_FEDERATION_1_1:
            case PICKETLINK_FEDERATION_2_0:
                this.readElement(reader, subsystemNode, addOperations);
                break;
            default:
                throw unexpectedElement(reader);
        }
    }

    private void readElement(XMLExtendedStreamReader reader, ModelNode subsystemNode, List<ModelNode> addOperations)
        throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_DOCUMENT) {
            if (!reader.isStartElement()) {
                continue;
            }

            // if the current element is supported but is not a model element
            if (XMLElement.forName(reader.getLocalName()) != null) {
                continue;
            }

            ModelElement modelKey = ModelElement.forName(reader.getLocalName());

            if (modelKey == null) {
                throw unexpectedElement(reader);
            }

            switch (modelKey) {
                case FEDERATION:
                    parseFederation(reader, subsystemNode, addOperations);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseFederation(final XMLExtendedStreamReader reader, final ModelNode subsystemNode,
        final List<ModelNode> addOperations) throws XMLStreamException {
        ModelNode federationNode = parseConfig(reader, FEDERATION, COMMON_NAME.getName(), subsystemNode,
            Arrays.asList(FederationResourceDefinition.ATTRIBUTE_DEFINITIONS), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case KEY_STORE:
                        parseKeyStore(reader, parentNode, addOperations);
                        break;
                    case SAML:
                        parseConfig(reader, SAML, null, parentNode, SAMLResourceDefinition.INSTANCE.getAttributes(),
                            addOperations);
                        break;
                    case IDENTITY_PROVIDER:
                        parseIdentityProviderConfig(reader, parentNode, addOperations);
                        break;
                    case SERVICE_PROVIDER:
                        parseServiceProviderConfig(reader, parentNode, addOperations);
                        break;
                    default:
                        throw unexpectedElement(reader);
                }
            }
        }, FEDERATION, federationNode, reader, addOperations);
    }

    protected void parseKeyStore(XMLExtendedStreamReader reader, ModelNode parentNode, List<ModelNode> addOperations) throws XMLStreamException {
        ModelNode identityProviderNode = parseConfig(reader, KEY_STORE, null, parentNode,
            KeyStoreProviderResourceDefinition.INSTANCE.getAttributes(), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                List<ModelNode> addOperations) throws XMLStreamException {
                throw unexpectedElement(reader);
            }
        }, KEY_STORE, identityProviderNode, reader, addOperations);
    }

    private void parseServiceProviderConfig(final XMLExtendedStreamReader reader, ModelNode federationNode,
        final List<ModelNode> addOperations) throws XMLStreamException {
        ModelNode serviceProviderNode = parseConfig(reader, SERVICE_PROVIDER,
            COMMON_NAME.getName(), federationNode, Arrays.asList(ServiceProviderResourceDefinition.ATTRIBUTE_DEFINITIONS), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case COMMON_HANDLER:
                        parseHandlerConfig(reader, parentNode, addOperations);
                        break;
                }
            }
        }, SERVICE_PROVIDER, serviceProviderNode, reader, addOperations);
    }

    protected void parseHandlerConfig(final XMLExtendedStreamReader reader, final ModelNode entityProviderNode, final List<ModelNode> addOperations) throws XMLStreamException {
        ModelNode handlerNode = parseConfig(reader, COMMON_HANDLER, COMMON_NAME.getName(), entityProviderNode, HandlerResourceDefinition.INSTANCE
            .getAttributes(), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case COMMON_HANDLER_PARAMETER:
                        parseConfig(reader, COMMON_HANDLER_PARAMETER, COMMON_NAME.getName(), parentNode,
                            HandlerParameterResourceDefinition.INSTANCE.getAttributes(), addOperations);
                        break;
                    default:
                        throw unexpectedElement(reader);
                }
            }
        }, COMMON_HANDLER, handlerNode, reader, addOperations);
    }

    private void parseIdentityProviderConfig(final XMLExtendedStreamReader reader, final ModelNode federationNode,
        final List<ModelNode> addOperations) throws XMLStreamException {
        ModelNode identityProviderNode = parseConfig(reader, IDENTITY_PROVIDER,
            COMMON_NAME.getName(), federationNode,
            Arrays.asList(IdentityProviderResourceDefinition.ATTRIBUTE_DEFINITIONS), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case IDENTITY_PROVIDER_TRUST_DOMAIN:
                        parseConfig(reader, IDENTITY_PROVIDER_TRUST_DOMAIN, COMMON_NAME.getName(), parentNode,
                            TrustDomainResourceDefinition.INSTANCE.getAttributes(), addOperations);
                        break;
                    case IDENTITY_PROVIDER_ROLE_GENERATOR:
                        parseConfig(reader, IDENTITY_PROVIDER_ROLE_GENERATOR, COMMON_NAME.getName(), parentNode,
                            RoleGeneratorResourceDefinition.INSTANCE.getAttributes(), addOperations);
                        break;
                    case IDENTITY_PROVIDER_ATTRIBUTE_MANAGER:
                        parseConfig(reader, IDENTITY_PROVIDER_ATTRIBUTE_MANAGER, COMMON_NAME.getName(), parentNode,
                            AttributeManagerResourceDefinition.INSTANCE.getAttributes(), addOperations);
                        break;
                    case COMMON_HANDLER:
                        parseHandlerConfig(reader, parentNode, addOperations);
                        break;
                    default:
                        throw unexpectedElement(reader);
                }
            }
        }, IDENTITY_PROVIDER, identityProviderNode, reader, addOperations);
    }

    /**
     * Creates the root subsystem's root address.
     *
     * @return
     */
    private ModelNode createSubsystemRoot() {
        ModelNode subsystemAddress = new ModelNode();

        subsystemAddress.add(ModelDescriptionConstants.SUBSYSTEM, FederationExtension.SUBSYSTEM_NAME);

        subsystemAddress.protect();

        return Util.getEmptyOperation(ADD, subsystemAddress);
    }

    /**
     * Reads a element from the stream considering the parameters.
     *
     * @param reader XMLExtendedStreamReader instance from which the elements are read.
     * @param xmlElement Name of the Model Element to be parsed.
     * @param key Name of the attribute to be used to as the key for the model.
     * @param addOperations List of operations.
     * @param lastNode Parent ModelNode instance.
     * @param attributes AttributeDefinition instances to be used to extract the attributes and populate the resulting model.
     *
     * @return A ModelNode instance populated.
     *
     * @throws javax.xml.stream.XMLStreamException
     */
    protected ModelNode parseConfig(XMLExtendedStreamReader reader, ModelElement xmlElement, String key, ModelNode lastNode,
        List<SimpleAttributeDefinition> attributes, List<ModelNode> addOperations) throws XMLStreamException {
        if (!reader.getLocalName().equals(xmlElement.getName())) {
            return null;
        }

        ModelNode modelNode = Util.getEmptyOperation(ADD, null);

        int attributeCount = reader.getAttributeCount();

        for (int i = 0; i < attributeCount; i++) {
            String attributeLocalName = reader.getAttributeLocalName(i);

            if (ModelElement.forName(attributeLocalName) == null) {
                throw unexpectedAttribute(reader, i);
            }
        }

        for (SimpleAttributeDefinition simpleAttributeDefinition : attributes) {
            String attributeValue = reader.getAttributeValue("", simpleAttributeDefinition.getXmlName());
            simpleAttributeDefinition.parseAndSetParameter(attributeValue, modelNode, reader);
        }

        String name = xmlElement.getName();

        if (key != null) {
            name = key;

            if (modelNode.hasDefined(key)) {
                name = modelNode.get(key).asString();
            } else {
                String attributeValue = reader.getAttributeValue("", key);

                if (attributeValue != null) {
                    name = attributeValue;
                }
            }
        }

        modelNode.get(ModelDescriptionConstants.OP_ADDR).set(lastNode.clone().get(OP_ADDR).add(xmlElement.getName(), name));

        addOperations.add(modelNode);

        return modelNode;
    }

    protected void parseElement(final ElementParser parser, ModelElement parentElement, final ModelNode parentNode,
        final XMLExtendedStreamReader reader, final List<ModelNode> addOperations) throws XMLStreamException {
        Set<String> visited = new HashSet<>();

        while (reader.hasNext() && reader.nextTag() != END_DOCUMENT) {
            String tagName = reader.getLocalName();

            if (!reader.isStartElement()) {
                if (reader.isEndElement() && tagName.equals(parentElement.getName())) {
                    break;
                }

                continue;
            }

            if (!tagName.equals(parentElement.getName())) {
                ModelElement element = ModelElement.forName(tagName);

                if (element != null) {
                    parser.parse(reader, element, parentNode, addOperations);
                } else {
                    if (XMLElement.forName(tagName) != null) {
                        if (visited.contains(tagName)) {
                            throw duplicateNamedElement(reader, tagName);
                        }
                    } else {
                        throw unexpectedElement(reader);
                    }
                }
            }

            visited.add(tagName);
        }
    }

    interface ElementParser {

        void parse(XMLExtendedStreamReader reader, ModelElement element, ModelNode parentNode, List<ModelNode> addOperations)
            throws XMLStreamException;
    }


}
