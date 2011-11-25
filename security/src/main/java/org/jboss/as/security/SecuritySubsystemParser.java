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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

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
        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        ModelNode address = subsystem.get(OP_ADDR);
        address.add(SUBSYSTEM, SecurityExtension.SUBSYSTEM_NAME);

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
                    if(schemaVer == Namespace.SECURITY_1_0)
                        throw unexpectedElement(reader);
                    final int count = reader.getAttributeCount();
                    vault = createAddOperation(address, VAULT, CLASSIC);
                    if(count > 1) {
                        throw unexpectedAttribute(reader, count);
                    }

                    for (int i = 0; i < count; i++) {
                        requireNoNamespaceAttribute(reader, i);
                        final String value = reader.getAttributeValue(i);
                        final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                            case CODE: {
                                String code = value;
                                vault.get(CODE).set(code);
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

        if(node.hasDefined(Constants.VAULT)){
            ModelNode vault = node.get(Constants.VAULT, Constants.CLASSIC);
            writer.writeStartElement(Element.VAULT.getLocalName());
            VaultResourceDefinition.CODE.marshallAsAttribute(vault, writer);

            if (vault.hasDefined(Constants.OPTIONS)) {
                ModelNode properties = vault.get(Constants.OPTIONS);
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
        Set<String> keys = new HashSet<String>(policyDetails.keys());
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
            ClassicAuthenticationResourceDefinition.LOGIN_MODULES.marshallAsElement(modelNode, writer);
            writer.writeEndElement();
        }
    }

    private void writeAuthorization(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUTHORIZATION.getLocalName());
            AuthorizationResourceDefinition.POLICY_MODULES.marshallAsElement(modelNode, writer);
            writer.writeEndElement();
        }
    }

    private void writeACL(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.ACL.getLocalName());
            ACLResourceDefinition.ACL_MODULES.marshallAsElement(modelNode, writer);
            writer.writeEndElement();
        }
    }

    private void writeAudit(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUDIT.getLocalName());
            AuditResourceDefinition.PROVIDER_MODULES.marshallAsElement(modelNode, writer);
            writer.writeEndElement();
        }
    }

    private void writeIdentityTrust(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.IDENTITY_TRUST.getLocalName());
            IdentityTrustResourceDefinition.TRUST_MODULES.marshallAsElement(modelNode, writer);
            writer.writeEndElement();
        }
    }

    private void writeMapping(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.MAPPING.getLocalName());
            MappingResourceDefinition.MAPPING_MODULES.marshallAsElement(modelNode, writer);
            writer.writeEndElement();
        }
    }

    private void writeAuthenticationJaspi(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUTHENTICATION_JASPI.getLocalName());
            ModelNode moduleStack = modelNode.get(LOGIN_MODULE_STACK);
            writeLoginModuleStack(writer, moduleStack);
            JASPIAuthenticationResourceDefinition.AUTH_MODULES.marshallAsElement(modelNode, writer);
            writer.writeEndElement();
        }
    }

    private void writeLoginModuleStack(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            List<Property> stacks = modelNode.asPropertyList();
            for (Property stack : stacks) {
                writer.writeStartElement(Element.LOGIN_MODULE_STACK.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), stack.getName());
                LoginModuleStackResourceDefinition.LOGIN_MODULES.marshallAsElement(stack.getValue(), writer);
                writer.writeEndElement();
            }
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
                    SecuritySubsystemRootResourceDefinition.DEEP_COPY_SUBJECT_MODE.parseAndSetParameter(value, operation, reader.getLocation());
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
                    parsePropertyElement(reader, vault.get(Constants.OPTIONS));
                    break;
                }
            }
        }
    }

    private List<ModelNode> parseSecurityDomains(final XMLExtendedStreamReader reader, final ModelNode parentAddress)
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

    private void parseSecurityDomain(List<ModelNode> list, XMLExtendedStreamReader reader, ModelNode parentAddress) throws XMLStreamException {
        ModelNode op = new ModelNode();
        list.add(op);
        op.get(OP).set(ADD);
        ModelNode address = op.get(OP_ADDR);

        EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: {
                    if (value == null || value.length() == 0)
                        throw invalidAttributeValue(reader, i);

                    address.set(parentAddress).add(SECURITY_DOMAIN, value);
                    break;
                }
                case CACHE_TYPE: {
                    SecurityDomainResourceDefinition.CACHE_TYPE.parseAndSetParameter(value, op, reader.getLocation());
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }

        final EnumSet<Element> visited = EnumSet.noneOf(Element.class);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!visited.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case AUTHENTICATION: {
                    if (visited.contains(Element.AUTHENTICATION_JASPI))
                        throw new XMLStreamException(
                                "A security domain can have either an <authentication> or <authentication-jaspi> element, not both",
                                reader.getLocation());
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
                    if (visited.contains(Element.AUTHENTICATION))
                        throw new XMLStreamException(
                                "A security domain can have either an <authentication> or <authentication-jaspi> element, not both",
                                reader.getLocation());
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

    private void parseAuthentication(List<ModelNode> list, ModelNode parentAddress, XMLExtendedStreamReader reader)
            throws XMLStreamException {
        requireNoAttributes(reader);

        ModelNode op = appendAddOperation(list, parentAddress, AUTHENTICATION, CLASSIC);
        parseLoginModules(reader, op.get(LOGIN_MODULES));
    }

    private void parseLoginModules(XMLExtendedStreamReader reader, ModelNode op) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LOGIN_MODULE: {
                    EnumSet<Attribute> required = EnumSet.of(Attribute.CODE, Attribute.FLAG);
                    EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.TYPE);
                    parseCommonModule(reader, op.add(), required, notAllowed);

                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private ModelNode appendAddOperation(List<ModelNode> list, ModelNode parentAddress, String name, String value) {
        ModelNode op = createAddOperation(parentAddress, name, value);
        list.add(op);
        return op;
    }

    private ModelNode createAddOperation(ModelNode parentAddress, String name, String value) {
        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).set(parentAddress).add(name, value);
        return op;
    }

    private void parseAuthorization(List<ModelNode> list, ModelNode parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
        requireNoAttributes(reader);

        ModelNode op = appendAddOperation(list, parentAddress, AUTHORIZATION, CLASSIC);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case POLICY_MODULE: {
                    EnumSet<Attribute> required = EnumSet.of(Attribute.CODE, Attribute.FLAG);
                    EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.TYPE);
                    parseCommonModule(reader, op.get(POLICY_MODULES).add(), required, notAllowed);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseACL(List<ModelNode> list, ModelNode parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
        requireNoAttributes(reader);

        ModelNode op = appendAddOperation(list, parentAddress, ACL, CLASSIC);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ACL_MODULE: {
                    EnumSet<Attribute> required = EnumSet.of(Attribute.CODE, Attribute.FLAG);
                    EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.TYPE);
                    parseCommonModule(reader, op.get(ACL_MODULES).add(), required, notAllowed);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseAudit(List<ModelNode> list, ModelNode parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
        requireNoAttributes(reader);

        ModelNode op = appendAddOperation(list, parentAddress, AUDIT, CLASSIC);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case PROVIDER_MODULE: {
                        EnumSet<Attribute> required = EnumSet.of(Attribute.CODE);
                        EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.TYPE, Attribute.FLAG);
                        parseCommonModule(reader, op.get(PROVIDER_MODULES).add(), required, notAllowed);
                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);
                    }
                }
        }

    }

    private void parseIdentityTrust(List<ModelNode> list, ModelNode parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
        requireNoAttributes(reader);

        ModelNode op = appendAddOperation(list, parentAddress, IDENTITY_TRUST, CLASSIC);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case TRUST_MODULE: {
                    EnumSet<Attribute> required = EnumSet.of(Attribute.CODE, Attribute.FLAG);
                    EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.TYPE);
                    parseCommonModule(reader, op.get(TRUST_MODULES).add(), required, notAllowed);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseMapping(List<ModelNode> list, ModelNode parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
        requireNoAttributes(reader);

        ModelNode op = appendAddOperation(list, parentAddress, MAPPING, CLASSIC);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case MAPPING_MODULE: {
                    EnumSet<Attribute> required = EnumSet.of(Attribute.CODE);
                    EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.FLAG);
                    parseCommonModule(reader, op.get(MAPPING_MODULES).add(), required, notAllowed);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseCommonModule(XMLExtendedStreamReader reader, ModelNode node, EnumSet<Attribute> required,
            EnumSet<Attribute> notAllowed) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            if (notAllowed.contains(attribute))
                throw unexpectedAttribute(reader, i);
            required.remove(attribute);
            switch (attribute) {
                case CODE: {
                    ModelNode code = LoginModulesAttributeDefinition.parseField(CODE, value, reader.getLocation());
                    node.get(CODE).set(code);
                    break;
                }
                case FLAG: {
                    ModelNode flag = LoginModulesAttributeDefinition.parseField(FLAG, value, reader.getLocation());
                    node.get(FLAG).set(flag);
                    break;
                }
                case TYPE: {
                    ModelNode type = MappingModulesAttributeDefinition.parseField(TYPE, value, reader.getLocation());
                    node.get(TYPE).set(type);
                    break;
                }
                case MODULE: {
                    ModelNode module = MappingModulesAttributeDefinition.parseField(MODULE, value, reader.getLocation());
                    node.get(MODULE).set(module);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }

        parseProperties(Element.MODULE_OPTION.getLocalName(), reader, node.get(MODULE_OPTIONS));
    }

    private void parseAuthenticationJaspi(List<ModelNode> list, ModelNode parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
        requireNoAttributes(reader);

        ModelNode op = appendAddOperation(list, parentAddress, AUTHENTICATION, JASPI);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LOGIN_MODULE_STACK: {
                    parseLoginModuleStack(list, op.get(OP_ADDR), reader);
                    break;
                }
                case AUTH_MODULE: {
                    ModelNode node = op.get(AUTH_MODULES);
                    parseAuthModule(reader, node.add());
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseLoginModuleStack(List<ModelNode> list, ModelNode parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
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
                    if (value == null)
                        throw invalidAttributeValue(reader, i);
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


        ModelNode op = appendAddOperation(list, parentAddress, LOGIN_MODULE_STACK, name);
        parseLoginModules(reader, op.get(LOGIN_MODULES));
    }

    private void parseAuthModule(XMLExtendedStreamReader reader, ModelNode op) throws XMLStreamException {
        EnumSet<Attribute> required = EnumSet.of(Attribute.CODE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case CODE: {
                    ModelNode code = JASPIAuthenticationModulesAttributeDefinition.parseField(CODE, value, reader.getLocation());
                    op.get(CODE).set(code);
                    break;
                }
                case LOGIN_MODULE_STACK_REF: {
                    ModelNode ref = JASPIAuthenticationModulesAttributeDefinition.parseField(LOGIN_MODULE_STACK_REF, value, reader.getLocation());
                    op.get(LOGIN_MODULE_STACK_REF).set(ref);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }

        parseProperties(Element.MODULE_OPTION.getLocalName(), reader, op.get(MODULE_OPTIONS));
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
        String val = null;
        EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.VALUE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: {
                    if (value == null)
                        throw invalidAttributeValue(reader, i);
                    name = value.trim();
                    break;
                }
                case VALUE: {
                    val = value != null ? value.trim() : value;
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

    private void parseJSSE(List<ModelNode> list, ModelNode parentAddress, XMLExtendedStreamReader reader) throws XMLStreamException {
        ModelNode op = appendAddOperation(list, parentAddress, JSSE, CLASSIC);
        EnumSet<Attribute> visited = EnumSet.noneOf(Attribute.class);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));

            switch (attribute) {
                case KEYSTORE_PASSWORD: {
                    ModelNode password = KeyStoreAttributeDefinition.parseField(PASSWORD, value, reader.getLocation());
                    op.get(KEYSTORE, PASSWORD).set(password);
                    visited.add(attribute);
                    break;
                }
                case KEYSTORE_TYPE: {
                    ModelNode type = KeyStoreAttributeDefinition.parseField(TYPE, value, reader.getLocation());
                    op.get(KEYSTORE, TYPE).set(type);
                    break;
                }
                case KEYSTORE_URL: {
                    ModelNode url = KeyStoreAttributeDefinition.parseField(URL, value, reader.getLocation());
                    op.get(KEYSTORE, URL).set(url);
                    break;
                }
                case KEYSTORE_PROVIDER: {
                    ModelNode provider = KeyStoreAttributeDefinition.parseField(PROVIDER, value, reader.getLocation());
                    op.get(KEYSTORE, PROVIDER).set(provider);
                    break;
                }
                case KEYSTORE_PROVIDER_ARGUMENT: {
                    ModelNode argument = KeyStoreAttributeDefinition.parseField(PROVIDER_ARGUMENT, value, reader.getLocation());
                    op.get(KEYSTORE, PROVIDER_ARGUMENT).set(argument);
                    break;
                }
                case KEY_MANAGER_FACTORY_PROVIDER: {
                    ModelNode provider = KeyManagerAttributeDefinition.parseField(PROVIDER, value, reader.getLocation());
                    op.get(KEY_MANAGER, PROVIDER).set(provider);
                    break;
                }
                case KEY_MANAGER_FACTORY_ALGORITHM: {
                    ModelNode provider = KeyManagerAttributeDefinition.parseField(ALGORITHM, value, reader.getLocation());
                    op.get(KEY_MANAGER, ALGORITHM).set(provider);
                    break;
                }
                case TRUSTSTORE_PASSWORD: {
                    ModelNode password = KeyStoreAttributeDefinition.parseField(PASSWORD, value, reader.getLocation());
                    op.get(TRUSTSTORE, PASSWORD).set(password);
                    visited.add(attribute);
                    break;
                }
                case TRUSTSTORE_TYPE: {
                    ModelNode type = KeyStoreAttributeDefinition.parseField(TYPE, value, reader.getLocation());
                    op.get(TRUSTSTORE, TYPE).set(type);
                    break;
                }
                case TRUSTSTORE_URL: {
                    ModelNode url = KeyStoreAttributeDefinition.parseField(URL, value, reader.getLocation());
                    op.get(TRUSTSTORE, URL).set(url);
                    break;
                }
                case TRUSTSTORE_PROVIDER: {
                    ModelNode provider = KeyStoreAttributeDefinition.parseField(PROVIDER, value, reader.getLocation());
                    op.get(TRUSTSTORE, PROVIDER).set(provider);
                    break;
                }
                case TRUSTSTORE_PROVIDER_ARGUMENT: {
                    ModelNode argument = KeyStoreAttributeDefinition.parseField(PROVIDER_ARGUMENT, value, reader.getLocation());
                    op.get(TRUSTSTORE, PROVIDER_ARGUMENT).set(argument);
                    break;
                }
                case TRUST_MANAGER_FACTORY_PROVIDER: {
                    ModelNode provider = KeyManagerAttributeDefinition.parseField(PROVIDER, value, reader.getLocation());
                    op.get(TRUST_MANAGER, PROVIDER).set(provider);
                    break;
                }
                case TRUST_MANAGER_FACTORY_ALGORITHM: {
                    ModelNode provider = KeyManagerAttributeDefinition.parseField(ALGORITHM, value, reader.getLocation());
                    op.get(TRUST_MANAGER, ALGORITHM).set(provider);
                    break;
                }
                case CLIENT_ALIAS: {
                    JSSEResourceDefinition.CLIENT_ALIAS.parseAndSetParameter(value, op, reader.getLocation());
                    break;
                }
                case SERVER_ALIAS: {
                    JSSEResourceDefinition.SERVER_ALIAS.parseAndSetParameter(value, op, reader.getLocation());
                    break;
                }
                case CLIENT_AUTH: {
                    JSSEResourceDefinition.CLIENT_AUTH.parseAndSetParameter(value, op, reader.getLocation());
                    break;
                }
                case SERVICE_AUTH_TOKEN: {
                    JSSEResourceDefinition.SERVICE_AUTH_TOKEN.parseAndSetParameter(value, op, reader.getLocation());
                    break;
                }
                case CIPHER_SUITES: {
                    JSSEResourceDefinition.CIPHER_SUITES.parseAndSetParameter(value, op, reader.getLocation());
                    break;
                }
                case PROTOCOLS: {
                    JSSEResourceDefinition.PROTOCOLS.parseAndSetParameter(value, op, reader.getLocation());
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (visited.size() == 0) {
            throw new XMLStreamException("Missing required attribute: either " + Attribute.KEYSTORE_PASSWORD.getLocalName()
                    + " or " + Attribute.TRUSTSTORE_PASSWORD.getLocalName() + " must be present", reader.getLocation());
        }

        parseProperties(Element.PROPERTY.getLocalName(), reader, op.get(ADDITIONAL_PROPERTIES));
    }
}