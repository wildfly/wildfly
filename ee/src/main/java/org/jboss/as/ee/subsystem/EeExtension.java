/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ee.EeMessages.MESSAGES;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * JBossAS domain extension used to initialize the ee subsystem handlers and associated classes.
 *
 * @author Weston M. Price
 * @author Emanuel Muckenhuber
 */
public class EeExtension implements Extension {

    public static final String NAMESPACE = "urn:jboss:domain:ee:1.0";
    public static final String SUBSYSTEM_NAME = "ee";
    private static final EESubsystemParser parser = new EESubsystemParser();
    private static final String RESOURCE_NAME = EeExtension.class.getPackage().getName() + ".LocalDescriptions";

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, EeExtension.class.getClassLoader(), true, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);

        // Register the root subsystem resource.
        final ManagementResourceRegistration rootResource = subsystem.registerSubsystemModel(EeSubsystemRootResource.INSTANCE);

        // Mandatory describe operation
        rootResource.registerOperationHandler(DESCRIBE, EESubsystemDescribeHandler.INSTANCE, EESubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        subsystem.registerXMLElementWriter(parser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE, parser);
    }

    static ModelNode createEESubSystemAddOperation() {
        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
        return subsystem;
    }


    static class EESubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            //TODO seems to be a problem with empty elements cleaning up the queue in FormattingXMLStreamWriter.runAttrQueue
            //context.startSubsystemElement(NewEeExtension.NAMESPACE, true);
            context.startSubsystemElement(EeExtension.NAMESPACE, false);

            ModelNode eeSubSystem = context.getModelNode();
            EeSubsystemRootResource.EAR_SUBDEPLOYMENTS_ISOLATED.marshallAsElement(eeSubSystem, writer);
            GlobalModulesDefinition.INSTANCE.marshallAsElement(eeSubSystem, writer);

            writer.writeEndElement();

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            // EE subsystem doesn't have any attributes, so make sure that the xml doesn't have any
            requireNoAttributes(reader);

            final ModelNode eeSubSystem = createEESubSystemAddOperation();
            // add the subsystem to the ModelNode(s)
            list.add(eeSubSystem);

            // elements
            final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case EE_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        if (!encountered.add(element)) {
                            throw unexpectedElement(reader);
                        }
                        switch (element) {
                            case GLOBAL_MODULES: {
                                final ModelNode model = parseGlobalModules(reader);
                                eeSubSystem.get(GlobalModulesDefinition.GLOBAL_MODULES).set(model);
                                break;
                            }
                            case EAR_SUBDEPLOYMENTS_ISOLATED: {
                                final String earSubDeploymentsIsolated = parseEarSubDeploymentsIsolatedElement(reader);
                                // set the ear subdeployment isolation on the subsystem operation
                                EeSubsystemRootResource.EAR_SUBDEPLOYMENTS_ISOLATED.parseAndSetParameter(earSubDeploymentsIsolated, eeSubSystem, reader);
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

        static ModelNode parseGlobalModules(XMLExtendedStreamReader reader) throws XMLStreamException {

            ModelNode globalModules = new ModelNode();

            requireNoAttributes(reader);

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Element.forName(reader.getLocalName())) {
                    case MODULE: {
                        final int count = reader.getAttributeCount();
                        String name = null;
                        String slot = null;
                        for (int i = 0; i < count; i++) {
                            requireNoNamespaceAttribute(reader, i);
                            final String value = reader.getAttributeValue(i);
                            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                            switch (attribute) {
                                case NAME:
                                    if (name != null) {
                                        throw unexpectedAttribute(reader, i);
                                    }
                                    name = value;
                                    break;
                                case SLOT:
                                    if (slot != null) {
                                        throw unexpectedAttribute(reader, i);
                                    }
                                    slot = value;
                                    break;
                                default:
                                    throw unexpectedAttribute(reader, i);
                            }
                        }
                        if (name == null) {
                            throw missingRequired(reader, Collections.singleton(NAME));
                        }
                        if (slot == null) {
                            slot = "main";
                        }
                        final ModelNode module = new ModelNode();
                        module.get(GlobalModulesDefinition.NAME).set(name);
                        module.get(GlobalModulesDefinition.SLOT).set(slot);
                        globalModules.add(module);
                        requireNoContent(reader);
                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);
                    }
                }
            }
            return globalModules;
        }

        static String parseEarSubDeploymentsIsolatedElement(XMLExtendedStreamReader reader) throws XMLStreamException {

            // we don't expect any attributes for this element.
            requireNoAttributes(reader);

            final String value = reader.getElementText();
            if (value == null || value.trim().isEmpty()) {
                throw MESSAGES.invalidValue(value, Element.EAR_SUBDEPLOYMENTS_ISOLATED.getLocalName(), reader.getLocation());
            }
            return value.trim();
        }
    }

    private static class EESubsystemDescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final EESubsystemDescribeHandler INSTANCE = new EESubsystemDescribeHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode model = Resource.Tools.readModel(resource);
            final ModelNode op = createEESubSystemAddOperation();
            if (model.hasDefined(GlobalModulesDefinition.GLOBAL_MODULES)) {
                op.get(GlobalModulesDefinition.GLOBAL_MODULES).set(model.get(GlobalModulesDefinition.GLOBAL_MODULES));
            }
            if (model.hasDefined(Element.EAR_SUBDEPLOYMENTS_ISOLATED.getLocalName())) {
                op.get(Element.EAR_SUBDEPLOYMENTS_ISOLATED.getLocalName()).set(model.get(Element.EAR_SUBDEPLOYMENTS_ISOLATED.getLocalName()));
            }
            context.getResult().add(op);
            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }

}
