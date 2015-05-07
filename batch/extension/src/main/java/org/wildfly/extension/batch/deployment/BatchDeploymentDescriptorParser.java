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

package org.wildfly.extension.batch.deployment;

import java.util.Properties;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jberet.repository.InMemoryRepository;
import org.jberet.repository.JdbcRepository;
import org.jberet.repository.JobRepository;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXMLParser;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.extension.batch.Attribute;
import org.wildfly.extension.batch.Element;
import org.wildfly.extension.batch._private.BatchLogger;
import org.wildfly.extension.batch.job.repository.JobRepositoryFactory;

/**
 * A parser for batch deployment descriptors in {@code jboss-all.xml}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchDeploymentDescriptorParser implements XMLStreamConstants, JBossAllXMLParser<JobRepository> {

    public static final AttachmentKey<JobRepository> ATTACHMENT_KEY = AttachmentKey.create(JobRepository.class);
    public static final String NAMESPACE = "urn:jboss:batch:1.0";
    public static final QName ROOT_ELEMENT = new QName(NAMESPACE, "batch");

    @Override
    public JobRepository parse(final XMLExtendedStreamReader reader, final DeploymentUnit deploymentUnit) throws XMLStreamException {
        if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String localName = reader.getLocalName();
            final Element element = Element.forName(localName);
            if (element == Element.JOB_REPOSITORY) {
                return parseJobRepository(reader, deploymentUnit);
            }
        }
        // An empty tag was found. A null value will result in the subsystem default being used.
        BatchLogger.LOGGER.debugf("An empty batch element in the deployment descriptor was found for %s.", deploymentUnit.getName());
        return null;
    }

    private JobRepository parseJobRepository(final XMLExtendedStreamReader reader, final DeploymentUnit deploymentUnit) throws XMLStreamException {
        if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String localName = reader.getLocalName();
            final Element element = Element.forName(localName);
            if (element == Element.IN_MEMORY) {
                ParseUtils.requireNoContent(reader);
                return new InMemoryRepository();
            } else if (element == Element.JDBC) {
                final String value = ParseUtils.readStringAttributeElement(reader, Attribute.JNDI_NAME.getLocalName());
                final Properties configProperties = new Properties();
                configProperties.setProperty(JobRepositoryFactory.JNDI_NAME, value);
                return JdbcRepository.create(configProperties);
            } else {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
        // Log an error indicating the job-repository is empty, but return null to continue as normal
        BatchLogger.LOGGER.emptyJobRepositoryElement(deploymentUnit.getName());
        return null;
    }
}
