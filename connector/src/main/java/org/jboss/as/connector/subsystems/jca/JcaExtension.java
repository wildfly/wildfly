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
package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;
import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION;
import static org.jboss.as.connector.subsystems.jca.Constants.BEAN_VALIDATION;
import static org.jboss.as.connector.subsystems.jca.Constants.BOOTSTRAP_CONTEXT;
import static org.jboss.as.connector.subsystems.jca.Constants.CACHED_CONNECTION_MANAGER;
import static org.jboss.as.connector.subsystems.jca.Constants.DEFAULT_NAME;
import static org.jboss.as.connector.subsystems.jca.Constants.DISTRIBUTED_WORKMANAGER;
import static org.jboss.as.connector.subsystems.jca.Constants.JCA;
import static org.jboss.as.connector.subsystems.jca.Constants.TRACER;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_LONG_RUNNING;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_SHORT_RUNNING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.connector.subsystems.jca.JcaArchiveValidationDefinition.ArchiveValidationParameters;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.threads.ThreadsParser;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class JcaExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jca";

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(5, 0, 0);

    private static final String RESOURCE_NAME = JcaExtension.class.getPackage().getName() + ".LocalDescriptions";


    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, JcaExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initialize(final ExtensionContext context) {
        ROOT_LOGGER.debugf("Initializing Connector Extension");

        final boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        subsystem.registerSubsystemModel(JcaSubsystemRootDefinition.createInstance(registerRuntimeOnly));

        subsystem.registerXMLElementWriter(ConnectorSubsystemParser.INSTANCE);
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JCA_1_1.getUriString(), () -> ConnectorSubsystemParser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JCA_2_0.getUriString(), () -> ConnectorSubsystemParser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JCA_3_0.getUriString(), () -> ConnectorSubsystemParser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JCA_4_0.getUriString(), () -> ConnectorSubsystemParser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JCA_5_0.getUriString(), () -> ConnectorSubsystemParser.INSTANCE);
    }

    static final class ConnectorSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
            XMLElementWriter<SubsystemMarshallingContext> {

        static final ConnectorSubsystemParser INSTANCE = new ConnectorSubsystemParser();

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
            ModelNode node = context.getModelNode();

            writeArchiveValidation(writer, node);
            writeBeanValidation(writer, node);
            writeTracer(writer, node);
            writeWorkManagers(writer, node);
            writeDistributedWorkManagers(writer, node);
            writeBootstrapContexts(writer, node);
            writeCachedConnectionManager(writer, node);
            writer.writeEndElement();
        }

        private void writeArchiveValidation(XMLExtendedStreamWriter writer, ModelNode parentNode) throws XMLStreamException {
            if (parentNode.hasDefined(ARCHIVE_VALIDATION)) {
                ModelNode node = parentNode.get(ARCHIVE_VALIDATION).get(ARCHIVE_VALIDATION);
                if (ArchiveValidationParameters.ARCHIVE_VALIDATION_ENABLED.getAttribute().isMarshallable(node) ||
                        ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_ERROR.getAttribute().isMarshallable(node) ||
                        ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_WARN.getAttribute().isMarshallable(node)) {
                    writer.writeEmptyElement(Element.ARCHIVE_VALIDATION.getLocalName());
                    ArchiveValidationParameters.ARCHIVE_VALIDATION_ENABLED.getAttribute().marshallAsAttribute(node, writer);
                    ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_ERROR.getAttribute().marshallAsAttribute(node, writer);
                    ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_WARN.getAttribute().marshallAsAttribute(node, writer);

                }
            }
        }

        private void writeBeanValidation(XMLExtendedStreamWriter writer, ModelNode parentNode) throws XMLStreamException {
            if (parentNode.hasDefined(BEAN_VALIDATION)) {
                ModelNode node = parentNode.get(BEAN_VALIDATION).get(BEAN_VALIDATION);

                if (JcaBeanValidationDefinition.BeanValidationParameters.BEAN_VALIDATION_ENABLED.getAttribute().isMarshallable(node)) {
                    writer.writeEmptyElement(Element.BEAN_VALIDATION.getLocalName());
                    JcaBeanValidationDefinition.BeanValidationParameters.BEAN_VALIDATION_ENABLED.getAttribute().marshallAsAttribute(node, writer);
                }
            }
        }

        private void writeTracer(XMLExtendedStreamWriter writer, ModelNode parentNode) throws XMLStreamException {
            if (parentNode.hasDefined(TRACER)) {
                ModelNode node = parentNode.get(TRACER).get(TRACER);

                if (TracerDefinition.TracerParameters.TRACER_ENABLED.getAttribute().isMarshallable(node)) {
                    writer.writeEmptyElement(Element.TRACER.getLocalName());
                    TracerDefinition.TracerParameters.TRACER_ENABLED.getAttribute().marshallAsAttribute(node, writer);
                }
            }
        }

        private void writeCachedConnectionManager(XMLExtendedStreamWriter writer, ModelNode parentNode) throws XMLStreamException {
            if (parentNode.hasDefined(CACHED_CONNECTION_MANAGER)) {
                ModelNode node = parentNode.get(CACHED_CONNECTION_MANAGER).get(CACHED_CONNECTION_MANAGER);

                final String name = JcaCachedConnectionManagerDefinition.CcmParameters.INSTALL.getAttribute().getName();
                if (node.hasDefined(name) &&
                        node.get(name).asBoolean()) {
                    writer.writeEmptyElement(Element.CACHED_CONNECTION_MANAGER.getLocalName());
                    JcaCachedConnectionManagerDefinition.CcmParameters.DEBUG.getAttribute().marshallAsAttribute(node, writer);
                    JcaCachedConnectionManagerDefinition.CcmParameters.ERROR.getAttribute().marshallAsAttribute(node, writer);
                    JcaCachedConnectionManagerDefinition.CcmParameters.IGNORE_UNKNOWN_CONNECTIONS.getAttribute().marshallAsAttribute(node, writer);
                }
            }
        }

        private void writeDistributedWorkManagers(XMLExtendedStreamWriter writer, ModelNode parentNode) throws XMLStreamException {
            if (parentNode.hasDefined(DISTRIBUTED_WORKMANAGER) && parentNode.get(DISTRIBUTED_WORKMANAGER).asList().size() != 0) {
                ModelNode workManagers = parentNode.get(DISTRIBUTED_WORKMANAGER);
                for (String name : workManagers.keys()) {
                    ModelNode workManager = workManagers.get(name);
                    writer.writeStartElement(Element.DISTRIBUTED_WORKMANAGER.getLocalName());
                    ((SimpleAttributeDefinition) JcaDistributedWorkManagerDefinition.DWmParameters.NAME.getAttribute()).marshallAsAttribute(workManager, writer);

                    JcaDistributedWorkManagerDefinition.DWmParameters.ELYTRON_ENABLED.getAttribute().marshallAsElement(workManager, writer);

                    for (Property prop : workManager.asPropertyList()) {
                        if (WORKMANAGER_LONG_RUNNING.equals(prop.getName()) && prop.getValue().isDefined() && prop.getValue().asPropertyList().size() != 0) {
                            ThreadsParser.getInstance().writeBoundedQueueThreadPool(writer, prop.getValue().asProperty(), Element.LONG_RUNNING_THREADS.getLocalName(), false);
                        }
                        if (WORKMANAGER_SHORT_RUNNING.equals(prop.getName()) && prop.getValue().isDefined() && prop.getValue().asPropertyList().size() != 0) {
                            ThreadsParser.getInstance().writeBoundedQueueThreadPool(writer, prop.getValue().asProperty(), Element.SHORT_RUNNING_THREADS.getLocalName(), false);
                        }

                        if ((JcaDistributedWorkManagerDefinition.DWmParameters.POLICY.getAttribute().getName().equals(prop.getName()) && prop.getValue().isDefined()) ||
                                (JcaDistributedWorkManagerDefinition.DWmParameters.POLICY.getAttribute().getName().equals(prop.getName()) && workManager.hasDefined(JcaDistributedWorkManagerDefinition.DWmParameters.POLICY_OPTIONS.getAttribute().getName()))) {
                            writer.writeStartElement(Element.POLICY.getLocalName());
                            if (prop.getValue().isDefined() )
                                writer.writeAttribute(JcaDistributedWorkManagerDefinition.DWmParameters.NAME.getAttribute().getXmlName(), prop.getValue().asString());
                            else
                                writer.writeAttribute(JcaDistributedWorkManagerDefinition.DWmParameters.NAME.getAttribute().getXmlName(), JcaDistributedWorkManagerDefinition.DWmParameters.POLICY.getAttribute().getDefaultValue().asString());
                            if (workManager.hasDefined(JcaDistributedWorkManagerDefinition.DWmParameters.POLICY_OPTIONS.getAttribute().getName())) {
                                for (Property option : workManager.get(JcaDistributedWorkManagerDefinition.DWmParameters.POLICY_OPTIONS.getAttribute().getName()).asPropertyList()) {
                                    writeProperty(writer, option.getName(), option
                                            .getValue().asString(), Element.OPTION.getLocalName());
                                }
                            }
                            writer.writeEndElement();
                        }

                        if (JcaDistributedWorkManagerDefinition.DWmParameters.SELECTOR.getAttribute().getName().equals(prop.getName()) && prop.getValue().isDefined()) {
                            writer.writeStartElement(Element.SELECTOR.getLocalName());
                            writer.writeAttribute(JcaDistributedWorkManagerDefinition.DWmParameters.NAME.getAttribute().getXmlName(), prop.getValue().asString());

                            if (workManager.hasDefined(JcaDistributedWorkManagerDefinition.DWmParameters.SELECTOR_OPTIONS.getAttribute().getName())) {
                                for (Property option : workManager.get(JcaDistributedWorkManagerDefinition.DWmParameters.SELECTOR_OPTIONS.getAttribute().getName()).asPropertyList()) {
                                    writeProperty(writer, option.getName(), option
                                            .getValue().asString(), Element.OPTION.getLocalName());
                                }
                            }
                            writer.writeEndElement();
                        }

                    }

                    writer.writeEndElement();
                }
            }
        }

        private void writeWorkManagers(XMLExtendedStreamWriter writer, ModelNode parentNode) throws XMLStreamException {
            List<Property> workManagers;
            if (parentNode.hasDefined(WORKMANAGER) && (workManagers = parentNode.get(WORKMANAGER).asPropertyList()).size() != 0) {
                List<ModelNode> defaultFirst = new ArrayList<>();
                for (Property prop : workManagers) {
                    ModelNode workManager = prop.getValue();
                    if ("default".equals(workManager.get(NAME).asString())) {
                        defaultFirst.add(0, workManager);
                    } else {
                        defaultFirst.add(workManager);
                    }
                }
                for (int i = 0; i < defaultFirst.size(); i++) {
                    ModelNode workManager = defaultFirst.get(i);
                    if (i == 0 && "default".equals(workManager.get(NAME).asString())) {
                        writer.writeStartElement(Element.DEFAULT_WORKMANAGER.getLocalName());
                    } else {
                        writer.writeStartElement(Element.WORKMANAGER.getLocalName());
                        JcaWorkManagerDefinition.WmParameters.NAME.getAttribute().marshallAsAttribute(workManager, writer);
                    }

                    JcaWorkManagerDefinition.WmParameters.ELYTRON_ENABLED.getAttribute().marshallAsElement(workManager, writer);

                    if (workManager.hasDefined(WORKMANAGER_SHORT_RUNNING))  {
                        ThreadsParser.getInstance().writeBoundedQueueThreadPool(writer, workManager.get(WORKMANAGER_SHORT_RUNNING).asProperty(), Element.SHORT_RUNNING_THREADS.getLocalName(), false);
                    }
                    if (workManager.hasDefined(WORKMANAGER_LONG_RUNNING)) {
                        ThreadsParser.getInstance().writeBoundedQueueThreadPool(writer, workManager.get(WORKMANAGER_LONG_RUNNING).asProperty(), Element.LONG_RUNNING_THREADS.getLocalName(), false);
                    }
                    writer.writeEndElement();
                }
            }
        }


        private void writeBootstrapContexts(XMLExtendedStreamWriter writer, ModelNode parentNode) throws XMLStreamException {
            if (parentNode.hasDefined(BOOTSTRAP_CONTEXT) && parentNode.get(BOOTSTRAP_CONTEXT).asList().size() != 0) {

                boolean started = false;

                ModelNode contexts = parentNode.get(BOOTSTRAP_CONTEXT);
                for (String name : contexts.keys()) {
                    ModelNode context = contexts.get(name);
                    if (!context.get(JcaBootstrapContextDefinition.BootstrapCtxParameters.NAME.getAttribute().getName()).asString().equals(DEFAULT_NAME) &&
                            (JcaBootstrapContextDefinition.BootstrapCtxParameters.NAME.getAttribute().isMarshallable(context) ||
                                    JcaBootstrapContextDefinition.BootstrapCtxParameters.WORKMANAGER.getAttribute().isMarshallable(context))) {
                        if (!started) {
                            writer.writeStartElement(Element.BOOTSTRAP_CONTEXTS.getLocalName());
                            started = true;
                        }
                        writer.writeStartElement(Element.BOOTSTRAP_CONTEXT.getLocalName());
                        JcaBootstrapContextDefinition.BootstrapCtxParameters.NAME.getAttribute().marshallAsAttribute(context, writer);
                        JcaBootstrapContextDefinition.BootstrapCtxParameters.WORKMANAGER.getAttribute().marshallAsAttribute(context, writer);
                        writer.writeEndElement();
                    }
                }
                if (started) {
                    writer.writeEndElement();
                }
            }
        }

        @Override
        public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {

            final ModelNode address = new ModelNode();
            address.add(org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM, JCA);
            address.protect();

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(address);
            list.add(subsystem);

            // Handle elements
            final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
            final EnumSet<Element> requiredElement = EnumSet.of(Element.DEFAULT_WORKMANAGER);
            boolean ccmAdded = false;
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {

                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case JCA_5_0:
                    case JCA_4_0:
                    case JCA_3_0:
                    case JCA_2_0:
                    case JCA_1_1: {
                        final Element element = Element.forName(reader.getLocalName());
                        if (!visited.add(element)) {
                            throw unexpectedElement(reader);
                        }

                        switch (element) {
                            case ARCHIVE_VALIDATION: {
                                list.add(parseArchiveValidation(reader, address));
                                break;
                            }
                            case BEAN_VALIDATION: {
                                list.add(parseBeanValidation(reader, address));
                                break;
                            }
                            case DEFAULT_WORKMANAGER: {
                                parseWorkManager(reader, address, list, subsystem, true);
                                final ModelNode bootstrapContextOperation = new ModelNode();
                                bootstrapContextOperation.get(OP).set(ADD);
                                final ModelNode bootStrapCOntextAddress = address.clone();
                                bootStrapCOntextAddress.add(BOOTSTRAP_CONTEXT, DEFAULT_NAME);
                                bootStrapCOntextAddress.protect();

                                bootstrapContextOperation.get(OP_ADDR).set(bootStrapCOntextAddress);
                                bootstrapContextOperation.get(WORKMANAGER).set(DEFAULT_NAME);
                                bootstrapContextOperation.get(NAME).set(DEFAULT_NAME);
                                list.add(bootstrapContextOperation);

                                requiredElement.remove(Element.DEFAULT_WORKMANAGER);

                                break;
                            }
                            case CACHED_CONNECTION_MANAGER: {
                                list.add(parseCcm(reader, address));
                                ccmAdded = true;
                                break;
                            }
                            case WORKMANAGER: {
                                parseWorkManager(reader, address, list, subsystem, false);
                                // AS7-4434 Multiple work managers are allowed
                                visited.remove(Element.WORKMANAGER);
                                break;
                            }
                            case DISTRIBUTED_WORKMANAGER: {
                                parseDistributedWorkManager(reader, address, list, subsystem, false);
                                // AS7-4434 Multiple work managers are allowed
                                visited.remove(Element.DISTRIBUTED_WORKMANAGER);
                                break;
                            }
                            case BOOTSTRAP_CONTEXTS: {
                                parseBootstrapContexts(reader, address, list);
                                break;
                            }
                            case TRACER: {
                                if (Namespace.forUri(reader.getNamespaceURI()).equals(Namespace.JCA_3_0) ||
                                    Namespace.forUri(reader.getNamespaceURI()).equals(Namespace.JCA_4_0) ||
                                    Namespace.forUri(reader.getNamespaceURI()).equals(Namespace.JCA_5_0)) {
                                    list.add(parseTracer(reader, address));
                                } else {
                                    throw unexpectedElement(reader);
                                }
                                break;
                            }
                            default:
                                throw unexpectedElement(reader);
                        }
                        break;
                    }
                    default:
                        throw unexpectedElement(reader);
                }
            }
            if (!requiredElement.isEmpty()) {
                throw missingRequiredElement(reader, requiredElement);
            }
            if (!ccmAdded) {
                final ModelNode ccmOperation = new ModelNode();
                ccmOperation.get(OP).set(ADD);

                final ModelNode ccmAddress = address.clone();
                ccmAddress.add(CACHED_CONNECTION_MANAGER, CACHED_CONNECTION_MANAGER);
                ccmAddress.protect();

                ccmOperation.get(OP_ADDR).set(ccmAddress);
                assert ccmOperation.keys().size() == 2; // prevent people adding params without considering special
                                                        // WFLY-2640/WFLY-8141 logic. This assert can be changed once
                                                        // you've made the necessary adjustments

                list.add(ccmOperation);
            }
        }

        private ModelNode parseArchiveValidation(final XMLExtendedStreamReader reader, final ModelNode parentOperation)
                throws XMLStreamException {
            final ModelNode archiveValidationOperation = new ModelNode();
            archiveValidationOperation.get(OP).set(ADD);

            final ModelNode archiveValidationAddress = parentOperation.clone();
            archiveValidationAddress.add(ARCHIVE_VALIDATION, ARCHIVE_VALIDATION);
            archiveValidationAddress.protect();

            archiveValidationOperation.get(OP_ADDR).set(archiveValidationAddress);


            final int cnt = reader.getAttributeCount();
            for (int i = 0; i < cnt; i++) {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case ENABLED: {
                        String value = rawAttributeText(reader, ArchiveValidationParameters.ARCHIVE_VALIDATION_ENABLED.getAttribute().getXmlName());
                        ArchiveValidationParameters.ARCHIVE_VALIDATION_ENABLED.getAttribute().parseAndSetParameter(value, archiveValidationOperation, reader);
                        break;
                    }
                    case FAIL_ON_ERROR: {
                        String value = rawAttributeText(reader, ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_ERROR.getAttribute().getXmlName());
                        ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_ERROR.getAttribute().parseAndSetParameter(value, archiveValidationOperation, reader);
                        break;
                    }
                    case FAIL_ON_WARN: {
                        String value = rawAttributeText(reader, ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_WARN.getAttribute().getXmlName());
                        ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_WARN.getAttribute().parseAndSetParameter(value, archiveValidationOperation, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
            // Handle elements
            requireNoContent(reader);

            return archiveValidationOperation;

        }

        private void parseWorkManager(final XMLExtendedStreamReader reader, final ModelNode parentAddress,
                                      final List<ModelNode> list, final ModelNode node, boolean defaultWm) throws XMLStreamException {

            final ModelNode workManagerOperation = new ModelNode();
            workManagerOperation.get(OP).set(ADD);

            final int cnt = reader.getAttributeCount();
            String name = null;
            for (int i = 0; i < cnt; i++) {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = rawAttributeText(reader, JcaWorkManagerDefinition.WmParameters.NAME.getAttribute().getXmlName());
                        JcaWorkManagerDefinition.WmParameters.NAME.getAttribute().parseAndSetParameter(name, workManagerOperation, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }

            if (name == null) {
                if (defaultWm) {
                    name = DEFAULT_NAME;
                    workManagerOperation.get(NAME).set(name);
                } else {
                    throw new XMLStreamException("name attribute is mandatory for workmanager element");
                }
            }

            final ModelNode workManagerAddress = parentAddress.clone();
            workManagerAddress.add(WORKMANAGER, name);
            workManagerAddress.protect();

            workManagerOperation.get(OP_ADDR).set(workManagerAddress);
            list.add(workManagerOperation);

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {

                final Element element = Element.forName(reader.getLocalName());
                Namespace readerNS = Namespace.forUri(reader.getNamespaceURI());
                switch (element) {
                    case LONG_RUNNING_THREADS: {

                        org.jboss.as.threads.Namespace ns = org.jboss.as.threads.Namespace.THREADS_1_1;
                        ThreadsParser.getInstance().parseBlockingBoundedQueueThreadPool(reader, readerNS.getUriString(),
                                ns, workManagerAddress, list, WORKMANAGER_LONG_RUNNING, name);
                        break;
                    }
                    case SHORT_RUNNING_THREADS: {

                        org.jboss.as.threads.Namespace ns = org.jboss.as.threads.Namespace.THREADS_1_1;
                        ThreadsParser.getInstance().parseBlockingBoundedQueueThreadPool(reader, readerNS.getUriString(),
                                ns, workManagerAddress, list, WORKMANAGER_SHORT_RUNNING, name);

                        break;
                    }
                    case ELYTRON_ENABLED: {
                        switch (readerNS) {
                            case JCA_5_0: {
                                String value = rawElementText(reader);
                                JcaWorkManagerDefinition.WmParameters.ELYTRON_ENABLED.getAttribute().parseAndSetParameter(value, workManagerOperation, reader);
                                break;
                            }
                            default: {
                                throw unexpectedElement(reader);
                            }
                        }
                        break;
                    }
                    default:
                        throw unexpectedElement(reader);
                }


            }

        }

        private void parseDistributedWorkManager(final XMLExtendedStreamReader reader, final ModelNode parentAddress,
                                                 final List<ModelNode> list, final ModelNode node, boolean defaultWm) throws XMLStreamException {

            final ModelNode distributedWorkManagerOperation = new ModelNode();
            distributedWorkManagerOperation.get(OP).set(ADD);

            final int cnt = reader.getAttributeCount();
            String name = null;
            final AttributeDefinition attributeDefinition = JcaDistributedWorkManagerDefinition.DWmParameters.NAME.getAttribute();
            final String attributeName = attributeDefinition.getXmlName();
            for (int i = 0; i < cnt; i++) {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = rawAttributeText(reader, attributeName);
                        ((SimpleAttributeDefinition) attributeDefinition).parseAndSetParameter(name, distributedWorkManagerOperation, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }

            if (name == null) {
                throw ParseUtils.missingRequired(reader, attributeName);
            }

            final ModelNode distributedWorkManagerAddress = parentAddress.clone();
            distributedWorkManagerAddress.add(DISTRIBUTED_WORKMANAGER, name);
            distributedWorkManagerAddress.protect();

            distributedWorkManagerOperation.get(OP_ADDR).set(distributedWorkManagerAddress);
            list.add(distributedWorkManagerOperation);


            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {

                final Element element = Element.forName(reader.getLocalName());
                Namespace readerNS = Namespace.forUri(reader.getNamespaceURI());
                switch (element) {
                    case LONG_RUNNING_THREADS: {

                        org.jboss.as.threads.Namespace ns = org.jboss.as.threads.Namespace.THREADS_1_1;
                        ThreadsParser.getInstance().parseBlockingBoundedQueueThreadPool(reader, readerNS.getUriString(),
                                ns, distributedWorkManagerAddress, list, WORKMANAGER_LONG_RUNNING, name);
                        break;
                    }
                    case SHORT_RUNNING_THREADS: {
                        org.jboss.as.threads.Namespace ns = org.jboss.as.threads.Namespace.THREADS_1_1;
                        ThreadsParser.getInstance().parseBlockingBoundedQueueThreadPool(reader, readerNS.getUriString(),
                                ns, distributedWorkManagerAddress, list, WORKMANAGER_SHORT_RUNNING, name);
                        break;
                    }
                    case POLICY: {
                        switch (readerNS) {
                            case JCA_2_0:
                            case JCA_3_0:
                            case JCA_4_0:
                            case JCA_5_0: {
                                parsePolicy(reader, distributedWorkManagerOperation);
                                break;
                            }
                            default: {
                                throw unexpectedElement(reader);
                            }
                        }
                        break;
                    }
                    case SELECTOR: {
                        switch (readerNS) {
                            case JCA_2_0:
                            case JCA_3_0:
                            case JCA_4_0:
                            case JCA_5_0: {
                                parseSelector(reader, distributedWorkManagerOperation);
                                break;
                            }
                            default: {
                                throw unexpectedElement(reader);
                            }
                        }
                        break;
                    }
                    case ELYTRON_ENABLED: {
                        switch (readerNS) {
                            case JCA_5_0: {
                                String value = rawElementText(reader);
                                ((SimpleAttributeDefinition) JcaDistributedWorkManagerDefinition.DWmParameters.ELYTRON_ENABLED.getAttribute()).parseAndSetParameter(value, distributedWorkManagerOperation, reader);
                                break;
                            }
                            default: {
                                throw unexpectedElement(reader);
                            }
                        }
                        break;
                    }
                    default:
                        throw unexpectedElement(reader);
                }


            }

        }


        private void parsePolicy(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {


            final int cnt = reader.getAttributeCount();

            for (int i = 0; i < cnt; i++) {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        String policy = rawAttributeText(reader, attribute.getLocalName());
                        ((SimpleAttributeDefinition) JcaDistributedWorkManagerDefinition.DWmParameters.POLICY.getAttribute()).parseAndSetParameter(policy, operation, reader);
                        break;
                    }

                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {

                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case OPTION: {
                        requireSingleAttribute(reader, "name");
                        final String name = rawAttributeText(reader, "name");
                        String value = rawElementText(reader);
                        final String trimmed = value == null ? null : value.trim();
                        ((PropertiesAttributeDefinition) JcaDistributedWorkManagerDefinition.DWmParameters.POLICY_OPTIONS.getAttribute()).parseAndAddParameterElement(name, trimmed, operation, reader);
                        break;
                    }
                }
                // Handle elements

            }
        }

        private void parseSelector(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {


            final int cnt = reader.getAttributeCount();

            for (int i = 0; i < cnt; i++) {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {

                    case NAME: {
                        String selector = rawAttributeText(reader, attribute.getLocalName());
                        ((SimpleAttributeDefinition) JcaDistributedWorkManagerDefinition.DWmParameters.SELECTOR.getAttribute()).parseAndSetParameter(selector, operation, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }

            // Handle elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {

                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case OPTION: {
                        requireSingleAttribute(reader, "name");
                        final String name = rawAttributeText(reader, "name");
                        String value = rawElementText(reader);
                        final String trimmed = value == null ? null : value.trim();
                        ((PropertiesAttributeDefinition) JcaDistributedWorkManagerDefinition.DWmParameters.SELECTOR_OPTIONS.getAttribute()).parseAndAddParameterElement(name, trimmed, operation, reader);
                        break;
                    }
                }
                // Handle elements

            }
        }

        private ModelNode parseBeanValidation(final XMLExtendedStreamReader reader, final ModelNode parentOperation) throws XMLStreamException {
            final ModelNode beanValidationOperation = new ModelNode();
            beanValidationOperation.get(OP).set(ADD);

            final ModelNode beanValidationAddress = parentOperation.clone();
            beanValidationAddress.add(BEAN_VALIDATION, BEAN_VALIDATION);
            beanValidationAddress.protect();

            beanValidationOperation.get(OP_ADDR).set(beanValidationAddress);


            final int cnt = reader.getAttributeCount();
            for (int i = 0; i < cnt; i++) {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case ENABLED: {
                        String value = rawAttributeText(reader, JcaBeanValidationDefinition.BeanValidationParameters.BEAN_VALIDATION_ENABLED.getAttribute().getXmlName());
                        JcaBeanValidationDefinition.BeanValidationParameters.BEAN_VALIDATION_ENABLED.getAttribute().parseAndSetParameter(value, beanValidationOperation, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
            // Handle elements
            requireNoContent(reader);

            return beanValidationOperation;

        }

        private ModelNode parseTracer(final XMLExtendedStreamReader reader, final ModelNode parentOperation) throws XMLStreamException {
            final ModelNode tracerOperation = new ModelNode();
            tracerOperation.get(OP).set(ADD);

            final ModelNode tracerAddress = parentOperation.clone();
            tracerAddress.add(TRACER, TRACER);
            tracerAddress.protect();

            tracerOperation.get(OP_ADDR).set(tracerAddress);


            final int cnt = reader.getAttributeCount();
            for (int i = 0; i < cnt; i++) {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case ENABLED: {
                        String value = rawAttributeText(reader, TracerDefinition.TracerParameters.TRACER_ENABLED.getAttribute().getXmlName());
                        TracerDefinition.TracerParameters.TRACER_ENABLED.getAttribute().parseAndSetParameter(value, tracerOperation, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
            // Handle elements
            requireNoContent(reader);

            return tracerOperation;

        }

        private ModelNode parseCcm(final XMLExtendedStreamReader reader, final ModelNode parentOperation) throws XMLStreamException {
            final ModelNode ccmOperation = new ModelNode();
            ccmOperation.get(OP).set(ADD);

            final ModelNode ccmAddress = parentOperation.clone();
            ccmAddress.add(CACHED_CONNECTION_MANAGER, CACHED_CONNECTION_MANAGER);
            ccmAddress.protect();

            ccmOperation.get(OP_ADDR).set(ccmAddress);


            final int cnt = reader.getAttributeCount();
            for (int i = 0; i < cnt; i++) {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case DEBUG: {
                        String value = rawAttributeText(reader, JcaCachedConnectionManagerDefinition.CcmParameters.DEBUG.getAttribute().getXmlName());
                        JcaCachedConnectionManagerDefinition.CcmParameters.DEBUG.getAttribute().parseAndSetParameter(value, ccmOperation, reader);
                        break;
                    }
                    case ERROR: {
                        String value = rawAttributeText(reader, JcaCachedConnectionManagerDefinition.CcmParameters.ERROR.getAttribute().getXmlName());
                        JcaCachedConnectionManagerDefinition.CcmParameters.ERROR.getAttribute().parseAndSetParameter(value, ccmOperation, reader);
                        break;
                    }
                    case IGNORE_UNKNOWN_CONNECHIONS: {
                        String value = rawAttributeText(reader, JcaCachedConnectionManagerDefinition.CcmParameters.IGNORE_UNKNOWN_CONNECTIONS.getAttribute().getXmlName());
                        JcaCachedConnectionManagerDefinition.CcmParameters.IGNORE_UNKNOWN_CONNECTIONS.getAttribute().parseAndSetParameter(value, ccmOperation, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
            ccmOperation.get(JcaCachedConnectionManagerDefinition.CcmParameters.INSTALL.getAttribute().getName()).set(true);
            // Handle elements
            requireNoContent(reader);

            return ccmOperation;

        }

        private void parseBootstrapContexts(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final List<ModelNode> list) throws XMLStreamException {
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {

                final Element element = Element.forName(reader.getLocalName());

                switch (element) {
                    case BOOTSTRAP_CONTEXT: {
                        ModelNode bootstrapContextOperation = new ModelNode();
                        bootstrapContextOperation.get(OP).set(ADD);

                        final int cnt = reader.getAttributeCount();
                        String name = null;
                        String wmName = null;
                        for (int i = 0; i < cnt; i++) {
                            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                            switch (attribute) {
                                case NAME: {
                                    name = rawAttributeText(reader, JcaBootstrapContextDefinition.BootstrapCtxParameters.NAME.getAttribute().getXmlName());
                                    JcaBootstrapContextDefinition.BootstrapCtxParameters.NAME.getAttribute().parseAndSetParameter(name, bootstrapContextOperation, reader);
                                    break;
                                }
                                case WORKMANAGER: {
                                    wmName = rawAttributeText(reader, JcaBootstrapContextDefinition.BootstrapCtxParameters.WORKMANAGER.getAttribute().getXmlName());
                                    JcaBootstrapContextDefinition.BootstrapCtxParameters.WORKMANAGER.getAttribute().parseAndSetParameter(wmName, bootstrapContextOperation, reader);
                                    break;
                                }
                                default: {
                                    throw unexpectedAttribute(reader, i);
                                }
                            }
                        }

                        if (name == null) {
                            if (DEFAULT_NAME.equals(wmName)) {
                                name = DEFAULT_NAME;
                            } else {
                                throw new XMLStreamException("name attribute is mandatory for workmanager element");
                            }
                        }

                        final ModelNode bootstrapContextAddress = parentAddress.clone();
                        bootstrapContextAddress.add(BOOTSTRAP_CONTEXT, name);
                        bootstrapContextAddress.protect();

                        bootstrapContextOperation.get(OP_ADDR).set(bootstrapContextAddress);

                        // Handle elements
                        requireNoContent(reader);

                        list.add(bootstrapContextOperation);

                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);

                    }
                }
            }
        }

        public String rawAttributeText(XMLStreamReader reader, String attributeName) {
            String attributeString = reader.getAttributeValue("", attributeName) == null ? null : reader.getAttributeValue(
                    "", attributeName)
                    .trim();
            return attributeString;
        }


        public String rawElementText(XMLStreamReader reader) throws XMLStreamException {
            String elementText = reader.getElementText();
            elementText = elementText == null || elementText.trim().length() == 0 ? null : elementText.trim();
            return elementText;
        }

        private void writeProperty(XMLExtendedStreamWriter writer, String name, String value, String localName)
                throws XMLStreamException {

            writer.writeStartElement(localName);
            writer.writeAttribute("name", name);
            writer.writeCharacters(value);
            writer.writeEndElement();

        }
    }
}
