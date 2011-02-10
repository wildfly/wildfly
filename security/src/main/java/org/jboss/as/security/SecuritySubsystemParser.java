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
 */package org.jboss.as.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.security.CommonAttributes.AUTHENTICATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.DEEP_COPY_SUBJECT_MODE;
import static org.jboss.as.security.CommonAttributes.DEFAULT_CALLBACK_HANDLER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.JAAS_APPLICATION_POLICY;
import static org.jboss.as.security.CommonAttributes.MODULE_OPTIONS;
import static org.jboss.as.security.CommonAttributes.SUBJECT_FACTORY_CLASS_NAME;

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
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a> *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class SecuritySubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        ModelNode address = subsystem.get(OP_ADDR);
        address.add(SUBSYSTEM, SecurityExtension.SUBSYSTEM_NAME);

        requireNoAttributes(reader);

        List<ModelNode> jaasUpdates = null;
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
                        case JAAS: {
                            jaasUpdates = parseJaas(reader, address);
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

        if (jaasUpdates != null) {
            list.addAll(jaasUpdates);
        }
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = context.getModelNode();

        if (isNonStandard(node, AUTHENTICATION_MANAGER_CLASS_NAME)
                || (node.hasDefined(DEEP_COPY_SUBJECT_MODE) && node.get(DEEP_COPY_SUBJECT_MODE).asBoolean())
                || isNonStandard(node, DEFAULT_CALLBACK_HANDLER_CLASS_NAME)) {
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
        }

        if (isNonStandard(node, SUBJECT_FACTORY_CLASS_NAME)) {
            writer.writeEmptyElement(Element.SUBJECT_FACTORY.getLocalName());
            writeAttribute(writer, Attribute.SUBJECT_FACTORY_CLASS_NAME,
                    node.get(SUBJECT_FACTORY_CLASS_NAME));
        }

        if (node.hasDefined(JAAS_APPLICATION_POLICY) && node.get(JAAS_APPLICATION_POLICY).asInt() > 0) {

            writer.writeStartElement(Element.JAAS.getLocalName());
            for (Property policy : node.get(JAAS_APPLICATION_POLICY).asPropertyList()) {
                writer.writeStartElement(Element.APPLICATION_POLICY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), policy.getName());
                ModelNode policyDetails = policy.getValue();
                if (policyDetails.hasDefined(Attribute.EXTENDS.getLocalName())) {
                    writer.writeAttribute(Attribute.EXTENDS.getLocalName(), policyDetails.get(Attribute.EXTENDS.getLocalName()).asString());
                }
                writeApplicationPolicyContent(writer, policyDetails);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    private void writeApplicationPolicyContent(XMLExtendedStreamWriter writer, ModelNode policyDetails) throws XMLStreamException {
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
                case AUTHORIZATION:
                case ACL:
                case AUDIT:
                case IDENTITY_TRUST:
                case MAPPING:
                case AUTHENTICATION_JASPI: {
                    throw new UnsupportedOperationException("NYI: full marshalling of application policy");
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

    private void writeCommonModule(XMLExtendedStreamWriter writer, ModelNode module) throws XMLStreamException {
        writer.writeAttribute(Attribute.CODE.getLocalName(), module.require(Attribute.CODE.getLocalName()).asString());
        writer.writeAttribute(Attribute.FLAG.getLocalName(), module.require(Attribute.FLAG.getLocalName()).asString());
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

    private void parseSecurityManagement(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {
        // read attributes
        String authenticationManagerClassName = null;
        boolean deepCopySubjectMode = false;
        String defaultCallbackHandlerClassName = null;
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
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);

        if (authenticationManagerClassName == null) {
            operation.get(AUTHENTICATION_MANAGER_CLASS_NAME).set(authenticationManagerClassName);
        }

        if (defaultCallbackHandlerClassName == null) {
            operation.get(DEFAULT_CALLBACK_HANDLER_CLASS_NAME).set(defaultCallbackHandlerClassName);
        }

        if (deepCopySubjectMode) {
            operation.get(DEEP_COPY_SUBJECT_MODE).set(deepCopySubjectMode);
        }
    }

    private void parseSubjectFactory(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {
        // read attributes
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

    private List<ModelNode> parseJaas(final XMLExtendedStreamReader reader, final ModelNode parentAddress) throws XMLStreamException {
        // no attributes
        requireNoAttributes(reader);

        List<ModelNode> list = new ArrayList<ModelNode>();

        final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (!visited.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    switch (element) {
                        case APPLICATION_POLICY: {
                            list.add(parseApplicationPolicy(reader, parentAddress));
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

    private ModelNode parseApplicationPolicy(XMLExtendedStreamReader reader, ModelNode parentAddress) throws XMLStreamException {

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
                    address.set(parentAddress).add(JAAS_APPLICATION_POLICY, value);
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
                            parseAuthentication(reader, op.get(Element.AUTHENTICATION.getLocalName()));
                            break;
                        }
                        case AUTHORIZATION: {
                            visited.remove(element); // this one is unbounded
                            // for now fall through to NYI exception
                        }
                        case ACL:
                        case AUDIT:
                        case IDENTITY_TRUST:
                        case MAPPING:
                        case AUTHENTICATION_JASPI: {
                            throw new UnsupportedOperationException("NYI: full parsing of application policy");
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

    private void parseAuthentication(XMLExtendedStreamReader reader, ModelNode op) throws XMLStreamException {

        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case LOGIN_MODULE: {
                            parseCommonModule(reader, op.add());
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

    private void parseCommonModule(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {

        EnumSet<Attribute> required = EnumSet.of(Attribute.CODE, Attribute.FLAG);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case CODE: {
                    node.get(Attribute.CODE.getLocalName()).set(value);
                    break;
                }
                case FLAG: {
                    // TODO validate
                    node.get(Attribute.FLAG.getLocalName()).set(value);
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

}