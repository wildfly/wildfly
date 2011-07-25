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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

/**
 * @author Emanuel Muckenhuber
 */
public class EJB3Extension implements Extension {

    public static final String SUBSYSTEM_NAME = "ejb3";
    public static final String NAMESPACE = "urn:jboss:domain:ejb3:1.0";

    private static final EJB3SubsystemParser parser = new EJB3SubsystemParser();

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(EJB3SubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, EJB3SubsystemAdd.INSTANCE, EJB3SubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, SubsystemDescribeHandler.INSTANCE, SubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        subsystem.registerXMLElementWriter(parser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(NAMESPACE, parser);
    }

    static class EJB3SubsystemParser implements XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(NAMESPACE, false);
            ModelNode ejbSubSystem = context.getModelNode();
            // write the ear subdeployment isolation attribute
            if (ejbSubSystem.hasDefined(CommonAttributes.TIMER_SERVICE)) {
                writer.writeStartElement(Element.TIMER_SERVICE.getLocalName());
                final ModelNode timerService = ejbSubSystem.get(CommonAttributes.TIMER_SERVICE);

                final ModelNode threadPool = timerService.get(CommonAttributes.THREAD_POOL);
                if (threadPool.isDefined()) {
                    writer.writeStartElement(Element.THREAD_POOL.getLocalName());

                    final ModelNode coreThreads = threadPool.get(CommonAttributes.CORE_THREADS);
                    if (coreThreads.isDefined()) {
                        writer.writeAttribute(Attribute.CORE_THREADS.getLocalName(), "" + coreThreads.asInt());
                    }
                    writer.writeEndElement();
                }
                final ModelNode timerDataStore = timerService.get(CommonAttributes.TIMER_DATA_STORE_LOCATION);
                if (timerDataStore.isDefined()) {
                    writer.writeStartElement(Element.DATA_STORE.getLocalName());
                    final ModelNode path = timerDataStore.get(CommonAttributes.PATH);
                    if (path.isDefined()) {
                        writer.writeAttribute(Attribute.PATH.getLocalName(), path.asString());
                    }
                    final ModelNode relativeTo = timerDataStore.get(CommonAttributes.RELATIVE_TO);
                    if (relativeTo.isDefined()) {
                        writer.writeAttribute(Attribute.RELATIVE_TO.getLocalName(), relativeTo.asString());
                    }
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {

            // EE subsystem doesn't have any attributes, so make sure that the xml doesn't have any
            requireNoAttributes(reader);

            final ModelNode update = new ModelNode();
            update.get(OP).set(ADD);
            update.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
            list.add(update);

            // elements
            final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
            while (reader.hasNext() && reader.nextTag() != XMLStreamReader.END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case EJB3_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        if (!encountered.add(element)) {
                            throw unexpectedElement(reader);
                        }
                        switch (element) {
                            case TIMER_SERVICE: {
                                final ModelNode model = parseTimerService(reader);
                                update.get(CommonAttributes.TIMER_SERVICE).set(model);
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

        private ModelNode parseTimerService(final XMLExtendedStreamReader reader) throws XMLStreamException {
            ModelNode timerService = new ModelNode();

            requireNoAttributes(reader);

            while (reader.hasNext() && reader.nextTag() != XMLStreamReader.END_ELEMENT) {
                switch (Element.forName(reader.getLocalName())) {
                    case THREAD_POOL: {
                        final int count = reader.getAttributeCount();
                        Integer coreThreads = null;
                        for (int i = 0; i < count; i++) {
                            requireNoNamespaceAttribute(reader, i);
                            final String value = reader.getAttributeValue(i);
                            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                            switch (attribute) {
                                case CORE_THREADS:
                                    if (coreThreads != null) {
                                        throw unexpectedAttribute(reader, i);
                                    }
                                    coreThreads = Integer.valueOf(value);
                                    break;
                                default:
                                    unexpectedAttribute(reader, i);
                            }
                        }
                        if (coreThreads == null) {
                            missingRequired(reader, Collections.singleton(Attribute.CORE_THREADS));
                        }
                        timerService.get(CommonAttributes.THREAD_POOL).get(CommonAttributes.CORE_THREADS).set(coreThreads.intValue());
                        requireNoContent(reader);
                        break;
                    }
                    case DATA_STORE: {
                        final int count = reader.getAttributeCount();
                        String path = null;
                        String relativeTo = null;
                        for (int i = 0; i < count; i++) {
                            requireNoNamespaceAttribute(reader, i);
                            final String value = reader.getAttributeValue(i);
                            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                            switch (attribute) {
                                case PATH:
                                    if (path != null) {
                                        throw unexpectedAttribute(reader, i);
                                    }
                                    path = value;
                                    break;
                                case RELATIVE_TO:
                                    if (relativeTo != null) {
                                        throw unexpectedAttribute(reader, i);
                                    }
                                    relativeTo = value;
                                    break;
                                default:
                                    unexpectedAttribute(reader, i);
                            }
                        }
                        if (path == null) {
                            missingRequired(reader, Collections.singleton(Attribute.PATH));
                        }
                        timerService.get(CommonAttributes.TIMER_DATA_STORE_LOCATION).get(CommonAttributes.PATH).set(path);
                        if(relativeTo != null) {
                            timerService.get(CommonAttributes.TIMER_DATA_STORE_LOCATION).get(CommonAttributes.RELATIVE_TO).set(relativeTo);
                        }
                        requireNoContent(reader);
                        break;
                    }
                    default: {
                        unexpectedElement(reader);
                    }
                }
            }
            return timerService;
        }
    }

    private static ModelNode createAddSubSystemOperation() {
        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
        return subsystem;
    }

    private static class SubsystemDescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final SubsystemDescribeHandler INSTANCE = new SubsystemDescribeHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.getResult().add(createAddSubSystemOperation());
            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode(); // internal operation
        }
    }
}
