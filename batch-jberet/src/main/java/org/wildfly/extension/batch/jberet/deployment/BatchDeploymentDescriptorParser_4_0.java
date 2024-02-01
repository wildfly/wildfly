/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.deployment;

import org.jberet.repository.InMemoryRepository;
import org.jberet.repository.JobRepository;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.extension.batch.jberet.Attribute;
import org.wildfly.extension.batch.jberet.Element;
import org.wildfly.extension.batch.jberet._private.BatchLogger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

/**
 * A parser for batch deployment descriptors ({@code batch-jberet:3.0}) in {@code jboss-all.xml}.
 */
public class BatchDeploymentDescriptorParser_4_0 extends BatchDeploymentDescriptorParser_3_0 {

    public static final String NAMESPACE = "urn:jboss:domain:batch-jberet:4.0";
    public static final QName ROOT_ELEMENT = new QName(NAMESPACE, "batch");

    @Override
    public BatchEnvironmentMetaData parse(final XMLExtendedStreamReader reader, final DeploymentUnit deploymentUnit) throws XMLStreamException {
        JobRepository jobRepository = null;
        String jobRepositoryName = null;
        String dataSourceName = null;
        String jobExecutorName = null;
        Boolean restartJobsOnResume = null;
        Integer executionRecordsLimit = null;
        boolean empty = true;

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            // Process the job repository
            if (element == Element.JOB_REPOSITORY) {
                executionRecordsLimit = parseExecutionRecordsLimit(reader);

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
                        } else if (jobRepositoryElement == Element.JDBC) {
                            dataSourceName = parseJdbcJobRepository(reader);
                        } else if (jobRepositoryElement == Element.JPA) {
                            dataSourceName = parseJpaJobRepository(reader);
                        } else {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    // Log an error indicating the job-repository is empty, but continue as normal
                    if (jobRepository == null && jobRepositoryName == null && dataSourceName == null) {
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
        return new BatchEnvironmentMetaData(jobRepository, jobRepositoryName, dataSourceName, jobExecutorName,
                restartJobsOnResume, executionRecordsLimit);
    }

    String parseJpaJobRepository(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final String dataSourceName = readRequiredAttribute(reader, Attribute.DATA_SOURCE);
        ParseUtils.requireNoContent(reader);
        return dataSourceName;
    }
}
