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
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.model.ParseUtils.unexpectedElement;
import static org.jboss.as.security.CommonAttributes.AUTHENTICATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.DEEP_COPY_SUBJECT_MODE;
import static org.jboss.as.security.CommonAttributes.DEFAULT_CALLBACK_HANDLER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.JAAS_APPLICATION_POLICY;
import static org.jboss.as.security.CommonAttributes.SUBJECT_FACTORY_CLASS_NAME;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The security extension
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class NewSecurityExtension implements NewExtension {

    public static final String SUBSYSTEM_NAME = "security";

    private static final SecuritySubsystemParser PARSER = new SecuritySubsystemParser();

    @Override
    public void initialize(NewExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(SecuritySubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, NewSecuritySubsystemAdd.INSTANCE, SecuritySubsystemProviders.SUBSYSTEM_ADD,
                false);
        subsystem.registerXMLElementWriter(PARSER);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), PARSER);
    }

    static class SecuritySubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
            XMLElementWriter<SubsystemMarshallingContext> {

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            ModelNode address = subsystem.get(OP_ADDR);
            address.add(SUBSYSTEM, SUBSYSTEM_NAME);

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
                throw new UnsupportedOperationException("Implement detyped jaas element marshalling");
            }

            writer.writeEndElement();
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
//            ApplicationPolicyParser parser = new ApplicationPolicyParser();
//            List<Ap>
//            AddJaasUpdate jaasUpdate = new AddJaasUpdate();
//            jaasUpdate.setApplicationPolicies(parser.parse(reader));
//            return jaasUpdate;
            throw new UnsupportedOperationException("Implement detyped jaas element parsing");
        }

    }

}
