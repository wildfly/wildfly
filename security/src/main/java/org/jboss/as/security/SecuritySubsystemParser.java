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
import static org.jboss.as.security.CommonAttributes.AUDIT_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.AUTHENTICATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.AUTHORIZATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.DEEP_COPY_SUBJECT_MODE;
import static org.jboss.as.security.CommonAttributes.DEFAULT_CALLBACK_HANDLER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.IDENTITY_TRUST_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.MAPPING_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.MODULE_OPTIONS;
import static org.jboss.as.security.CommonAttributes.SECURITY_DOMAIN;
import static org.jboss.as.security.CommonAttributes.SUBJECT_FACTORY_CLASS_NAME;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
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
                if (policyDetails.hasDefined(Attribute.EXTENDS.getLocalName())) {
                    writer.writeAttribute(Attribute.EXTENDS.getLocalName(), policyDetails.get(Attribute.EXTENDS.getLocalName())
                            .asString());
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
        keys.remove(Attribute.NAME.getLocalName());
        keys.remove(Attribute.EXTENDS.getLocalName());

        for (String key : keys) {
            Element element = Element.forName(key);
            switch (element) {
                case AUTHENTICATION: {
                    writeAuthentication(writer, policyDetails.get(Element.AUTHENTICATION.getLocalName()));
                    break;
                }
                case AUTHORIZATION: {
                    writeAuthorization(writer, policyDetails.get(Element.AUTHORIZATION.getLocalName()));
                    break;
                }
                case ACL: {
                    writeACL(writer, policyDetails.get(Element.ACL.getLocalName()));
                    break;
                }
                case AUDIT: {
                    writeAudit(writer, policyDetails.get(Element.AUDIT.getLocalName()));
                    break;
                }
                case IDENTITY_TRUST: {
                    writeIdentityTrust(writer, policyDetails.get(Element.IDENTITY_TRUST.getLocalName()));
                    break;
                }
                case MAPPING: {
                    writeMapping(writer, policyDetails.get(Element.MAPPING.getLocalName()));
                    break;
                }
                case AUTHENTICATION_JASPI: {
                    writeAuthenticationJaspi(writer, policyDetails.get(Element.AUTHENTICATION_JASPI.getLocalName()));
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
                writeCommonModule(writer, loginModule, Element.AUTHENTICATION);
            }
            writer.writeEndElement();
        }
    }

    private void writeAuthorization(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUTHORIZATION.getLocalName());
            for (ModelNode loginModule : modelNode.asList()) {
                writer.writeStartElement(Element.POLICY_MODULE.getLocalName());
                writeCommonModule(writer, loginModule, Element.AUTHORIZATION);
            }
            writer.writeEndElement();
        }
    }

    private void writeACL(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.ACL.getLocalName());
            for (ModelNode loginModule : modelNode.asList()) {
                writer.writeStartElement(Element.ACL_MODULE.getLocalName());
                writeCommonModule(writer, loginModule, Element.ACL);
            }
            writer.writeEndElement();
        }
    }

    private void writeAudit(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUDIT.getLocalName());
            for (ModelNode loginModule : modelNode.asList()) {
                writer.writeStartElement(Element.PROVIDER_MODULE.getLocalName());
                writeCommonModule(writer, loginModule, Element.AUDIT);
            }
            writer.writeEndElement();
        }
    }

    private void writeIdentityTrust(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.IDENTITY_TRUST.getLocalName());
            for (ModelNode loginModule : modelNode.asList()) {
                writer.writeStartElement(Element.TRUST_MODULE.getLocalName());
                writeCommonModule(writer, loginModule, Element.IDENTITY_TRUST);
            }
            writer.writeEndElement();
        }
    }

    private void writeMapping(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.MAPPING.getLocalName());
            for (ModelNode loginModule : modelNode.asList()) {
                writer.writeStartElement(Element.MAPPING_MODULE.getLocalName());
                writeCommonModule(writer, loginModule, Element.MAPPING);
            }
            writer.writeEndElement();
        }
    }

    private void writeAuthenticationJaspi(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUTHENTICATION_JASPI.getLocalName());
            ModelNode moduleStack = modelNode.get(Element.LOGIN_MODULE_STACK.getLocalName());
            writeLoginModuleStack(writer, moduleStack);
            ModelNode authModule = modelNode.get(Element.AUTH_MODULE.getLocalName());
            writeAuthModule(writer, authModule);
            writer.writeEndElement();
        }
    }

    private void writeLoginModuleStack(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.LOGIN_MODULE_STACK.getLocalName());
            List<ModelNode> modules = modelNode.asList();
            Iterator<ModelNode> iter = modules.iterator();
            ModelNode nameNode = iter.next();
            writer.writeAttribute(Attribute.NAME.getLocalName(), nameNode.require(Attribute.NAME.getLocalName()).asString());
            while (iter.hasNext()) {
                ModelNode loginModule = iter.next();
                writer.writeStartElement(Element.LOGIN_MODULE.getLocalName());
                writeCommonModule(writer, loginModule, Element.AUTHENTICATION);
            }
            writer.writeEndElement();
        }
    }

    private void writeAuthModule(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUTH_MODULE.getLocalName());
            writeCommonModule(writer, modelNode, Element.AUTH_MODULE);
        }
    }

    private void writeCommonModule(XMLExtendedStreamWriter writer, ModelNode module, Element type) throws XMLStreamException {
        // check map for known modules
        String code = module.require(Attribute.CODE.getLocalName()).asString();
        code = getCode(code, type);
        writer.writeAttribute(Attribute.CODE.getLocalName(), code);
        if (module.hasDefined(Attribute.FLAG.getLocalName()))
            writer.writeAttribute(Attribute.FLAG.getLocalName(), module.get(Attribute.FLAG.getLocalName()).asString());
        if (module.hasDefined(Attribute.TYPE.getLocalName()))
            writer.writeAttribute(Attribute.TYPE.getLocalName(), module.get(Attribute.TYPE.getLocalName()).asString());
        if (module.hasDefined(Attribute.LOGIN_MODULE_STACK_REF.getLocalName()))
            writer.writeAttribute(Attribute.LOGIN_MODULE_STACK_REF.getLocalName(),
                    module.get(Attribute.LOGIN_MODULE_STACK_REF.getLocalName()).asString());
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
                    op.get(attribute.getLocalName()).set(value);
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
                            parseAuthentication(reader, op.get(Element.AUTHENTICATION.getLocalName()), true);
                            break;
                        }
                        case AUTHORIZATION: {
                            parseAuthorization(reader, op.get(Element.AUTHORIZATION.getLocalName()));
                            break;
                        }
                        case ACL: {
                            parseACL(reader, op.get(Element.ACL.getLocalName()));
                            break;
                        }
                        case AUDIT: {
                            parseAudit(reader, op.get(Element.AUDIT.getLocalName()));
                            break;
                        }
                        case IDENTITY_TRUST: {
                            parseIdentityTrust(reader, op.get(Element.IDENTITY_TRUST.getLocalName()));
                            break;
                        }
                        case MAPPING: {
                            parseMapping(reader, op.get(Element.MAPPING.getLocalName()));
                            break;
                        }
                        case AUTHENTICATION_JASPI: {
                            if (visited.contains(Element.AUTHENTICATION))
                                throw new XMLStreamException(
                                        "A security domain can have either an <authentication> or <authentication-jaspi> element, not both",
                                        reader.getLocation());
                            parseAuthenticationJaspi(reader, op.get(Element.AUTHENTICATION_JASPI.getLocalName()));
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
                            parseCommonModule(reader, op.add(), required, notAllowed, Element.AUTHENTICATION);
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
                            parseCommonModule(reader, op.add(), required, notAllowed, Element.AUTHORIZATION);
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
                            parseCommonModule(reader, op.add(), required, notAllowed, Element.ACL);
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
                            parseCommonModule(reader, op.add(), required, notAllowed, Element.AUDIT);
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
                            parseCommonModule(reader, op.add(), required, notAllowed, Element.IDENTITY_TRUST);
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
                            parseCommonModule(reader, op.add(), required, notAllowed, Element.MAPPING);
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
            EnumSet<Attribute> notAllowed, Element type) throws XMLStreamException {
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
                    String code = null;
                    // check map for known modules
                    switch (type) {
                        case AUTHENTICATION: {
                            code = AUTHENTICATION_MAP.get(value);
                            break;
                        }
                        default: // TODO
                    }
                    if (code == null)
                        code = value;
                    node.get(Attribute.CODE.getLocalName()).set(code);
                    break;
                }
                case FLAG: {
                    validateFlag(value, reader, i);
                    node.get(Attribute.FLAG.getLocalName()).set(value);
                    break;
                }
                case TYPE: {
                    validateType(value, reader, i);
                    node.get(Attribute.TYPE.getLocalName()).set(value);
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
        final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (!visited.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    switch (element) {
                        case LOGIN_MODULE_STACK: {
                            parseLoginModuleStack(reader, op.get(Element.LOGIN_MODULE_STACK.getLocalName()));
                            break;
                        }
                        case AUTH_MODULE: {
                            parseAuthModule(reader, op.get(Element.AUTH_MODULE.getLocalName()));
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
        ModelNode moduleStack = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: {
                    moduleStack = op.add(Attribute.NAME.getLocalName(), value);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }

        parseAuthentication(reader, moduleStack, false);
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
                    op.get(Attribute.CODE.getLocalName()).set(value);
                    break;
                }
                case LOGIN_MODULE_STACK_REF: {
                    op.get(Attribute.LOGIN_MODULE_STACK_REF.getLocalName()).set(value);
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

    private String getCode(String code, Element type) {
        String value = null;
        switch (type) {
            case AUTHENTICATION: {
                Set<Entry<String, String>> entries = AUTHENTICATION_MAP.entrySet();
                for (Entry<String, String> mapEntry : entries) {
                    if (mapEntry.getValue().equals(code)) {
                        value = mapEntry.getKey();
                        break;
                    }
                }
                return value;
            }
            default:
                return code;
        }
    }

}
