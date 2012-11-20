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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.invalidAttributeValue;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.security.Constants.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
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
 */
public class SecuritySubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext>, ModulesMap {

    private static final SecuritySubsystemParser INSTANCE = new SecuritySubsystemParser();

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
                    parseVaultOptions(reader, vault);
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

        if (node.hasDefined(DEEP_COPY_SUBJECT_MODE) && node.get(DEEP_COPY_SUBJECT_MODE).asBoolean()) {
            writer.writeEmptyElement(Element.SECURITY_MANAGEMENT.getLocalName());
            writeAttribute(writer, Attribute.DEEP_COPY_SUBJECT_MODE, node.get(DEEP_COPY_SUBJECT_MODE));
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
            //ClassicAuthenticationResourceDefinition.LOGIN_MODULES.marshallAsElement(modelNode, writer);
            writeLoginModule(writer, modelNode);
            writer.writeEndElement();
        }
    }

    private void writeAuthorization(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUTHORIZATION.getLocalName());
            //AuthorizationResourceDefinition.POLICY_MODULES.marshallAsElement(modelNode, writer);
            writeLoginModule(writer, modelNode);
            writer.writeEndElement();
        }
    }

    private void writeACL(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.ACL.getLocalName());
            //ACLResourceDefinition.ACL_MODULES.marshallAsElement(modelNode, writer);
            writeLoginModule(writer, modelNode);
            writer.writeEndElement();
        }
    }

    private void writeAudit(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUDIT.getLocalName());
           // AuditResourceDefinition.PROVIDER_MODULES.marshallAsElement(modelNode, writer);
            //TODO
            writer.writeEndElement();
        }
    }

    private void writeIdentityTrust(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.IDENTITY_TRUST.getLocalName());
            //IdentityTrustResourceDefinition.TRUST_MODULES.marshallAsElement(modelNode, writer);
            writeLoginModule(writer, modelNode);
            writer.writeEndElement();
        }
    }

    private void writeMapping(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.MAPPING.getLocalName());

            //MappingResourceDefinition.MAPPING_MODULES.marshallAsElement(modelNode, writer);
            writer.writeEndElement();
        }
    }

    private void writeAuthenticationJaspi(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUTHENTICATION_JASPI.getLocalName());
            ModelNode moduleStack = modelNode.get(LOGIN_MODULE_STACK);
            writeLoginModuleStack(writer, moduleStack);
            //todo jsapi
            //JASPIAuthenticationResourceDefinition.AUTH_MODULES.marshallAsElement(modelNode, writer);
            writer.writeEndElement();
        }
    }

    private void writeLoginModuleStack(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            List<Property> stacks = modelNode.asPropertyList();
            for (Property stack : stacks) {
                writer.writeStartElement(Element.LOGIN_MODULE_STACK.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), stack.getName());
                writeLoginModule(writer, modelNode);
                //LoginModuleStackResourceDefinition.LOGIN_MODULES.marshallAsElement(stack.getValue(), writer);
                writer.writeEndElement();
            }
        }
    }

    private void writeLoginModule(XMLExtendedStreamWriter writer, ModelNode modelNode) {
        //todo
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

    private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value)
            throws XMLStreamException {
        writer.writeAttribute(attr.getLocalName(), value.asString());
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

    private void parseVaultOptions(final XMLExtendedStreamReader reader, final ModelNode vault)
            throws XMLStreamException {

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {


            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case VAULT_OPTION: {
                    parsePropertyElement(reader, vault.get(Constants.VAULT_OPTIONS));
                    break;
                }
            }
        }
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
        ModelNode op = new ModelNode();
        list.add(op);
        op.get(OP).set(ADD);

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
                    if (value == null || value.length() == 0) { throw invalidAttributeValue(reader, i); }

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

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!visited.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case AUTHENTICATION: {
                    if (visited.contains(Element.AUTHENTICATION_JASPI)) {
                        throw SecurityMessages.MESSAGES.xmlStreamExceptionAuth(reader.getLocation());
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
                    if (visited.contains(Element.AUTHENTICATION)) { throw SecurityMessages.MESSAGES.xmlStreamExceptionAuth(reader.getLocation()); }
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
    }

    private void parseAuthentication(List<ModelNode> list, PathAddress parentAddress, XMLExtendedStreamReader reader)
            throws XMLStreamException {
        requireNoAttributes(reader);
        PathAddress address = parentAddress.append(AUTHENTICATION, CLASSIC);
        ModelNode op = Util.createAddOperation(address);
        list.add(op);
        parseLoginModules(reader, address);
    }

    private void parseLoginModules(XMLExtendedStreamReader reader, PathAddress parentAddress) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LOGIN_MODULE: {
                    EnumSet<Attribute> required = EnumSet.of(Attribute.CODE, Attribute.FLAG);
                    EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.TYPE, Attribute.LOGIN_MODULE_STACK_REF);
                    parseCommonModule(reader, parentAddress, LOGIN_MODULE, required, notAllowed);

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
                    parseCommonModule(reader, address, POLICY_MODULE, required, notAllowed);
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
                    parseCommonModule(reader, address, ACL_MODULE, required, notAllowed);
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
                    parseCommonModule(reader, address, PROVIDER_MODULE, required, notAllowed);
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
                    parseCommonModule(reader, address, TRUST_MODULE, required, notAllowed);
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
                    parseCommonModule(reader, address, MAPPING_MODULE, required, notAllowed);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseCommonModule(XMLExtendedStreamReader reader, final PathAddress parentAddress, final String keyName, EnumSet<Attribute> required,
                                   EnumSet<Attribute> notAllowed) throws XMLStreamException {
        ModelNode node = Util.createAddOperation(parentAddress);
        String code = null;
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
                    LoginModulesDefinition.CODE.parseAndSetParameter(value, node, reader);
                    break;
                }
                case FLAG: {
                    LoginModulesDefinition.FLAG.parseAndSetParameter(value, node, reader);
                    break;
                }
                case TYPE: {
                    ModelNode type = MappingModulesAttributeDefinition.parseField(TYPE, value, reader);
                    node.get(TYPE).set(type);
                    break;
                }
                case MODULE: {
                    ModelNode module = MappingModulesAttributeDefinition.parseField(MODULE, value, reader);
                    node.get(MODULE).set(module);
                    break;
                }
                case LOGIN_MODULE_STACK_REF: {
                    ModelNode ref = JASPIAuthenticationModulesAttributeDefinition.parseField(LOGIN_MODULE_STACK_REF, value, reader);
                    node.get(LOGIN_MODULE_STACK_REF).set(ref);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        node.get(OP_ADDR).set(parentAddress.append(keyName, code).toModelNode());
        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }

        parseProperties(Element.MODULE_OPTION.getLocalName(), reader, node.get(MODULE_OPTIONS));
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
                    //ModelNode node = op.get(AUTH_MODULES);
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
        EnumSet<Attribute> notAllowed = null;
        // in version 1.2 of the schema the optional flag attribute has been included.
        if (schemaVer == Namespace.SECURITY_1_2) {
            notAllowed = EnumSet.of(Attribute.TYPE);
        }
        // in earlier versions of the schema, the flag attribute was missing (not allowed).
        else {
            notAllowed = EnumSet.of(Attribute.TYPE, Attribute.FLAG);
        }
        parseCommonModule(reader, parentAddress, AUTH_MODULE, required, notAllowed);
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
        parseLoginModules(reader, address);
    }

    private void parseProperties(String childElementName, XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (childElementName.equals(element.getLocalName())) {
                parsePropertyElement(reader, node);
            } else {
                throw unexpectedElement(reader);
            }
        }
    }

    private void parsePropertyElement(XMLExtendedStreamReader reader, ModelNode properties) throws XMLStreamException {
        String name = null;
        ModelNode val = null;
        EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.VALUE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: {
                    if (value == null) { throw invalidAttributeValue(reader, i); }
                    name = value.trim();
                    break;
                }
                case VALUE: {
                    String propValue = value != null ? value.trim() : value;
                    if (propValue != null) { val = new ModelNode().set(ParseUtils.parsePossibleExpression(propValue)); } else {
                        val = new ModelNode();
                    }
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }

        properties.add(name, val);
        requireNoContent(reader);
    }

    private void parseJSSE(List<ModelNode> list, PathAddress parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
        ModelNode op = appendAddOperation(list, parentAddress, JSSE, CLASSIC);
        EnumSet<Attribute> visited = EnumSet.noneOf(Attribute.class);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));

            switch (attribute) {
                case KEYSTORE_PASSWORD: {
                    ModelNode password = KeyStoreAttributeDefinition.parseField(PASSWORD, value, reader);
                    op.get(KEYSTORE, PASSWORD).set(password);
                    visited.add(attribute);
                    break;
                }
                case KEYSTORE_TYPE: {
                    ModelNode type = KeyStoreAttributeDefinition.parseField(TYPE, value, reader);
                    op.get(KEYSTORE, TYPE).set(type);
                    break;
                }
                case KEYSTORE_URL: {
                    ModelNode url = KeyStoreAttributeDefinition.parseField(URL, value, reader);
                    op.get(KEYSTORE, URL).set(url);
                    break;
                }
                case KEYSTORE_PROVIDER: {
                    ModelNode provider = KeyStoreAttributeDefinition.parseField(PROVIDER, value, reader);
                    op.get(KEYSTORE, PROVIDER).set(provider);
                    break;
                }
                case KEYSTORE_PROVIDER_ARGUMENT: {
                    ModelNode argument = KeyStoreAttributeDefinition.parseField(PROVIDER_ARGUMENT, value, reader);
                    op.get(KEYSTORE, PROVIDER_ARGUMENT).set(argument);
                    break;
                }
                case KEY_MANAGER_FACTORY_PROVIDER: {
                    ModelNode provider = KeyManagerAttributeDefinition.parseField(PROVIDER, value, reader);
                    op.get(KEY_MANAGER, PROVIDER).set(provider);
                    break;
                }
                case KEY_MANAGER_FACTORY_ALGORITHM: {
                    ModelNode provider = KeyManagerAttributeDefinition.parseField(ALGORITHM, value, reader);
                    op.get(KEY_MANAGER, ALGORITHM).set(provider);
                    break;
                }
                case TRUSTSTORE_PASSWORD: {
                    ModelNode password = KeyStoreAttributeDefinition.parseField(PASSWORD, value, reader);
                    op.get(TRUSTSTORE, PASSWORD).set(password);
                    visited.add(attribute);
                    break;
                }
                case TRUSTSTORE_TYPE: {
                    ModelNode type = KeyStoreAttributeDefinition.parseField(TYPE, value, reader);
                    op.get(TRUSTSTORE, TYPE).set(type);
                    break;
                }
                case TRUSTSTORE_URL: {
                    ModelNode url = KeyStoreAttributeDefinition.parseField(URL, value, reader);
                    op.get(TRUSTSTORE, URL).set(url);
                    break;
                }
                case TRUSTSTORE_PROVIDER: {
                    ModelNode provider = KeyStoreAttributeDefinition.parseField(PROVIDER, value, reader);
                    op.get(TRUSTSTORE, PROVIDER).set(provider);
                    break;
                }
                case TRUSTSTORE_PROVIDER_ARGUMENT: {
                    ModelNode argument = KeyStoreAttributeDefinition.parseField(PROVIDER_ARGUMENT, value, reader);
                    op.get(TRUSTSTORE, PROVIDER_ARGUMENT).set(argument);
                    break;
                }
                case TRUST_MANAGER_FACTORY_PROVIDER: {
                    ModelNode provider = KeyManagerAttributeDefinition.parseField(PROVIDER, value, reader);
                    op.get(TRUST_MANAGER, PROVIDER).set(provider);
                    break;
                }
                case TRUST_MANAGER_FACTORY_ALGORITHM: {
                    ModelNode provider = KeyManagerAttributeDefinition.parseField(ALGORITHM, value, reader);
                    op.get(TRUST_MANAGER, ALGORITHM).set(provider);
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

        if (visited.size() == 0) {
            throw SecurityMessages.MESSAGES.xmlStreamExceptionMissingAttribute(Attribute.KEYSTORE_PASSWORD.getLocalName(),
                    Attribute.TRUSTSTORE_PASSWORD.getLocalName(), reader.getLocation());
        }

        parseProperties(Element.PROPERTY.getLocalName(), reader, op.get(ADDITIONAL_PROPERTIES));
    }
}
