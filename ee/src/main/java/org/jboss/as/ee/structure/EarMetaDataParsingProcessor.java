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

package org.jboss.as.ee.structure;

import static org.jboss.as.ee.EeMessages.MESSAGES;

import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.metadata.ear.spec.Ear6xMetaData;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.parser.spec.EarMetaDataParser;
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

/**
 * Deployment processor responsible for parsing the applicaiton.xml file of an ear.
 *
 * @author John Bailey
 */
public class EarMetaDataParsingProcessor implements DeploymentUnitProcessor {
    private static final String APPLICATION_XML = "META-INF/application.xml";

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
        final VirtualFile deploymentFile = deploymentRoot.getRoot();
        final VirtualFile applicationXmlFile = deploymentFile.getChild(APPLICATION_XML);
        if (!applicationXmlFile.exists()) {
            return;
        }

        InputStream inputStream = null;
        try {
            inputStream = applicationXmlFile.openStream();
            final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setXMLResolver(NoopXMLResolver.create());
            XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(inputStream);
            final EarMetaData earMetaData = EarMetaDataParser.parse(xmlReader);
            if (earMetaData != null) {
                deploymentUnit.putAttachment(Attachments.EAR_METADATA, earMetaData);
                if(earMetaData instanceof Ear6xMetaData) {
                    Ear6xMetaData metaData = (Ear6xMetaData)earMetaData;
                    if(metaData.getEarEnvironmentRefsGroup() != null) {
                        final DeploymentDescriptorEnvironment bindings = new DeploymentDescriptorEnvironment("java:app/env/", ((Ear6xMetaData) earMetaData).getEarEnvironmentRefsGroup());
                        deploymentUnit.putAttachment(org.jboss.as.ee.component.Attachments.MODULE_DEPLOYMENT_DESCRIPTOR_ENVIRONMENT, bindings);
                    }
                }
            }
        } catch (Exception e) {
            throw MESSAGES.failedToParse(e, applicationXmlFile);
        } finally {
            VFSUtils.safeClose(inputStream);
        }

    }

    public void undeploy(final DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(Attachments.EAR_METADATA);
    }


}
