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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.threads.ThreadsParser;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.extension.batch.job.repository.JobRepositoryType;

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
        final ModelNode subsystemAddOp = Util.createAddOperation(subsystemAddress);
        list.add(subsystemAddOp);
        // Add the job-repository=jdbc resource
        final ModelNode jdbcAddOp = Util.createAddOperation(subsystemAddress.append(JobRepositoryDefinition.JDBC.getPathElement()));
        list.add(jdbcAddOp);

        final Set<Element> requiredElements = EnumSet.of(Element.JOB_REPOSITORY, Element.THREAD_POOL);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
            if (namespace == Namespace.BATCH_1_0) {
                final String localName = reader.getLocalName();
                final Element element = Element.forName(localName);
                if (element == Element.JOB_REPOSITORY) {
                    requiredElements.remove(Element.JOB_REPOSITORY);
                    parseJobRepository(reader, subsystemAddOp, jdbcAddOp);
                } else if (element == Element.THREAD_POOL) {
                    requiredElements.remove(Element.THREAD_POOL);
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
        if (!requiredElements.isEmpty()) {
            throw ParseUtils.missingRequired(reader, requiredElements);
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseJobRepository(final XMLExtendedStreamReader reader, final ModelNode subsystemAddOp, final ModelNode jdbcAddOp) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String localName = reader.getLocalName();
            final Element element = Element.forName(localName);
            final JobRepositoryType jobRepositoryType;
            if (element == Element.IN_MEMORY) {
                jobRepositoryType = JobRepositoryType.IN_MEMORY;
                ParseUtils.requireNoContent(reader);
            } else if (element == Element.JDBC) {
                jobRepositoryType = JobRepositoryType.JDBC;
                if (reader.getAttributeCount() > 0) {
                    final String value = ParseUtils.readStringAttributeElement(reader, Attribute.JNDI_NAME.getLocalName());
                    JobRepositoryDefinition.JNDI_NAME.parseAndSetParameter(value, jdbcAddOp, reader);
                } else {
                    ParseUtils.requireNoContent(reader);
                }
            } else {
                throw ParseUtils.unexpectedElement(reader);
            }
            BatchSubsystemDefinition.JOB_REPOSITORY_TYPE.parseAndSetParameter(jobRepositoryType.toString(), subsystemAddOp, reader);
        }
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        final ModelNode model = context.getModelNode();
        BatchSubsystemDefinition.JOB_REPOSITORY_TYPE.marshallAsElement(model, writer);

        // Write the thread pool
        if (model.hasDefined(BatchConstants.THREAD_POOL)) {
            final List<Property> threadPools = model.get(BatchConstants.THREAD_POOL).asPropertyList();
            for (Property threadPool : threadPools) {
                threadsParser.writeUnboundedQueueThreadPool(writer, threadPool, Element.THREAD_POOL.getLocalName(), false);
            }
        }

        // Write out the thread factory
        if (model.hasDefined(BatchConstants.THREAD_FACTORY)) {
            final List<Property> threadFactories = model.get(BatchConstants.THREAD_FACTORY).asPropertyList();
            for (Property threadFactory : threadFactories) {
                threadsParser.writeThreadFactory(writer, threadFactory);
            }
        }

        writer.writeEndElement();
    }
}
