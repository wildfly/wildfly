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

package org.jboss.as.connector.deployers.processors;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.jca.common.metadata.ds.DsParser;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * Picks up -ds.xml deployments
 *
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class DsXmlDeploymentParsingProcessor implements DeploymentUnitProcessor {


    static final AttachmentKey<DataSources> DATA_SOURCES_ATTACHMENT_KEY = AttachmentKey.create(DataSources.class);

    /**
     * Construct a new instance.
     */
    public DsXmlDeploymentParsingProcessor() {
    }

    /**
     * Process a deployment for standard ra deployment files. Will parse the xml
     * file and attach an configuration discovered during processing.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     *
     */
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final VirtualFile deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();

        if (deploymentRoot == null || !deploymentRoot.exists())
            return;

        final String deploymentRootName = deploymentRoot.getLowerCaseName();

        if (!deploymentRootName.endsWith("-ds.xml")) {
            return;
        }

        InputStream xmlStream = null;
        try {
            File f = deploymentRoot.getPhysicalFile();

            xmlStream = new FileInputStream(f);

            DsParser parser = new DsParser();
            DataSources dataSources = parser.parse(xmlStream);
            if(dataSources != null) {
                deploymentUnit.putAttachment(DATA_SOURCES_ATTACHMENT_KEY, dataSources);
            }
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException(e.getMessage(), e);
        } finally {
            VFSUtils.safeClose(xmlStream);
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
