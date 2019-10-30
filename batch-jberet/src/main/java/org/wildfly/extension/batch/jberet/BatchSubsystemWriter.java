/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.batch.jberet;

import javax.xml.stream.XMLStreamException;
import java.util.List;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.threads.ThreadsParser;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
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
