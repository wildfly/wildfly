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
import static org.jboss.as.security.Constants.ACL;
import static org.jboss.as.security.Constants.ADDITIONAL_PROPERTIES;
import static org.jboss.as.security.Constants.AUDIT;
import static org.jboss.as.security.Constants.AUDIT_MANAGER_CLASS_NAME;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.AUTHENTICATION_JASPI;
import static org.jboss.as.security.Constants.AUTHENTICATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.Constants.AUTHORIZATION;
import static org.jboss.as.security.Constants.AUTHORIZATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.Constants.AUTH_MODULE;
import static org.jboss.as.security.Constants.CIPHER_SUITES;
import static org.jboss.as.security.Constants.CLIENT_ALIAS;
import static org.jboss.as.security.Constants.CLIENT_AUTH;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.DEEP_COPY_SUBJECT_MODE;
import static org.jboss.as.security.Constants.DEFAULT_CALLBACK_HANDLER_CLASS_NAME;
import static org.jboss.as.security.Constants.EXTENDS;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.IDENTITY_TRUST;
import static org.jboss.as.security.Constants.IDENTITY_TRUST_MANAGER_CLASS_NAME;
import static org.jboss.as.security.Constants.JSSE;
import static org.jboss.as.security.Constants.KEYSTORE_PASSWORD;
import static org.jboss.as.security.Constants.KEYSTORE_PROVIDER;
import static org.jboss.as.security.Constants.KEYSTORE_PROVIDER_ARGUMENT;
import static org.jboss.as.security.Constants.KEYSTORE_TYPE;
import static org.jboss.as.security.Constants.KEYSTORE_URL;
import static org.jboss.as.security.Constants.KEY_MANAGER_FACTORY_ALGORITHM;
import static org.jboss.as.security.Constants.KEY_MANAGER_FACTORY_PROVIDER;
import static org.jboss.as.security.Constants.LOGIN_MODULE_STACK;
import static org.jboss.as.security.Constants.LOGIN_MODULE_STACK_REF;
import static org.jboss.as.security.Constants.MAPPING;
import static org.jboss.as.security.Constants.MAPPING_MANAGER_CLASS_NAME;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.NAME;
import static org.jboss.as.security.Constants.PROTOCOLS;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.jboss.as.security.Constants.SERVER_ALIAS;
import static org.jboss.as.security.Constants.SERVICE_AUTH_TOKEN;
import static org.jboss.as.security.Constants.SUBJECT_FACTORY_CLASS_NAME;
import static org.jboss.as.security.Constants.TRUSTSTORE_PASSWORD;
import static org.jboss.as.security.Constants.TRUSTSTORE_PROVIDER;
import static org.jboss.as.security.Constants.TRUSTSTORE_PROVIDER_ARGUMENT;
import static org.jboss.as.security.Constants.TRUSTSTORE_TYPE;
import static org.jboss.as.security.Constants.TRUSTSTORE_URL;
import static org.jboss.as.security.Constants.TRUST_MANAGER_FACTORY_ALGORITHM;
import static org.jboss.as.security.Constants.TRUST_MANAGER_FACTORY_PROVIDER;
import static org.jboss.as.security.Constants.TYPE;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
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
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Brian Stansberry
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
        final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (!visited.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    switch (element) {
                        case SECURITY_MANAGEMENT: {
                            parseSecurityManagement(reader, subsystem);
                            break;
                        }
                        case SUBJECT_FACTORY: {
                            parseSubjectFactory(reader, subsystem);
                            break;
                        }
                        case SECURITY_DOMAINS: {
                            securityDomainsUpdates = parseSecurityDomains(reader, address);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        list.add(subsystem);

        if (securityDomainsUpdates != null) {
            list.addAll(securityDomainsUpdates);
        }
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = context.getModelNode();

        if (isNonStandard(node, AUTHENTICATION_MANAGER_CLASS_NAME)
                || (node.hasDefined(DEEP_COPY_SUBJECT_MODE) && node.get(DEEP_COPY_SUBJECT_MODE).asBoolean())
                || isNonStandard(node, DEFAULT_CALLBACK_HANDLER_CLASS_NAME)
                || isNonStandard(node, AUTHORIZATION_MANAGER_CLASS_NAME) || isNonStandard(node, AUDIT_MANAGER_CLASS_NAME)
                || isNonStandard(node, IDENTITY_TRUST_MANAGER_CLASS_NAME) || isNonStandard(node, MAPPING_MANAGER_CLASS_NAME)) {
            writer.writeEmptyElement(Element.SECURITY_MANAGEMENT.getLocalName());
            if (isNonStandard(node, AUTHENTICATION_MANAGER_CLASS_NAME)) {
                writeAttribute(writer, Attribute.AUTHENTICATION_MANAGER_CLASS_NAME, node.get(AUTHENTICATION_MANAGER_CLASS_NAME));
            }
            if (node.hasDefined(DEEP_COPY_SUBJECT_MODE) && node.get(DEEP_COPY_SUBJECT_MODE).asBoolean()) {
                writeAttribute(writer, Attribute.DEEP_COPY_SUBJECT_MODE, node.get(DEEP_COPY_SUBJECT_MODE));
            }
            if (isNonStandard(node, DEFAULT_CALLBACK_HANDLER_CLASS_NAME)) {
                writeAttribute(writer, Attribute.DEFAULT_CALLBACK_HANDLER_CLASS_NAME,
                        node.get(DEFAULT_CALLBACK_HANDLER_CLASS_NAME));
            }
            if (isNonStandard(node, AUTHORIZATION_MANAGER_CLASS_NAME)) {
                writeAttribute(writer, Attribute.AUTHORIZATION_MANAGER_CLASS_NAME, node.get(AUTHORIZATION_MANAGER_CLASS_NAME));
            }
            if (isNonStandard(node, AUDIT_MANAGER_CLASS_NAME)) {
                writeAttribute(writer, Attribute.AUDIT_MANAGER_CLASS_NAME, node.get(AUDIT_MANAGER_CLASS_NAME));
            }
            if (isNonStandard(node, IDENTITY_TRUST_MANAGER_CLASS_NAME)) {
                writeAttribute(writer, Attribute.IDENTITY_TRUST_MANAGER_CLASS_NAME, node.get(IDENTITY_TRUST_MANAGER_CLASS_NAME));
            }
            if (isNonStandard(node, MAPPING_MANAGER_CLASS_NAME)) {
                writeAttribute(writer, Attribute.MAPPING_MANAGER_CLASS_NAME, node.get(MAPPING_MANAGER_CLASS_NAME));
            }
        }

        if (isNonStandard(node, SUBJECT_FACTORY_CLASS_NAME)) {
            writer.writeEmptyElement(Element.SUBJECT_FACTORY.getLocalName());
            writeAttribute(writer, Attribute.SUBJECT_FACTORY_CLASS_NAME, node.get(SUBJECT_FACTORY_CLASS_NAME));
        }

        if (node.hasDefined(SECURITY_DOMAIN) && node.get(SECURITY_DOMAIN).asInt() > 0) {
            writer.writeStartElement(Element.SECURITY_DOMAINS.getLocalName());
            for (Property policy : node.get(SECURITY_DOMAIN).asPropertyList()) {
                writer.writeStartElement(Element.SECURITY_DOMAIN.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), policy.getName());
                ModelNode policyDetails = policy.getValue();
                if (policyDetails.hasDefined(EXTENDS)) {
                    writeAttribute(writer, Attribute.EXTENDS, policyDetails.get(EXTENDS));
                }
                writeSecurityDomainContent(writer, policyDetails);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeSecurityDomainContent(XMLExtendedStreamWriter writer, ModelNode policyDetails) throws XMLStreamException {
        Set<String> keys = new HashSet<String>(policyDetails.keys());
        keys.remove(NAME);
        keys.remove(EXTENDS);

        for (String key : keys) {
            Element element = Element.forName(key);
            switch (element) {
                case AUTHENTICATION: {
                    writeAuthentication(writer, policyDetails.get(AUTHENTICATION));
                    break;
                }
                case AUTHORIZATION: {
                    writeAuthorization(writer, policyDetails.get(AUTHORIZATION));
                    break;
                }
                case ACL: {
                    writeACL(writer, policyDetails.get(ACL));
                    break;
                }
                case AUDIT: {
                    writeAudit(writer, policyDetails.get(AUDIT));
                    break;
                }
                case IDENTITY_TRUST: {
                    writeIdentityTrust(writer, policyDetails.get(IDENTITY_TRUST));
                    break;
                }
                case MAPPING: {
                    writeMapping(writer, policyDetails.get(MAPPING));
                    break;
                }
                case AUTHENTICATION_JASPI: {
                    writeAuthenticationJaspi(writer, policyDetails.get(AUTHENTICATION_JASPI));
                    break;
                }
                case JSSE: {
                    writeJSSE(writer, policyDetails.get(JSSE));
                    break;
                }
                default:
                    throw new IllegalStateException("Unexpected field " + element.getLocalName());
            }
        }
    }

    private void writeAuthentication(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUTHENTICATION.getLocalName());
            for (ModelNode loginModule : modelNode.asList()) {
                writer.writeStartElement(Element.LOGIN_MODULE.getLocalName());
                writeCommonModule(writer, loginModule);
            }
            writer.writeEndElement();
        }
    }

    private void writeAuthorization(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUTHORIZATION.getLocalName());
            for (ModelNode loginModule : modelNode.asList()) {
                writer.writeStartElement(Element.POLICY_MODULE.getLocalName());
                writeCommonModule(writer, loginModule);
            }
            writer.writeEndElement();
        }
    }

    private void writeACL(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.ACL.getLocalName());
            for (ModelNode loginModule : modelNode.asList()) {
                writer.writeStartElement(Element.ACL_MODULE.getLocalName());
                writeCommonModule(writer, loginModule);
            }
            writer.writeEndElement();
        }
    }

    private void writeAudit(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUDIT.getLocalName());
            for (ModelNode loginModule : modelNode.asList()) {
                writer.writeStartElement(Element.PROVIDER_MODULE.getLocalName());
                writeCommonModule(writer, loginModule);
            }
            writer.writeEndElement();
        }
    }

    private void writeIdentityTrust(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.IDENTITY_TRUST.getLocalName());
            for (ModelNode loginModule : modelNode.asList()) {
                writer.writeStartElement(Element.TRUST_MODULE.getLocalName());
                writeCommonModule(writer, loginModule);
            }
            writer.writeEndElement();
        }
    }

    private void writeMapping(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.MAPPING.getLocalName());
            for (ModelNode loginModule : modelNode.asList()) {
                writer.writeStartElement(Element.MAPPING_MODULE.getLocalName());
                writeCommonModule(writer, loginModule);
            }
            writer.writeEndElement();
        }
    }

    private void writeAuthenticationJaspi(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUTHENTICATION_JASPI.getLocalName());
            ModelNode moduleStack = modelNode.get(LOGIN_MODULE_STACK);
            writeLoginModuleStack(writer, moduleStack);
            ModelNode authModule = modelNode.get(AUTH_MODULE);
            writeAuthModule(writer, authModule);
            writer.writeEndElement();
        }
    }

    private void writeLoginModuleStack(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            List<ModelNode> stacks = modelNode.asList();
            for (ModelNode stack : stacks) {
                writer.writeStartElement(Element.LOGIN_MODULE_STACK.getLocalName());
                List<ModelNode> nodes = stack.asList();
                Iterator<ModelNode> iter = nodes.iterator();
                ModelNode nameNode = iter.next();
                writeAttribute(writer, Attribute.NAME, nameNode.require(NAME));
                while (iter.hasNext()) {
                    ModelNode loginModuleNode = iter.next();
                    List<ModelNode> lms = loginModuleNode.asList();
                    for (ModelNode loginModule : lms) {
                        writer.writeStartElement(Element.LOGIN_MODULE.getLocalName());
                        writeCommonModule(writer, loginModule);
                    }
                }
                writer.writeEndElement();
            }
        }
    }

    private void writeAuthModule(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            List<ModelNode> authModulesNode = modelNode.asList();
            for (ModelNode authModule : authModulesNode) {
                writer.writeStartElement(Element.AUTH_MODULE.getLocalName());
                writeCommonModule(writer, authModule);
            }
        }
    }

    private void writeCommonModule(XMLExtendedStreamWriter writer, ModelNode module) throws XMLStreamException {
        String code = module.require(CODE).asString();
        writer.writeAttribute(Attribute.CODE.getLocalName(), code);
        if (module.hasDefined(FLAG))
            writeAttribute(writer, Attribute.FLAG, module.get(FLAG));
        if (module.hasDefined(TYPE))
            writeAttribute(writer, Attribute.TYPE, module.get(TYPE));
        if (module.hasDefined(LOGIN_MODULE_STACK_REF))
            writeAttribute(writer, Attribute.LOGIN_MODULE_STACK_REF, module.get(LOGIN_MODULE_STACK_REF));
        if (module.hasDefined(MODULE_OPTIONS) && module.get(MODULE_OPTIONS).asInt() > 0) {
            writeModuleOptions(writer, module.get(MODULE_OPTIONS));
        }
        writer.writeEndElement();
    }

    private void writeModuleOptions(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        for (Property prop : modelNode.asPropertyList()) {
            writer.writeEmptyElement(Element.MODULE_OPTION.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), prop.getName());
            writer.writeAttribute(Attribute.VALUE.getLocalName(), prop.getValue().asString());
        }
    }

    private void writeJSSE(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.JSSE.getLocalName());
            if (modelNode.hasDefined(KEYSTORE_PASSWORD))
                writeAttribute(writer, Attribute.KEYSTORE_PASSWORD, modelNode.get(KEYSTORE_PASSWORD));
            if (modelNode.hasDefined(KEYSTORE_URL))
                writeAttribute(writer, Attribute.KEYSTORE_URL, modelNode.get(KEYSTORE_URL));
            if (modelNode.hasDefined(KEYSTORE_TYPE))
                writeAttribute(writer, Attribute.KEYSTORE_TYPE, modelNode.get(KEYSTORE_TYPE));
            if (modelNode.hasDefined(KEYSTORE_PROVIDER))
                writeAttribute(writer, Attribute.KEYSTORE_PROVIDER, modelNode.get(KEYSTORE_PROVIDER));
            if (modelNode.hasDefined(KEYSTORE_PROVIDER_ARGUMENT))
                writeAttribute(writer, Attribute.KEYSTORE_PROVIDER_ARGUMENT, modelNode.get(KEYSTORE_PROVIDER_ARGUMENT));
            if (modelNode.hasDefined(KEY_MANAGER_FACTORY_PROVIDER))
                writeAttribute(writer, Attribute.KEY_MANAGER_FACTORY_PROVIDER, modelNode.get(KEY_MANAGER_FACTORY_PROVIDER));
            if (modelNode.hasDefined(KEY_MANAGER_FACTORY_ALGORITHM))
                writeAttribute(writer, Attribute.KEY_MANAGER_FACTORY_ALGORITHM, modelNode.get(KEY_MANAGER_FACTORY_ALGORITHM));
            if (modelNode.hasDefined(TRUSTSTORE_PASSWORD))
                writeAttribute(writer, Attribute.TRUSTSTORE_PASSWORD, modelNode.get(TRUSTSTORE_PASSWORD));
            if (modelNode.hasDefined(TRUSTSTORE_URL))
                writeAttribute(writer, Attribute.TRUSTSTORE_URL, modelNode.get(TRUSTSTORE_URL));
            if (modelNode.hasDefined(TRUSTSTORE_TYPE))
                writeAttribute(writer, Attribute.TRUSTSTORE_TYPE, modelNode.get(TRUSTSTORE_TYPE));
            if (modelNode.hasDefined(TRUSTSTORE_PROVIDER))
                writeAttribute(writer, Attribute.TRUSTSTORE_PROVIDER, modelNode.get(TRUSTSTORE_PROVIDER));
            if (modelNode.hasDefined(TRUSTSTORE_PROVIDER_ARGUMENT))
                writeAttribute(writer, Attribute.TRUSTSTORE_PROVIDER_ARGUMENT, modelNode.get(TRUSTSTORE_PROVIDER_ARGUMENT));
            if (modelNode.hasDefined(TRUST_MANAGER_FACTORY_PROVIDER))
                writeAttribute(writer, Attribute.TRUST_MANAGER_FACTORY_PROVIDER, modelNode.get(TRUST_MANAGER_FACTORY_PROVIDER));
            if (modelNode.hasDefined(TRUST_MANAGER_FACTORY_ALGORITHM))
                writeAttribute(writer, Attribute.TRUST_MANAGER_FACTORY_ALGORITHM,
                        modelNode.get(TRUST_MANAGER_FACTORY_ALGORITHM));
            if (modelNode.hasDefined(CLIENT_ALIAS))
                writeAttribute(writer, Attribute.CLIENT_ALIAS, modelNode.get(CLIENT_ALIAS));
            if (modelNode.hasDefined(SERVER_ALIAS))
                writeAttribute(writer, Attribute.SERVER_ALIAS, modelNode.get(SERVER_ALIAS));
            if (modelNode.hasDefined(CLIENT_AUTH))
                writeAttribute(writer, Attribute.CLIENT_AUTH, modelNode.get(CLIENT_AUTH));
            if (modelNode.hasDefined(SERVICE_AUTH_TOKEN))
                writeAttribute(writer, Attribute.SERVICE_AUTH_TOKEN, modelNode.get(SERVICE_AUTH_TOKEN));
            if (modelNode.hasDefined(CIPHER_SUITES))
                writeAttribute(writer, Attribute.CIPHER_SUITES, modelNode.get(CIPHER_SUITES));
            if (modelNode.hasDefined(PROTOCOLS))
                writeAttribute(writer, Attribute.PROTOCOLS, modelNode.get(PROTOCOLS));
            if (modelNode.hasDefined(ADDITIONAL_PROPERTIES)) {
                writer.writeStartElement(Element.ADDITIONAL_PROPERTIES.getLocalName());
                writer.writeCharacters(modelNode.get(ADDITIONAL_PROPERTIES).asString());
            }
            writer.writeEndElement();
        }
    }

    private boolean isNonStandard(ModelNode node, String attribute) {
        return node.hasDefined(attribute) && !"default".equals(node.get(attribute).asString());
    }

    private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value)
            throws XMLStreamException {
        writer.writeAttribute(attr.getLocalName(), value.asString());
    }

    private void parseSecurityManagement(final XMLExtendedStreamReader reader, final ModelNode operation)
            throws XMLStreamException {
        String authenticationManagerClassName = null;
        boolean deepCopySubjectMode = false;
        String defaultCallbackHandlerClassName = null;
        String authorizationManagerClassName = null;
        String auditManagerClassName = null;
        String identityTrustManagerClassName = null;
        String mappingManagerClassName = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case AUTHENTICATION_MANAGER_CLASS_NAME: {
                    authenticationManagerClassName = value;
                    break;
                }
                case DEEP_COPY_SUBJECT_MODE: {
                    deepCopySubjectMode = Boolean.parseBoolean(value);
                    break;
                }
                case DEFAULT_CALLBACK_HANDLER_CLASS_NAME: {
                    defaultCallbackHandlerClassName = value;
                    break;
                }
                case AUTHORIZATION_MANAGER_CLASS_NAME: {
                    authorizationManagerClassName = value;
                    break;
                }
                case AUDIT_MANAGER_CLASS_NAME: {
                    auditManagerClassName = value;
                    break;
                }
                case IDENTITY_TRUST_MANAGER_CLASS_NAME: {
                    identityTrustManagerClassName = value;
                    break;
                }
                case MAPPING_MANAGER_CLASS_NAME: {
                    mappingManagerClassName = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);

        if (authenticationManagerClassName != null) {
            operation.get(AUTHENTICATION_MANAGER_CLASS_NAME).set(authenticationManagerClassName);
        }
        if (defaultCallbackHandlerClassName != null) {
            operation.get(DEFAULT_CALLBACK_HANDLER_CLASS_NAME).set(defaultCallbackHandlerClassName);
        }
        if (deepCopySubjectMode) {
            operation.get(DEEP_COPY_SUBJECT_MODE).set(deepCopySubjectMode);
        }
        if (authorizationManagerClassName != null) {
            operation.get(AUTHORIZATION_MANAGER_CLASS_NAME).set(authorizationManagerClassName);
        }
        if (auditManagerClassName != null) {
            operation.get(AUDIT_MANAGER_CLASS_NAME).set(auditManagerClassName);
        }
        if (identityTrustManagerClassName != null) {
            operation.get(IDENTITY_TRUST_MANAGER_CLASS_NAME).set(identityTrustManagerClassName);
        }
        if (mappingManagerClassName != null) {
            operation.get(MAPPING_MANAGER_CLASS_NAME).set(mappingManagerClassName);
        }
    }

    private void parseSubjectFactory(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {
        String subjectFactoryClassName = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SUBJECT_FACTORY_CLASS_NAME: {
                    subjectFactoryClassName = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);

        if (subjectFactoryClassName != null) {
            operation.get(SUBJECT_FACTORY_CLASS_NAME).set(subjectFactoryClassName);
        }
    }

    private List<ModelNode> parseSecurityDomains(final XMLExtendedStreamReader reader, final ModelNode parentAddress)
            throws XMLStreamException {
        requireNoAttributes(reader);

        List<ModelNode> list = new ArrayList<ModelNode>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case SECURITY_DOMAIN: {
                            list.add(parseSecurityDomain(reader, parentAddress));
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        return list;
    }

    private ModelNode parseSecurityDomain(XMLExtendedStreamReader reader, ModelNode parentAddress) throws XMLStreamException {
        ModelNode op = new ModelNode();
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
                    address.set(parentAddress).add(SECURITY_DOMAIN, value);
                    break;
                }
                case EXTENDS: {
                    op.get(EXTENDS).set(value);
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
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
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
                            parseAuthentication(reader, op.get(AUTHENTICATION), true);
                            break;
                        }
                        case AUTHORIZATION: {
                            parseAuthorization(reader, op.get(AUTHORIZATION));
                            break;
                        }
                        case ACL: {
                            parseACL(reader, op.get(ACL));
                            break;
                        }
                        case AUDIT: {
                            parseAudit(reader, op.get(AUDIT));
                            break;
                        }
                        case IDENTITY_TRUST: {
                            parseIdentityTrust(reader, op.get(IDENTITY_TRUST));
                            break;
                        }
                        case MAPPING: {
                            parseMapping(reader, op.get(MAPPING));
                            break;
                        }
                        case AUTHENTICATION_JASPI: {
                            if (visited.contains(Element.AUTHENTICATION))
                                throw new XMLStreamException(
                                        "A security domain can have either an <authentication> or <authentication-jaspi> element, not both",
                                        reader.getLocation());
                            parseAuthenticationJaspi(reader, op.get(AUTHENTICATION_JASPI));
                            break;
                        }
                        case JSSE: {
                            parseJSSE(reader, op.get(JSSE));
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        return op;
    }

    private void parseAuthentication(XMLExtendedStreamReader reader, ModelNode op, boolean requireNoAttributes)
            throws XMLStreamException {
        if (requireNoAttributes)
            requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
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
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseAuthorization(XMLExtendedStreamReader reader, ModelNode op) throws XMLStreamException {
        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case POLICY_MODULE: {
                            EnumSet<Attribute> required = EnumSet.of(Attribute.CODE, Attribute.FLAG);
                            EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.TYPE);
                            parseCommonModule(reader, op.add(), required, notAllowed);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseACL(XMLExtendedStreamReader reader, ModelNode op) throws XMLStreamException {
        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case ACL_MODULE: {
                            EnumSet<Attribute> required = EnumSet.of(Attribute.CODE, Attribute.FLAG);
                            EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.TYPE);
                            parseCommonModule(reader, op.add(), required, notAllowed);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseAudit(XMLExtendedStreamReader reader, ModelNode op) throws XMLStreamException {
        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case PROVIDER_MODULE: {
                            EnumSet<Attribute> required = EnumSet.of(Attribute.CODE);
                            EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.TYPE, Attribute.FLAG);
                            parseCommonModule(reader, op.add(), required, notAllowed);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseIdentityTrust(XMLExtendedStreamReader reader, ModelNode op) throws XMLStreamException {
        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case TRUST_MODULE: {
                            EnumSet<Attribute> required = EnumSet.of(Attribute.CODE, Attribute.FLAG);
                            EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.TYPE);
                            parseCommonModule(reader, op.add(), required, notAllowed);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseMapping(XMLExtendedStreamReader reader, ModelNode op) throws XMLStreamException {
        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case MAPPING_MODULE: {
                            EnumSet<Attribute> required = EnumSet.of(Attribute.CODE);
                            EnumSet<Attribute> notAllowed = EnumSet.of(Attribute.FLAG);
                            parseCommonModule(reader, op.add(), required, notAllowed);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
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
                    String code = value;
                    node.get(CODE).set(code);
                    break;
                }
                case FLAG: {
                    validateFlag(value, reader, i);
                    node.get(FLAG).set(value);
                    break;
                }
                case TYPE: {
                    validateType(value, reader, i);
                    node.get(TYPE).set(value);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case MODULE_OPTION: {
                            parseModuleOption(reader, node.get(MODULE_OPTIONS));
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseAuthenticationJaspi(XMLExtendedStreamReader reader, ModelNode op) throws XMLStreamException {
        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case LOGIN_MODULE_STACK: {
                            ModelNode node = op.get(LOGIN_MODULE_STACK);
                            parseLoginModuleStack(reader, node.add());
                            break;
                        }
                        case AUTH_MODULE: {
                            ModelNode node = op.get(AUTH_MODULE);
                            parseAuthModule(reader, node.add());
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseLoginModuleStack(XMLExtendedStreamReader reader, ModelNode op) throws XMLStreamException {
        EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: {
                    op.add().get(NAME).set(value);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }
        parseAuthentication(reader, op.add(), false);
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
                    op.get(CODE).set(value);
                    break;
                }
                case LOGIN_MODULE_STACK_REF: {
                    op.get(LOGIN_MODULE_STACK_REF).set(value);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case MODULE_OPTION: {
                            parseModuleOption(reader, op.get(MODULE_OPTIONS));
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void validateFlag(String flag, XMLExtendedStreamReader reader, int index) throws XMLStreamException {
        if (!(flag.equals("required") || flag.equals("requisite") || flag.equals("sufficient") || flag.equals("optional")))
            throw invalidAttributeValue(reader, index);
    }

    private void validateType(String type, XMLExtendedStreamReader reader, int index) throws XMLStreamException {
        if (!(type.equals("attribute") || type.equals("credential") || type.equals("principal") || type.equals("role")))
            throw invalidAttributeValue(reader, index);
    }

    private void parseModuleOption(XMLExtendedStreamReader reader, ModelNode moduleOptions) throws XMLStreamException {
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
                    name = value;
                    break;
                }
                case VALUE: {
                    val = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }

        moduleOptions.add(name, val);
        requireNoContent(reader);
    }

    private void parseJSSE(XMLExtendedStreamReader reader, ModelNode op) throws XMLStreamException {
        EnumSet<Attribute> visited = EnumSet.noneOf(Attribute.class);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case KEYSTORE_PASSWORD: {
                    op.get(KEYSTORE_PASSWORD).set(value);
                    visited.add(attribute);
                    break;
                }
                case KEYSTORE_TYPE: {
                    op.get(KEYSTORE_TYPE).set(value);
                    break;
                }
                case KEYSTORE_URL: {
                    op.get(KEYSTORE_URL).set(value);
                    break;
                }
                case KEYSTORE_PROVIDER: {
                    op.get(KEYSTORE_PROVIDER).set(value);
                    break;
                }
                case KEYSTORE_PROVIDER_ARGUMENT: {
                    op.get(KEYSTORE_PROVIDER_ARGUMENT).set(value);
                    break;
                }
                case KEY_MANAGER_FACTORY_PROVIDER: {
                    op.get(KEY_MANAGER_FACTORY_PROVIDER).set(value);
                    break;
                }
                case KEY_MANAGER_FACTORY_ALGORITHM: {
                    op.get(KEY_MANAGER_FACTORY_ALGORITHM).set(value);
                    break;
                }
                case TRUSTSTORE_PASSWORD: {
                    op.get(TRUSTSTORE_PASSWORD).set(value);
                    visited.add(attribute);
                    break;
                }
                case TRUSTSTORE_TYPE: {
                    op.get(TRUSTSTORE_TYPE).set(value);
                    break;
                }
                case TRUSTSTORE_URL: {
                    op.get(TRUSTSTORE_URL).set(value);
                    break;
                }
                case TRUSTSTORE_PROVIDER: {
                    op.get(TRUSTSTORE_PROVIDER).set(value);
                    break;
                }
                case TRUSTSTORE_PROVIDER_ARGUMENT: {
                    op.get(TRUSTSTORE_PROVIDER_ARGUMENT).set(value);
                    break;
                }
                case TRUST_MANAGER_FACTORY_PROVIDER: {
                    op.get(TRUST_MANAGER_FACTORY_PROVIDER).set(value);
                    break;
                }
                case TRUST_MANAGER_FACTORY_ALGORITHM: {
                    op.get(TRUST_MANAGER_FACTORY_ALGORITHM).set(value);
                    break;
                }
                case CLIENT_ALIAS: {
                    op.get(CLIENT_ALIAS).set(value);
                    break;
                }
                case SERVER_ALIAS: {
                    op.get(SERVER_ALIAS).set(value);
                    break;
                }
                case CLIENT_AUTH: {
                    op.get(CLIENT_AUTH).set(value);
                    break;
                }
                case SERVICE_AUTH_TOKEN: {
                    op.get(SERVICE_AUTH_TOKEN).set(value);
                    break;
                }
                case CIPHER_SUITES: {
                    op.get(CIPHER_SUITES).set(value);
                    break;
                }
                case PROTOCOLS: {
                    op.get(PROTOCOLS).set(value);
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

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
                    requireNoAttributes(reader);
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case ADDITIONAL_PROPERTIES: {
                            op.get(ADDITIONAL_PROPERTIES).set(reader.getElementText().trim());
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

}
