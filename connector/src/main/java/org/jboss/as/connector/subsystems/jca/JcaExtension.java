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

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.connector.subsystems.resourceadapters.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.threads.BoundedQueueThreadPoolAdd;
import org.jboss.as.threads.BoundedQueueThreadPoolRemove;
import org.jboss.as.threads.ThreadsParser;
import org.jboss.as.threads.ThreadsSubsystemProviders;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import static org.jboss.as.connector.ConnectorLogger.ROOT_LOGGER;
import static org.jboss.as.connector.subsystems.jca.ArchiveValidationAdd.ArchiveValidationParameters;
import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION;
import static org.jboss.as.connector.subsystems.jca.Constants.BEAN_VALIDATION;
import static org.jboss.as.connector.subsystems.jca.Constants.BOOTSTRAP_CONTEXT;
import static org.jboss.as.connector.subsystems.jca.Constants.CACHED_CONNECTION_MANAGER;
import static org.jboss.as.connector.subsystems.jca.Constants.DEFAULT_NAME;
import static org.jboss.as.connector.subsystems.jca.Constants.JCA;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_LONG_RUNNING;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_SHORT_RUNNING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.threads.CommonAttributes.BLOCKING;
import static org.jboss.as.threads.CommonAttributes.NAME;

/**
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class JcaExtension implements Extension {

    public static String SUBSYSTEM_NAME = "jca";

    @Override
    public void initialize(final ExtensionContext context) {
        ROOT_LOGGER.debugf("Initializing Connector Extension");

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);

        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(JcaSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, JcaSubsystemAdd.INSTANCE, JcaSubsystemProviders.SUBSYSTEM_ADD_DESC, false);
        registration.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, JcaSubsystemProviders.SUBSYSTEM_REMOVE_DESC, false);

        registration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        final ManagementResourceRegistration archiveValidation =
            registration.registerSubModel(PathElement.pathElement(ARCHIVE_VALIDATION, ARCHIVE_VALIDATION), JcaSubsystemProviders.ARCHIVE_VALIDATION_DESC);
        archiveValidation.registerOperationHandler(ADD, ArchiveValidationAdd.INSTANCE, JcaSubsystemProviders.ADD_ARCHIVE_VALIDATION_DESC, false);
        archiveValidation.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, JcaSubsystemProviders.REMOVE_ARCHIVE_VALIDATION_DESC, false);

        for (final ArchiveValidationAdd.ArchiveValidationParameters parameter : ArchiveValidationAdd.ArchiveValidationParameters.values()) {
            archiveValidation.registerReadWriteAttribute(parameter.getAttribute().getName(), null, new ReloadRequiredWriteAttributeHandler() , AttributeAccess.Storage.CONFIGURATION);
        }

        final ManagementResourceRegistration beanValidation =
            registration.registerSubModel(PathElement.pathElement(BEAN_VALIDATION, BEAN_VALIDATION), JcaSubsystemProviders.BEAN_VALIDATION_DESC);
        beanValidation.registerOperationHandler(ADD, BeanValidationAdd.INSTANCE, JcaSubsystemProviders.ADD_BEAN_VALIDATION_DESC, false);
        beanValidation.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, JcaSubsystemProviders.REMOVE_BEAN_VALIDATION_DESC, false);

        for (final BeanValidationAdd.BeanValidationParameters parameter : BeanValidationAdd.BeanValidationParameters.values()) {
            beanValidation.registerReadWriteAttribute(parameter.getAttribute().getName(), null, new ReloadRequiredWriteAttributeHandler() , AttributeAccess.Storage.CONFIGURATION);
        }

        final ManagementResourceRegistration cachedConnectionManager =
            registration.registerSubModel(PathElement.pathElement(CACHED_CONNECTION_MANAGER, CACHED_CONNECTION_MANAGER), JcaSubsystemProviders.CACHED_CONNECTION_MANAGER_DESC);
        cachedConnectionManager.registerOperationHandler(ADD, CachedConnectionManagerAdd.INSTANCE, JcaSubsystemProviders.ADD_CACHED_CONNECTION_MANAGER_DESC, false);
        cachedConnectionManager.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, JcaSubsystemProviders.REMOVE_CACHED_CONNECTION_MANAGER_DESC, false);

        for (final CachedConnectionManagerAdd.CcmParameters parameter : CachedConnectionManagerAdd.CcmParameters.values()) {
            cachedConnectionManager.registerReadWriteAttribute(parameter.getAttribute().getName(), null, new ReloadRequiredWriteAttributeHandler() , AttributeAccess.Storage.CONFIGURATION);
        }

        final ManagementResourceRegistration workManager =
            registration.registerSubModel(PathElement.pathElement(WORKMANAGER), JcaSubsystemProviders.WORKMANAGER_DESC);
        workManager.registerOperationHandler(ADD, WorkManagerAdd.INSTANCE, JcaSubsystemProviders.ADD_WORKMANAGER_DESC, false);
        workManager.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, JcaSubsystemProviders.REMOVE_WORKMANAGER_DESC, false);

        for (final WorkManagerAdd.WmParameters parameter : WorkManagerAdd.WmParameters.values()) {
            workManager.registerReadWriteAttribute(parameter.getAttribute().getName(), null, new ReloadRequiredWriteAttributeHandler() , AttributeAccess.Storage.CONFIGURATION);
        }

        final ManagementResourceRegistration shortRunning = workManager.registerSubModel(PathElement.pathElement(WORKMANAGER_SHORT_RUNNING), ThreadsSubsystemProviders.BOUNDED_QUEUE_THREAD_POOL_DESC);
        shortRunning.registerOperationHandler(ADD, BoundedQueueThreadPoolAdd.INSTANCE, BoundedQueueThreadPoolAdd.INSTANCE, false);
        shortRunning.registerOperationHandler(REMOVE, BoundedQueueThreadPoolRemove.INSTANCE, BoundedQueueThreadPoolRemove.INSTANCE, false);
        final ManagementResourceRegistration longRunning = workManager.registerSubModel(PathElement.pathElement(WORKMANAGER_LONG_RUNNING), ThreadsSubsystemProviders.BOUNDED_QUEUE_THREAD_POOL_DESC);
        longRunning.registerOperationHandler(ADD, BoundedQueueThreadPoolAdd.INSTANCE, BoundedQueueThreadPoolAdd.INSTANCE, false);
        longRunning.registerOperationHandler(REMOVE, BoundedQueueThreadPoolRemove.INSTANCE, BoundedQueueThreadPoolRemove.INSTANCE, false);


        final ManagementResourceRegistration bootstrapContext =
            registration.registerSubModel(PathElement.pathElement(BOOTSTRAP_CONTEXT), JcaSubsystemProviders.BOOTSTRAP_CONTEXT_DESC);
        bootstrapContext.registerOperationHandler(ADD, BootstrapContextAdd.INSTANCE, JcaSubsystemProviders.ADD_BOOTSTRAP_CONTEXT_DESC, false);
        bootstrapContext.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, JcaSubsystemProviders.REMOVE_BOOTSTRAP_CONTEXT_DESC, false);

        for (final BootstrapContextAdd.BootstrapCtxParameters parameter : BootstrapContextAdd.BootstrapCtxParameters.values()) {
            bootstrapContext.registerReadWriteAttribute(parameter.getAttribute().getName(), null, new ReloadRequiredWriteAttributeHandler() , AttributeAccess.Storage.CONFIGURATION);
        }

        subsystem.registerXMLElementWriter(ConnectorSubsystemParser.INSTANCE);

    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JCA_1_0.getUriString(), ConnectorSubsystemParser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JCA_1_1.getUriString(), ConnectorSubsystemParser.INSTANCE);
    }

    private static ModelNode createEmptyAddOperation() {
        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, JCA);
        address.protect();

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);
        return subsystem;
    }

    static final class ConnectorSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
            XMLElementWriter<SubsystemMarshallingContext> {

        static final ConnectorSubsystemParser INSTANCE = new ConnectorSubsystemParser();

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
            ModelNode node = context.getModelNode();

            writeArchiveValidation(writer, node);
            writeBeanValidation(writer, node);
            writeWorkManagers(writer, node);
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

                if (BeanValidationAdd.BeanValidationParameters.BEAN_VALIDATION_ENABLED.getAttribute().isMarshallable(node)) {
                    writer.writeEmptyElement(Element.BEAN_VALIDATION.getLocalName());
                    BeanValidationAdd.BeanValidationParameters.BEAN_VALIDATION_ENABLED.getAttribute().marshallAsAttribute(node, writer);
                }
            }
        }

        private void writeCachedConnectionManager(XMLExtendedStreamWriter writer, ModelNode parentNode) throws XMLStreamException {
            if (parentNode.hasDefined(CACHED_CONNECTION_MANAGER)) {
                ModelNode node = parentNode.get(CACHED_CONNECTION_MANAGER).get(CACHED_CONNECTION_MANAGER);

                final String name = CachedConnectionManagerAdd.CcmParameters.INSTALL.getAttribute().getName();
                if(node.hasDefined(name) &&
                        node.get(name).asBoolean()) {
                    writer.writeEmptyElement(Element.CACHED_CONNECTION_MANAGER.getLocalName());
                    CachedConnectionManagerAdd.CcmParameters.DEBUG.getAttribute().marshallAsAttribute(node, writer);
                    CachedConnectionManagerAdd.CcmParameters.ERROR.getAttribute().marshallAsAttribute(node, writer);
                }
            }
        }

        private void writeWorkManagers(XMLExtendedStreamWriter writer, ModelNode parentNode) throws XMLStreamException {
            if (parentNode.hasDefined(WORKMANAGER) && parentNode.get(WORKMANAGER).asList().size() != 0) {
                for (Property property : parentNode.get(WORKMANAGER).asPropertyList()) {
                    if ("default".equals(property.getValue().get(NAME).asString())) {
                        writer.writeStartElement(Element.DEFAULT_WORKMANAGER.getLocalName());
                    } else {
                        writer.writeStartElement(Element.WORKMANAGER.getLocalName());
                        WorkManagerAdd.WmParameters.NAME.getAttribute().marshallAsAttribute(property.getValue(), writer);
                    }
                    for (Property prop : property.getValue().asPropertyList()) {
                        if (WORKMANAGER_LONG_RUNNING.equals(prop.getName()) && prop.getValue().isDefined() && prop.getValue().asPropertyList().size() != 0) {
                            ThreadsParser.getInstance().writeBoundedQueueThreadPool(writer, prop.getValue().asProperty().getValue(), Element.LONG_RUNNING_THREADS.getLocalName(), false);
                        }
                        if (WORKMANAGER_SHORT_RUNNING.equals(prop.getName())) {
                            ThreadsParser.getInstance().writeBoundedQueueThreadPool(writer, prop.getValue().asProperty().getValue(), Element.SHORT_RUNNING_THREADS.getLocalName(), false);
                        }
                    }
                    writer.writeEndElement();
                }
            }
        }


        private void writeBootstrapContexts(XMLExtendedStreamWriter writer, ModelNode parentNode) throws XMLStreamException {
            if (parentNode.hasDefined(BOOTSTRAP_CONTEXT) && parentNode.get(BOOTSTRAP_CONTEXT).asList().size() != 0) {

                boolean started = false;

                for (ModelNode node : parentNode.get(BOOTSTRAP_CONTEXT).asList()) {
                    if (BootstrapContextAdd.BootstrapCtxParameters.NAME.getAttribute().isMarshallable(node) ||
                            BootstrapContextAdd.BootstrapCtxParameters.WORKMANAGER.getAttribute().isMarshallable(node)) {
                        if (!started) {
                            writer.writeStartElement(Element.BOOTSTRAP_CONTEXTS.getLocalName());
                            started = true;
                        }
                        writer.writeStartElement(Element.BOOTSTRAP_CONTEXT.getLocalName());
                        BootstrapContextAdd.BootstrapCtxParameters.NAME.getAttribute().marshallAsAttribute(node, writer);
                        BootstrapContextAdd.BootstrapCtxParameters.WORKMANAGER.getAttribute().marshallAsAttribute(node, writer);
                        writer.writeEndElement();
                    }
                }
                if (started) {
                    writer.writeEndElement();
                }
            }
        }

        private boolean has(ModelNode node, String name) {
            return node.has(name) && node.get(name).isDefined();
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
                    case JCA_1_1:
                    case JCA_1_0: {
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
                                break;
                            }
                            case BOOTSTRAP_CONTEXTS: {
                                ModelNode operation = parseBootstrapContexts(reader, address);
                                if (operation != null) {
                                    list.add(operation);
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
                        String value = rawAttributeText(reader,  ArchiveValidationParameters.ARCHIVE_VALIDATION_ENABLED.getAttribute().getXmlName());
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
                        name = rawAttributeText(reader, WorkManagerAdd.WmParameters.NAME.getAttribute().getXmlName());
                        WorkManagerAdd.WmParameters.NAME.getAttribute().parseAndSetParameter(name, workManagerOperation, reader);
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
                        switch (readerNS) {
                            case JCA_1_0: {
                                org.jboss.as.threads.Namespace ns =  org.jboss.as.threads.Namespace.THREADS_1_0;
                                ThreadsParser.getInstance().parseBoundedQueueThreadPool(reader, readerNS.getUriString(), ns, workManagerAddress, list, WORKMANAGER_LONG_RUNNING, name + "-" + WORKMANAGER_LONG_RUNNING);
                                break;
                            }
                            default: {
                                org.jboss.as.threads.Namespace ns =  org.jboss.as.threads.Namespace.THREADS_1_1;
                                ThreadsParser.getInstance().parseBoundedQueueThreadPool(reader, readerNS.getUriString(), ns, workManagerAddress, list, WORKMANAGER_LONG_RUNNING, name + "-" + WORKMANAGER_LONG_RUNNING);
                            }
                        }
                        break;
                    }
                    case SHORT_RUNNING_THREADS: {
                        switch (readerNS) {
                            case JCA_1_0: {
                                org.jboss.as.threads.Namespace ns =  org.jboss.as.threads.Namespace.THREADS_1_0;
                                ThreadsParser.getInstance().parseBoundedQueueThreadPool(reader, readerNS.getUriString(), ns, workManagerAddress, list, WORKMANAGER_SHORT_RUNNING, name + "-" + WORKMANAGER_SHORT_RUNNING);
                                break;
                            }
                            default: {
                                org.jboss.as.threads.Namespace ns =  org.jboss.as.threads.Namespace.THREADS_1_1;
                                ThreadsParser.getInstance().parseBoundedQueueThreadPool(reader, readerNS.getUriString(), ns, workManagerAddress, list, WORKMANAGER_SHORT_RUNNING, name + "-" + WORKMANAGER_SHORT_RUNNING);
                                break;
                            }
                        }
                        break;
                    }
                    default:
                        throw unexpectedElement(reader);
                }

            }
            //AS7-1352 The "blocking" attribute for the JCA thread pools should be ignored as it should always be true.
            for (ModelNode op : list) {
                if (op.hasDefined(BLOCKING)) {
                    op.get(BLOCKING).set(Boolean.TRUE);
                }
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
                        String value = rawAttributeText(reader, BeanValidationAdd.BeanValidationParameters.BEAN_VALIDATION_ENABLED.getAttribute().getXmlName());
                        BeanValidationAdd.BeanValidationParameters.BEAN_VALIDATION_ENABLED.getAttribute().parseAndSetParameter(value, beanValidationOperation, reader);
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

        private ModelNode parseCcm(final XMLExtendedStreamReader reader, final ModelNode parentOperation) throws XMLStreamException {
            final ModelNode ccmOperation = new ModelNode();
            ccmOperation.get(OP).set(ADD);

            final ModelNode ccmAddress = parentOperation.clone();
            ccmAddress.add(CACHED_CONNECTION_MANAGER,CACHED_CONNECTION_MANAGER);
            ccmAddress.protect();

            ccmOperation.get(OP_ADDR).set(ccmAddress);


            final int cnt = reader.getAttributeCount();
            for (int i = 0; i < cnt; i++) {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case DEBUG: {
                        String value = rawAttributeText(reader, CachedConnectionManagerAdd.CcmParameters.DEBUG.getAttribute().getXmlName());
                        CachedConnectionManagerAdd.CcmParameters.DEBUG.getAttribute().parseAndSetParameter(value, ccmOperation, reader);
                        break;
                    }
                    case ERROR: {
                        String value = rawAttributeText(reader, CachedConnectionManagerAdd.CcmParameters.ERROR.getAttribute().getXmlName());
                        CachedConnectionManagerAdd.CcmParameters.ERROR.getAttribute().parseAndSetParameter(value, ccmOperation, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
            ccmOperation.get(CachedConnectionManagerAdd.CcmParameters.INSTALL.getAttribute().getName()).set(true);
            // Handle elements
            requireNoContent(reader);

            return ccmOperation;

        }

        private ModelNode parseBootstrapContexts(final XMLExtendedStreamReader reader, final ModelNode parentAddress) throws XMLStreamException {
            ModelNode bootstrapContextOperation = null;
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {

                final Element element = Element.forName(reader.getLocalName());

                switch (element) {
                    case BOOTSTRAP_CONTEXT: {
                        bootstrapContextOperation = new ModelNode();
                        bootstrapContextOperation.get(OP).set(ADD);

                        final int cnt = reader.getAttributeCount();
                        String name = null;
                        String wmName = null;
                        for (int i = 0; i < cnt; i++) {
                            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                            switch (attribute) {
                                case NAME: {
                                    name = rawAttributeText(reader, BootstrapContextAdd.BootstrapCtxParameters.NAME.getAttribute().getXmlName());
                                    BootstrapContextAdd.BootstrapCtxParameters.NAME.getAttribute().parseAndSetParameter(name, bootstrapContextOperation, reader);
                                    break;
                                }
                                case WORKMANAGER: {
                                    wmName = rawAttributeText(reader, BootstrapContextAdd.BootstrapCtxParameters.WORKMANAGER.getAttribute().getXmlName());
                                    BootstrapContextAdd.BootstrapCtxParameters.WORKMANAGER.getAttribute().parseAndSetParameter(wmName, bootstrapContextOperation, reader);
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

                        final ModelNode bootStrapCOntextAddress = parentAddress.clone();
                        bootStrapCOntextAddress.add(BOOTSTRAP_CONTEXT, name);
                        bootStrapCOntextAddress.protect();

                        bootstrapContextOperation.get(OP_ADDR).set(bootStrapCOntextAddress);

                        // Handle elements
                        requireNoContent(reader);
                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);

                    }
                }
            }
            return bootstrapContextOperation;
        }

        public String rawElementText(XMLStreamReader reader) throws XMLStreamException {
            String elementtext = reader.getElementText();
            elementtext = elementtext == null || elementtext.trim().length() == 0 ? null : elementtext.trim();
            return elementtext;
        }

        public String rawAttributeText(XMLStreamReader reader, String attributeName) {
        String attributeString = reader.getAttributeValue("", attributeName) == null ? null : reader.getAttributeValue(
                "", attributeName)
                .trim();
        return attributeString;
    }
    }
}
