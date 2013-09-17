/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.batch;

import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.threads.ThreadsParser;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class BatchSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    static final BatchSubsystemParser INSTANCE = new BatchSubsystemParser();

    private final ThreadsParser threadsParser;

    public BatchSubsystemParser() {
        threadsParser = ThreadsParser.getInstance();
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        final PathAddress subsystemAddress = PathAddress.pathAddress(BatchSubsystemDefinition.SUBSYSTEM_PATH);
        // Add the subsytem
        list.add(Util.createAddOperation(subsystemAddress));

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
            if (namespace == Namespace.BATCH_1_0) {
                final String localName = reader.getLocalName();
                final Element element = Element.forName(localName);
                if (element == Element.JOB_REPOSITORY) {
                    parseJobRepository(reader, subsystemAddress, list);
                } else if (element == Element.THREAD_POOL) {
                    threadsParser.parseUnboundedQueueThreadPool(reader, namespace.getUriString(),
                            org.jboss.as.threads.Namespace.THREADS_1_1, subsystemAddress.toModelNode(), list,
                            BatchConstants.THREAD_POOL, BatchConstants.THREAD_POOL_NAME);
                } else if (element == Element.THREAD_FACTORY) {
                    threadsParser.parseThreadFactory(reader, namespace.getUriString(),
                            org.jboss.as.threads.Namespace.THREADS_1_1, subsystemAddress.toModelNode(), list,
                            BatchConstants.THREAD_FACTORY, null);
                } else {
                    throw ParseUtils.unexpectedElement(reader);
                }
            } else {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseJobRepository(final XMLExtendedStreamReader reader, final PathAddress address, final List<ModelNode> list) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String localName = reader.getLocalName();
            final Element element = Element.forName(localName);
            if (element == Element.IN_MEMORY) {
                list.add(Util.createAddOperation(PathAddress.pathAddress(address, JobRepositoryDefinition.IN_MEMORY.getPathElement())));
            } else if (element == Element.JDBC) {
                if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                    final String value = ParseUtils.readStringAttributeElement(reader, Attribute.JNDI_NAME.getLocalName());
                    final ModelNode op = Util.createAddOperation(PathAddress.pathAddress(address, JobRepositoryDefinition.JDBC.getPathElement()));
                    JobRepositoryDefinition.JNDI_NAME.parseAndSetParameter(value, op, reader);
                    list.add(op);
                } else {
                    throw ParseUtils.unexpectedElement(reader);
                }
            } else {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
        // Expect end tag for job-repository
        if (!(reader.hasNext() && reader.nextTag() == END_ELEMENT)) {
            throw ParseUtils.unexpectedElement(reader);
        }
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        final ModelNode model = context.getModelNode();
        if (model.hasDefined(JobRepositoryDefinition.NAME)) {
            // Write the job-repository
            writer.writeStartElement(JobRepositoryDefinition.NAME);
            // The value is the job repository type
            final String value = model.get(JobRepositoryDefinition.NAME).asProperty().getName();
            // TODO (jrp) find a cleaner way to do this
            if (JobRepositoryDefinition.JDBC.getPathElement().getValue().equals(value)) {
                writer.writeStartElement(Element.JDBC.getLocalName());
                JobRepositoryDefinition.JNDI_NAME.marshallAsAttribute(model.get(JobRepositoryDefinition.NAME), writer);
                writer.writeEndElement();
            } else {
                // Write in-memory by default
                writer.writeStartElement(Element.IN_MEMORY.getLocalName());
                writer.writeEndElement();
            }
            // End job-repository
            writer.writeEndElement();

            // Write the thread pool
            threadsParser.writeUnboundedQueueThreadPool(writer, model.get(BatchConstants.THREAD_POOL).asProperty(), Element.THREAD_POOL.getLocalName(), false);

            // Write out the thread factory
            if (model.hasDefined(BatchConstants.THREAD_FACTORY)) {
                threadsParser.writeThreadFactory(writer, model.get(BatchConstants.THREAD_FACTORY).asProperty());
            }
        } else {
            // Should always be defined, but write in-memory by default
            writer.writeStartElement(JobRepositoryDefinition.NAME);
            writer.writeStartElement(Element.IN_MEMORY.getLocalName());
            writer.writeEndElement();
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }
}
