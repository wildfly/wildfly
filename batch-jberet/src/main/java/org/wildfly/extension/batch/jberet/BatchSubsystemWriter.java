/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet;

import javax.xml.stream.XMLStreamException;
import java.util.List;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.threads.ThreadsParser;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.extension.batch.jberet.job.repository.CommonAttributes;
import org.wildfly.extension.batch.jberet.job.repository.InMemoryJobRepositoryDefinition;
import org.wildfly.extension.batch.jberet.job.repository.JdbcJobRepositoryDefinition;
import org.wildfly.extension.batch.jberet.thread.pool.BatchThreadPoolResourceDefinition;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchSubsystemWriter implements XMLElementWriter<SubsystemMarshallingContext> {

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        final ThreadsParser threadsParser = ThreadsParser.getInstance();
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        final ModelNode model = context.getModelNode();
        BatchSubsystemDefinition.DEFAULT_JOB_REPOSITORY.marshallAsElement(model, writer);
        BatchSubsystemDefinition.DEFAULT_THREAD_POOL.marshallAsElement(model, writer);
        BatchSubsystemDefinition.RESTART_JOBS_ON_RESUME.marshallAsElement(model, writer);
        BatchSubsystemDefinition.SECURITY_DOMAIN.marshallAsElement(model, writer);

        // Write the in-memory job repositories
        if (model.hasDefined(InMemoryJobRepositoryDefinition.NAME)) {
            final List<Property> repositories = model.get(InMemoryJobRepositoryDefinition.NAME).asPropertyList();
            for (Property property : repositories) {
                writer.writeStartElement(Element.JOB_REPOSITORY.getLocalName());
                writeNameAttribute(writer, property.getName());
                CommonAttributes.EXECUTION_RECORDS_LIMIT.marshallAsAttribute(property.getValue(), writer);
                writer.writeEmptyElement(Element.IN_MEMORY.getLocalName());
                writer.writeEndElement(); // end job-repository
            }
        }

        // Write the JDBC job repositories
        if (model.hasDefined(JdbcJobRepositoryDefinition.NAME)) {
            final List<Property> repositories = model.get(JdbcJobRepositoryDefinition.NAME).asPropertyList();
            for (Property property : repositories) {
                writer.writeStartElement(Element.JOB_REPOSITORY.getLocalName());
                writeNameAttribute(writer, property.getName());
                CommonAttributes.EXECUTION_RECORDS_LIMIT.marshallAsAttribute(property.getValue(), writer);
                writer.writeStartElement(Element.JDBC.getLocalName());
                JdbcJobRepositoryDefinition.DATA_SOURCE.marshallAsAttribute(property.getValue(), writer);
                writer.writeEndElement();
                writer.writeEndElement(); // end job-repository
            }
        }

        // Write the thread pool
        if (model.hasDefined(BatchThreadPoolResourceDefinition.NAME)) {
            final List<Property> threadPools = model.get(BatchThreadPoolResourceDefinition.NAME).asPropertyList();
            for (Property threadPool : threadPools) {
                threadsParser.writeUnboundedQueueThreadPool(writer, threadPool, Element.THREAD_POOL.getLocalName(), true);
            }
        }

        // Write out the thread factory
        if (model.hasDefined(BatchSubsystemDefinition.THREAD_FACTORY)) {
            final List<Property> threadFactories = model.get(BatchSubsystemDefinition.THREAD_FACTORY).asPropertyList();
            for (Property threadFactory : threadFactories) {
                threadsParser.writeThreadFactory(writer, threadFactory);
            }
        }

        writer.writeEndElement();
    }

    private static void writeNameAttribute(final XMLExtendedStreamWriter writer, final String name) throws XMLStreamException {
        writer.writeAttribute(Attribute.NAME.getLocalName(), name);
    }
}
