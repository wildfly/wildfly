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

package org.wildfly.extension.batch.jberet.deployment;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jberet.repository.InMemoryRepository;
import org.jberet.repository.JobRepository;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXMLParser;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.extension.batch.jberet.Attribute;
import org.wildfly.extension.batch.jberet.Element;
import org.wildfly.extension.batch.jberet._private.BatchLogger;

/**
 * A parser for batch deployment descriptors in {@code jboss-all.xml}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchDeploymentDescriptorParser_1_0 implements XMLStreamConstants, JBossAllXMLParser<BatchEnvironmentMetaData> {

    public static final String NAMESPACE = "urn:jboss:batch-jberet:1.0";
    public static final QName ROOT_ELEMENT = new QName(NAMESPACE, "batch");

    @Override
    public BatchEnvironmentMetaData parse(final XMLExtendedStreamReader reader, final DeploymentUnit deploymentUnit) throws XMLStreamException {
        JobRepository jobRepository = null;
        String jobRepositoryName = null;
        String jobExecutorName = null;
        Boolean restartJobsOnResume = null;
        boolean empty = true;

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            // Process the job repository
            if (element == Element.JOB_REPOSITORY) {
                // Only one repository can be defined
                if (jobRepository != null || jobRepositoryName != null) {
                    BatchLogger.LOGGER.multipleJobRepositoriesFound();
                } else {
                    if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        final String name = reader.getLocalName();
                        final Element jobRepositoryElement = Element.forName(name);
                        if (jobRepositoryElement == Element.IN_MEMORY) {
                            ParseUtils.requireNoContent(reader);
                            jobRepository = new InMemoryRepository();
                        } else if (jobRepositoryElement == Element.NAMED) {
                            jobRepositoryName = readRequiredAttribute(reader, Attribute.NAME);
                            ParseUtils.requireNoContent(reader);
                        } else {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    // Log an error indicating the job-repository is empty, but continue as normal
                    if (jobRepository == null && jobRepositoryName == null) {
                        BatchLogger.LOGGER.emptyJobRepositoryElement(deploymentUnit.getName());
                    }
                }
                ParseUtils.requireNoContent(reader);

            } else if (element == Element.THREAD_POOL) {
                // Only thread-pool's defined on the subsystem are allowed to be referenced
                jobExecutorName = readRequiredAttribute(reader, Attribute.NAME);
                ParseUtils.requireNoContent(reader);
            } else if (element == Element.RESTART_JOBS_ON_RESUME) {
                restartJobsOnResume = Boolean.valueOf(readRequiredAttribute(reader, Attribute.VALUE));
                ParseUtils.requireNoContent(reader);
            } else {
                throw ParseUtils.unexpectedElement(reader);
            }
            empty = false;
        }
        if (empty) {
            // An empty tag was found. A null value will result in the subsystem default being used.
            BatchLogger.LOGGER.debugf("An empty batch element in the deployment descriptor was found for %s.", deploymentUnit.getName());
            return null;
        }
        return new BatchEnvironmentMetaData(jobRepository, jobRepositoryName, jobExecutorName, restartJobsOnResume);
    }

    private static String readRequiredAttribute(final XMLExtendedStreamReader reader, final Attribute attribute) throws XMLStreamException {
        final int attributeCount = reader.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            final Attribute current = Attribute.forName(reader.getAttributeLocalName(i));
            if (current == attribute) {
                return reader.getAttributeValue(i);
            }
        }
        throw ParseUtils.missingRequired(reader, attribute.getLocalName());
    }
}
