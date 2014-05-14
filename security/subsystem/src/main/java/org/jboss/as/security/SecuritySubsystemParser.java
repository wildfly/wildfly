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

package org.jboss.as.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CODE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.invalidAttributeValue;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.security.Constants.ACL;
import static org.jboss.as.security.Constants.ACL_MODULE;
import static org.jboss.as.security.Constants.AUDIT;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.AUTHORIZATION;
import static org.jboss.as.security.Constants.AUTH_MODULE;
import static org.jboss.as.security.Constants.CACHE_TYPE;
import static org.jboss.as.security.Constants.CLASSIC;
import static org.jboss.as.security.Constants.IDENTITY_TRUST;
import static org.jboss.as.security.Constants.JASPI;
import static org.jboss.as.security.Constants.JSSE;
import static org.jboss.as.security.Constants.KEYSTORE;
import static org.jboss.as.security.Constants.KEY_MANAGER;
import static org.jboss.as.security.Constants.LOGIN_MODULE;
import static org.jboss.as.security.Constants.LOGIN_MODULE_STACK;
import static org.jboss.as.security.Constants.MAPPING;
import static org.jboss.as.security.Constants.MAPPING_MODULE;
import static org.jboss.as.security.Constants.NAME;
import static org.jboss.as.security.Constants.POLICY_MODULE;
import static org.jboss.as.security.Constants.PROVIDER_MODULE;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.jboss.as.security.Constants.TRUSTSTORE;
import static org.jboss.as.security.Constants.TRUST_MANAGER;
import static org.jboss.as.security.Constants.TRUST_MODULE;
import static org.jboss.as.security.Constants.VAULT;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The root element parser for the Security subsystem.
 *
 * @author Marcus Moyses
 * @author Darran Lofthouse
 * @author Brian Stansberry
 * @author Jason T. Greene
 * @author Anil Saldhana
 * @author Tomaz Cerar
 */
public class SecuritySubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext>, ModulesMap {

    private static final SecuritySubsystemParser INSTANCE = new SecuritySubsystemParser();
    private Map<String, Integer> moduleNames;

    public static SecuritySubsystemParser getInstance() {
        return INSTANCE;
    }

    private SecuritySubsystemParser() {
        //
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        PathAddress address = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, SecurityExtension.SUBSYSTEM_NAME));
        final ModelNode subsystem = Util.createAddOperation(address);
        requireNoAttributes(reader);

        List<ModelNode> securityDomainsUpdates = null;
        ModelNode vault = null;
        final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            Namespace schemaVer = Namespace.forUri(reader.getNamespaceURI());
            final Element element = Element.forName(reader.getLocalName());
            if (!visited.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case SECURITY_MANAGEMENT: {
                    parseSecurityManagement(reader, subsystem);
                    break;
                }
                case SECURITY_DOMAINS: {
                    securityDomainsUpdates = parseSecurityDomains(reader, address);
                    break;
                }
                case SECURITY_PROPERTIES:
                    reader.discardRemainder();
                    break;
                case VAULT: {
                    if (schemaVer == Namespace.SECURITY_1_0) { throw unexpectedElement(reader); }
                    final int count = reader.getAttributeCount();
                    vault = createAddOperation(address, VAULT, CLASSIC);
                    if (count > 1) {
                        throw unexpectedAttribute(reader, count);
                    }

                    for (int i = 0; i < count; i++) {
                        requireNoNamespaceAttribute(reader, i);
                        final String value = reader.getAttributeValue(i);
                        final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                            case CODE: {
                                vault.get(CODE).set(value);
                                break;
                            }
                            default:
                                throw unexpectedAttribute(reader, i);
                        }
                    }
                    parseProperties(Element.VAULT_OPTION.getLocalName(), reader, vault, VaultResourceDefinition.OPTIONS);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        list.add(subsystem);

        if (vault != null) {
            list.add(vault);
        }

        if (securityDomainsUpdates != null) {
            list.addAll(securityDomainsUpdates);
        }
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = context.getModelNode();
        if (SecuritySubsystemRootResourceDefinition.DEEP_COPY_SUBJECT_MODE.isMarshallable(node)) {
            writer.writeEmptyElement(Element.SECURITY_MANAGEMENT.getLocalName());
            SecuritySubsystemRootResourceDefinition.DEEP_COPY_SUBJECT_MODE.marshallAsAttribute(node, writer);
        }

        if (node.hasDefined(SECURITY_DOMAIN) && node.get(SECURITY_DOMAIN).asInt() > 0) {
            writer.writeStartElement(Element.SECURITY_DOMAINS.getLocalName());
            for (Property policy : node.get(SECURITY_DOMAIN).asPropertyList()) {
                writer.writeStartElement(Element.SECURITY_DOMAIN.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), policy.getName());
                ModelNode policyDetails = policy.getValue();
                SecurityDomainResourceDefinition.CACHE_TYPE.marshallAsAttribute(policyDetails, writer);
                writeSecurityDomainContent(writer, policyDetails);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        if (node.hasDefined(Constants.VAULT)) {
            ModelNode vault = node.get(Constants.VAULT, Constants.CLASSIC);
            writer.writeStartElement(Element.VAULT.getLocalName());
            VaultResourceDefinition.CODE.marshallAsAttribute(vault, writer);

            if (vault.hasDefined(Constants.VAULT_OPTIONS)) {
                ModelNode properties = vault.get(Constants.VAULT_OPTIONS);
                for (Property prop : properties.asPropertyList()) {
                    writer.writeEmptyElement(Element.VAULT_OPTION.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), prop.getName());
                    writer.writeAttribute(Attribute.VALUE.getLocalName(), prop.getValue().asString());
                }
            }
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    private void writeSecurityDomainContent(XMLExtendedStreamWriter writer, ModelNode policyDetails) throws XMLStreamException {
        Set<String> keys = policyDetails.keys();
        keys.remove(NAME);
        keys.remove(CACHE_TYPE);

        for (String key : keys) {
            Element element = Element.forName(key);
            switch (element) {
                case AUTHENTICATION: {
                    ModelNode kind = policyDetails.get(AUTHENTICATION);
                    for (Property prop : kind.asPropertyList()) {
                        if (CLASSIC.equals(prop.getName())) {
                            writeAuthentication(writer, prop.getValue());
                        } else if (JASPI.equals(prop.getName())) {
                            writeAuthenticationJaspi(writer, prop.getValue());
                        }
                    }

                    break;
                }
                case AUTHORIZATION: {
                    writeAuthorization(writer, policyDetails.get(AUTHORIZATION, CLASSIC));
                    break;
                }
                case ACL: {
                    writeACL(writer, policyDetails.get(ACL, CLASSIC));
                    break;
                }
                case AUDIT: {
                    writeAudit(writer, policyDetails.get(AUDIT, CLASSIC));
                    break;
                }
                case IDENTITY_TRUST: {
                    writeIdentityTrust(writer, policyDetails.get(IDENTITY_TRUST, CLASSIC));
                    break;
                }
                case MAPPING: {
                    writeMapping(writer, policyDetails.get(MAPPING, CLASSIC));
                    break;
                }
                case JSSE: {
                    writeJSSE(writer, policyDetails.get(JSSE, CLASSIC));
                    break;
                }
            }
        }
    }

    private void writeAuthentication(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUTHENTICATION.getLocalName());
            writeLoginModule(writer, modelNode, Constants.LOGIN_MODULE);
            writer.writeEndElement();
        }
    }

    private void writeAuthorization(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUTHORIZATION.getLocalName());
            writeLoginModule(writer, modelNode, Constants.POLICY_MODULE, Element.POLICY_MODULE.getLocalName());
            writer.writeEndElement();
        }
    }

    private void writeACL(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.ACL.getLocalName());
            writeLoginModule(writer, modelNode, Constants.ACL_MODULE, Element.ACL_MODULE.getLocalName());
            writer.writeEndElement();
        }
    }

    private void writeAudit(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUDIT.getLocalName());
            writeLoginModule(writer, modelNode, Constants.PROVIDER_MODULE, Element.PROVIDER_MODULE.getLocalName());
            writer.writeEndElement();
        }
    }

    private void writeIdentityTrust(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.IDENTITY_TRUST.getLocalName());
            writeLoginModule(writer, modelNode, Constants.TRUST_MODULE, Element.TRUST_MODULE.getLocalName());
            writer.writeEndElement();
        }
    }

    private void writeMapping(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.MAPPING.getLocalName());
            writeLoginModule(writer, modelNode, Constants.MAPPING_MODULE, Constants.MAPPING_MODULE);
            writer.writeEndElement();
        }
    }


    private void writeAuthenticationJaspi(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUTHENTICATION_JASPI.getLocalName());
            ModelNode moduleStack = modelNode.get(LOGIN_MODULE_STACK);
            writeLoginModuleStack(writer, moduleStack);
            writeLoginModule(writer, modelNode, Constants.AUTH_MODULE, Element.AUTH_MODULE.getLocalName());
            writer.writeEndElement();
        }
    }

    private void writeLoginModuleStack(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            List<Property> stacks = modelNode.asPropertyList();
            for (Property stack : stacks) {
                writer.writeStartElement(Element.LOGIN_MODULE_STACK.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), stack.getName());
                writeLoginModule(writer, stack.getValue(), Constants.LOGIN_MODULE);
                writer.writeEndElement();
            }
        }
    }

    private void writeLoginModule(XMLExtendedStreamWriter writer, ModelNode modelNode, String key) throws XMLStreamException {
        writeLoginModule(writer, modelNode, key, Element.LOGIN_MODULE.getLocalName());
    }

    private void writeLoginModule(XMLExtendedStreamWriter writer, ModelNode modelNode, String key, final String elementName) throws XMLStreamException {
        if (!modelNode.hasDefined(key)){
            return;
        }
        final ModelNode modules = modelNode.get(key);
        for (Property moduleProp : modules.asPropertyList()) {
            ModelNode module = moduleProp.getValue();
            writer.writeStartElement(elementName);
            if (!moduleProp.getName().equals(module.get(CODE).asString())) {
                writer.writeAttribute(NAME, moduleProp.getName());
            }
            LoginModuleResourceDefinition.CODE.marshallAsAttribute(module, writer);
            LoginModuleResourceDefinition.FLAG.marshallAsAttribute(module, writer);
            MappingModuleDefinition.TYPE.marshallAsAttribute(module, writer);
            JASPIMappingModuleDefinition.LOGIN_MODULE_STACK_REF.marshallAsAttribute(module, writer);
            LoginModuleResourceDefinition.MODULE.marshallAsAttribute(module, false, writer);
            if (module.hasDefined(Constants.MODULE_OPTIONS)) {
                for (ModelNode option : module.get(Constants.MODULE_OPTIONS).asList()) {
                    writer.writeEmptyElement(Element.MODULE_OPTION.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), option.asProperty().getName());
                    writer.writeAttribute(Attribute.VALUE.getLocalName(), option.asProperty().getValue().asString());
                }
            }
            writer.writeEndElement();
        }
    }


    private void writeJSSE(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.JSSE.getLocalName());
            JSSEResourceDefinition.KEYSTORE.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.TRUSTSTORE.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.KEYMANAGER.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.TRUSTMANAGER.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.CIPHER_SUITES.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.SERVER_ALIAS.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.SERVICE_AUTH_TOKEN.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.CLIENT_ALIAS.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.CLIENT_AUTH.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.PROTOCOLS.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.ADDITIONAL_PROPERTIES.marshallAsElement(modelNode, writer);
            writer.writeEndElement();
        }
    }

    private void parseSecurityManagement(final XMLExtendedStreamReader reader, final ModelNode operation)
            throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEEP_COPY_SUBJECT_MODE: {
                    SecuritySubsystemRootResourceDefinition.DEEP_COPY_SUBJECT_MODE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
    }

    private List<ModelNode> parseSecurityDomains(final XMLExtendedStreamReader reader, final PathAddress parentAddress)
            throws XMLStreamException {
        requireNoAttributes(reader);

        List<ModelNode> list = new ArrayList<ModelNode>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SECURITY_DOMAIN: {
                    parseSecurityDomain(list, reader, parentAddress);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        return list;
    }

    private void parseSecurityDomain(List<ModelNode> list, XMLExtendedStreamReader reader, PathAddress parentAddress) throws XMLStreamException {
        ModelNode op = Util.createAddOperation();
        list.add(op);
        PathElement secDomainPath = null;
        EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: {
                    if (value == null || value.length() == 0) {
                        throw invalidAttributeValue(reader, i);
                    }
                    secDomainPath = PathElement.pathElement(SECURITY_DOMAIN, value);
                    break;
                }
                case CACHE_TYPE: {
                    SecurityDomainResourceDefinition.CACHE_TYPE.parseAndSetParameter(value, op, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }
        final PathAddress address = parentAddress.append(secDomainPath);
        op.get(OP_ADDR).set(address.toModelNode());
        final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
        moduleNames = new HashMap<String, Integer>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!visited.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case AUTHENTICATION: {
                    if (visited.contains(Element.AUTHENTICATION_JASPI)) {
                        throw SecurityLogger.ROOT_LOGGER.xmlStreamExceptionAuth(reader.getLocation());
                    }
                    parseAuthentication(list, address, reader);
                    break;
                }
                case AUTHORIZATION: {
                    parseAuthorization(list, address, reader);
                    break;
                }
                case ACL: {
                    parseACL(list, address, reader);
                    break;
                }
                case AUDIT: {
                    parseAudit(list, address, reader);
                    break;
                }
                case IDENTITY_TRUST: {
                    parseIdentityTrust(list, address, reader);
                    break;
                }
                case MAPPING: {
                    parseMapping(list, address, reader);
                    break;
                }
                case AUTHENTICATION_JASPI: {
                    if (visited.contains(Element.AUTHENTICATION)) { throw SecurityLogger.ROOT_LOGGER.xmlStreamExceptionAuth(reader.getLocation()); }
                    parseAuthenticationJaspi(list, address, reader);
                    break;
                }
                case JSSE: {
                    parseJSSE(list, address, reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        moduleNames.clear();
    }

    private void parseAuthentication(List<ModelNode> list, PathAddress parentAddress, XMLExtendedStreamReader reader)
            throws XMLStreamException {
        requireNoAttributes(reader);
        PathAddress address = parentAddress.append(AUTHENTICATION, CLASSIC);
        ModelNode op = Util.createAddOperation(address);
        list.add(op);
        parseLoginModules(reader, address, list);
    }

    private void parseLoginModules(XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException {

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LOGIN_MODULE: {
                    EnumSet<Attribute> required = EnumSet.of(Attribute.CODE, Attribute.FLAG);
                    EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.TYPE, Attribute.LOGIN_MODULE_STACK_REF);
                    parseCommonModule(reader, parentAddress, LOGIN_MODULE, required, notAllowed, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private ModelNode appendAddOperation(List<ModelNode> list, PathAddress parentAddress, String name, String value) {
        ModelNode op = createAddOperation(parentAddress, name, value);
        list.add(op);
        return op;
    }

    private ModelNode createAddOperation(PathAddress parentAddress, String name, String value) {
        return Util.createAddOperation(parentAddress.append(name, value));
    }

    private void parseAuthorization(List<ModelNode> list, PathAddress parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
        requireNoAttributes(reader);
        PathAddress address = parentAddress.append(AUTHORIZATION, CLASSIC);
        ModelNode op = Util.createAddOperation(address);
        list.add(op);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case POLICY_MODULE: {
                    EnumSet<Attribute> required = EnumSet.of(Attribute.CODE, Attribute.FLAG);
                    EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.TYPE, Attribute.LOGIN_MODULE_STACK_REF);
                    parseCommonModule(reader, address, POLICY_MODULE, required, notAllowed, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseACL(List<ModelNode> list, PathAddress parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
        requireNoAttributes(reader);
        PathAddress address = parentAddress.append(ACL, CLASSIC);
        ModelNode op = Util.createAddOperation(address);
        list.add(op);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ACL_MODULE: {
                    EnumSet<Attribute> required = EnumSet.of(Attribute.CODE, Attribute.FLAG);
                    EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.TYPE, Attribute.LOGIN_MODULE_STACK_REF);
                    parseCommonModule(reader, address, ACL_MODULE, required, notAllowed, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseAudit(List<ModelNode> list, PathAddress parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
        requireNoAttributes(reader);
        PathAddress address = parentAddress.append(AUDIT, CLASSIC);
        ModelNode op = Util.createAddOperation(address);
        list.add(op);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROVIDER_MODULE: {
                    EnumSet<Attribute> required = EnumSet.of(Attribute.CODE);
                    EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.TYPE, Attribute.FLAG, Attribute.LOGIN_MODULE_STACK_REF);
                    parseCommonModule(reader, address, PROVIDER_MODULE, required, notAllowed, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

    }

    private void parseIdentityTrust(List<ModelNode> list, PathAddress parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
        requireNoAttributes(reader);
        PathAddress address = parentAddress.append(IDENTITY_TRUST, CLASSIC);
        ModelNode op = Util.createAddOperation(address);
        list.add(op);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case TRUST_MODULE: {
                    EnumSet<Attribute> required = EnumSet.of(Attribute.CODE, Attribute.FLAG);
                    EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.TYPE, Attribute.LOGIN_MODULE_STACK_REF);
                    parseCommonModule(reader, address, TRUST_MODULE, required, notAllowed, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseMapping(List<ModelNode> list, PathAddress parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
        requireNoAttributes(reader);
        PathAddress address = parentAddress.append(MAPPING, CLASSIC);
        ModelNode op = Util.createAddOperation(address);
        list.add(op);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case MAPPING_MODULE: {
                    EnumSet<Attribute> required = EnumSet.of(Attribute.CODE);
                    EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.FLAG, Attribute.LOGIN_MODULE_STACK_REF);
                    parseCommonModule(reader, address, MAPPING_MODULE, required, notAllowed, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseCommonModule(XMLExtendedStreamReader reader, final PathAddress parentAddress, final String keyName, EnumSet<Attribute> required,
                                   EnumSet<Attribute> notAllowed, List<ModelNode> list) throws XMLStreamException {
        ModelNode node = Util.createAddOperation(parentAddress);
        String code = null;
        String name = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            if (notAllowed.contains(attribute)) { throw unexpectedAttribute(reader, i); }
            required.remove(attribute);
            switch (attribute) {
                case CODE: {
                    code = value;
                    LoginModuleResourceDefinition.CODE.parseAndSetParameter(value, node, reader);
                    break;
                }
                case FLAG: {
                    LoginModuleResourceDefinition.FLAG.parseAndSetParameter(value, node, reader);
                    break;
                }
                case TYPE: {
                    MappingModuleDefinition.TYPE.parseAndSetParameter(value, node, reader);
                    break;
                }
                case MODULE: {
                    LoginModuleResourceDefinition.MODULE.parseAndSetParameter(value, node, reader);
                    break;
                }
                case LOGIN_MODULE_STACK_REF: {
                    JASPIMappingModuleDefinition.LOGIN_MODULE_STACK_REF.parseAndSetParameter(value, node, reader);
                    break;
                }
                case NAME: {
                    name = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            name = code;
        }
        String key = keyName + "-" + name;
        if (moduleNames.put(key, 1) != null) { //is case user configures duplicate login-module with same code, we generate name for him.
            for (int i = 2; ; i++) {
                name = code + "-" + i;
                key = keyName + "-" + name;
                if (!moduleNames.containsKey(key)) {
                    moduleNames.put(key, i);
                    break;
                }
            }
        }

        node.get(OP_ADDR).set(parentAddress.append(keyName, name).toModelNode());
        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }
        parseProperties(Element.MODULE_OPTION.getLocalName(), reader, node, LoginModuleResourceDefinition.MODULE_OPTIONS);
        list.add(node);
    }

    private void parseAuthenticationJaspi(List<ModelNode> list, PathAddress parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
        requireNoAttributes(reader);
        PathAddress address = parentAddress.append(AUTHENTICATION, JASPI);
        ModelNode op = Util.createAddOperation(address);
        list.add(op);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LOGIN_MODULE_STACK: {
                    parseLoginModuleStack(list, address, reader);
                    break;
                }
                case AUTH_MODULE: {
                    parseAuthModule(list, reader, address);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseAuthModule(List<ModelNode> list, XMLExtendedStreamReader reader, PathAddress parentAddress) throws XMLStreamException {
        Namespace schemaVer = Namespace.forUri(reader.getNamespaceURI());
        EnumSet<Attribute> required = EnumSet.of(Attribute.CODE);
        EnumSet<Attribute> notAllowed;
        // in version 1.2 of the schema the optional flag attribute has been included.
        if (schemaVer == Namespace.SECURITY_1_2) {
            notAllowed = EnumSet.of(Attribute.TYPE);
        }
        // in earlier versions of the schema, the flag attribute was missing (not allowed).
        else {
            notAllowed = EnumSet.of(Attribute.TYPE, Attribute.FLAG);
        }
        parseCommonModule(reader, parentAddress, AUTH_MODULE, required, notAllowed, list);
    }

    private void parseLoginModuleStack(List<ModelNode> list, PathAddress parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
        EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        String name = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: {
                    if (value == null) { throw invalidAttributeValue(reader, i); }
                    name = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }

        PathAddress address = parentAddress.append(LOGIN_MODULE_STACK, name);
        ModelNode op = Util.createAddOperation(address);
        list.add(op);
        parseLoginModules(reader, address, list);
    }

    private void parseProperties(String childElementName, XMLExtendedStreamReader reader, ModelNode node, PropertiesAttributeDefinition attribute) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (childElementName.equals(element.getLocalName())) {
                final String[] array = requireAttributes(reader, org.jboss.as.controller.parsing.Attribute.NAME.getLocalName(), org.jboss.as.controller.parsing.Attribute.VALUE.getLocalName());
                attribute.parseAndAddParameterElement(array[0], array[1], node, reader);
            } else {
                throw unexpectedElement(reader);
            }
            requireNoContent(reader);
        }
    }

    private void parseJSSE(List<ModelNode> list, PathAddress parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
        ModelNode op = appendAddOperation(list, parentAddress, JSSE, CLASSIC);
        EnumSet<Attribute> visited = EnumSet.noneOf(Attribute.class);
        EnumSet<Attribute> required = EnumSet.noneOf(Attribute.class);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));

            switch (attribute) {
                case KEYSTORE_PASSWORD: {
                    ComplexAttributes.PASSWORD.parseAndSetParameter(value, op.get(KEYSTORE), reader);
                    visited.add(attribute);
                    break;
                }
                case KEYSTORE_TYPE: {
                    ComplexAttributes.TYPE.parseAndSetParameter(value, op.get(KEYSTORE), reader);
                    required.add(Attribute.KEYSTORE_PASSWORD);
                    break;
                }
                case KEYSTORE_URL: {
                    ComplexAttributes.URL.parseAndSetParameter(value, op.get(KEYSTORE), reader);
                    required.add(Attribute.KEYSTORE_PASSWORD);
                    break;
                }
                case KEYSTORE_PROVIDER: {
                    ComplexAttributes.PROVIDER.parseAndSetParameter(value, op.get(KEYSTORE), reader);
                    required.add(Attribute.KEYSTORE_PASSWORD);
                    break;
                }
                case KEYSTORE_PROVIDER_ARGUMENT: {
                    ComplexAttributes.PROVIDER_ARGUMENT.parseAndSetParameter(value, op.get(KEYSTORE), reader);
                    required.add(Attribute.KEYSTORE_PASSWORD);
                    break;
                }
                case KEY_MANAGER_FACTORY_PROVIDER: {
                    ComplexAttributes.PROVIDER.parseAndSetParameter(value, op.get(KEY_MANAGER), reader);
                    break;
                }
                case KEY_MANAGER_FACTORY_ALGORITHM: {
                    ComplexAttributes.ALGORITHM.parseAndSetParameter(value, op.get(KEY_MANAGER), reader);
                    break;
                }
                case TRUSTSTORE_PASSWORD: {
                    ComplexAttributes.PASSWORD.parseAndSetParameter(value, op.get(TRUSTSTORE), reader);
                    visited.add(attribute);
                    break;
                }
                case TRUSTSTORE_TYPE: {
                    ComplexAttributes.TYPE.parseAndSetParameter(value, op.get(TRUSTSTORE), reader);
                    required.add(Attribute.TRUSTSTORE_PASSWORD);
                    break;
                }
                case TRUSTSTORE_URL: {
                    ComplexAttributes.URL.parseAndSetParameter(value, op.get(TRUSTSTORE), reader);
                    required.add(Attribute.TRUSTSTORE_PASSWORD);
                    break;
                }
                case TRUSTSTORE_PROVIDER: {
                    ComplexAttributes.PROVIDER.parseAndSetParameter(value, op.get(TRUSTSTORE), reader);
                    required.add(Attribute.TRUSTSTORE_PASSWORD);
                    break;
                }
                case TRUSTSTORE_PROVIDER_ARGUMENT: {
                    ComplexAttributes.PROVIDER_ARGUMENT.parseAndSetParameter(value, op.get(TRUSTSTORE), reader);
                    required.add(Attribute.TRUSTSTORE_PASSWORD);
                    break;
                }
                case TRUST_MANAGER_FACTORY_PROVIDER: {
                    ComplexAttributes.PROVIDER.parseAndSetParameter(value, op.get(TRUST_MANAGER), reader);
                    break;
                }
                case TRUST_MANAGER_FACTORY_ALGORITHM: {
                    ComplexAttributes.ALGORITHM.parseAndSetParameter(value, op.get(TRUST_MANAGER), reader);
                    break;
                }
                case CLIENT_ALIAS: {
                    JSSEResourceDefinition.CLIENT_ALIAS.parseAndSetParameter(value, op, reader);
                    break;
                }
                case SERVER_ALIAS: {
                    JSSEResourceDefinition.SERVER_ALIAS.parseAndSetParameter(value, op, reader);
                    break;
                }
                case CLIENT_AUTH: {
                    JSSEResourceDefinition.CLIENT_AUTH.parseAndSetParameter(value, op, reader);
                    break;
                }
                case SERVICE_AUTH_TOKEN: {
                    JSSEResourceDefinition.SERVICE_AUTH_TOKEN.parseAndSetParameter(value, op, reader);
                    break;
                }
                case CIPHER_SUITES: {
                    JSSEResourceDefinition.CIPHER_SUITES.parseAndSetParameter(value, op, reader);
                    break;
                }
                case PROTOCOLS: {
                    JSSEResourceDefinition.PROTOCOLS.parseAndSetParameter(value, op, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (!visited.containsAll(required)) {
            throw SecurityLogger.ROOT_LOGGER.xmlStreamExceptionMissingAttribute(Attribute.KEYSTORE_PASSWORD.getLocalName(),
                    Attribute.TRUSTSTORE_PASSWORD.getLocalName(), reader.getLocation());
        }

        parseProperties(Element.PROPERTY.getLocalName(), reader, op, JSSEResourceDefinition.ADDITIONAL_PROPERTIES);
    }
}
