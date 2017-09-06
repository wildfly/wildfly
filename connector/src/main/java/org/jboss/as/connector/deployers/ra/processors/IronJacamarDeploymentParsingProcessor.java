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

package org.jboss.as.connector.deployers.ra.processors;

import java.io.InputStream;
import java.util.Locale;

import org.jboss.as.connector.deployers.Util;
import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.metadata.xmldescriptors.IronJacamarXmlDescriptor;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.common.metadata.ironjacamar.IronJacamarParser;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * DeploymentUnitProcessor responsible for parsing an iron-jacamar.xml descriptor
 * and attaching the corresponding IronJacamar metadata. It take care also to
 * register this metadata into IronJacamar0s MetadataRepository
 *
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 */
public class IronJacamarDeploymentParsingProcessor implements DeploymentUnitProcessor {

    /**
     * Construct a new instance.
     */
    public IronJacamarDeploymentParsingProcessor() {
    }

    /**
     * Process a deployment for iron-jacamar.xml files. Will parse the xml file
     * and attach metadata discovered during processing.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ResourceRoot resourceRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final VirtualFile deploymentRoot = resourceRoot.getRoot();
        final boolean resolveProperties = Util.shouldResolveJBoss(deploymentUnit);

        IronJacamarXmlDescriptor xmlDescriptor = process(deploymentRoot, resolveProperties);
        if (xmlDescriptor != null) {
            deploymentUnit.putAttachment(IronJacamarXmlDescriptor.ATTACHMENT_KEY, xmlDescriptor);
        }
    }

    public static IronJacamarXmlDescriptor process(VirtualFile deploymentRoot, boolean resolveProperties) throws DeploymentUnitProcessingException {
        IronJacamarXmlDescriptor xmlDescriptor = null;
        if (deploymentRoot == null || !deploymentRoot.exists())
            return null;

        final String deploymentRootName = deploymentRoot.getName().toLowerCase(Locale.ENGLISH);
        VirtualFile serviceXmlFile = null;
        if (deploymentRootName.endsWith(".rar")) {
            serviceXmlFile = deploymentRoot.getChild("/META-INF/ironjacamar.xml");
        }

        if (serviceXmlFile == null || !serviceXmlFile.exists())
            return null;

        InputStream xmlStream = null;
        Activation result = null;
        try {
            xmlStream = serviceXmlFile.openStream();
            IronJacamarParser ironJacamarParser = new IronJacamarParser();
            ironJacamarParser.setSystemPropertiesResolved(resolveProperties);
            result = ironJacamarParser.parse(xmlStream);
            if (result != null) {
                xmlDescriptor = new IronJacamarXmlDescriptor(result);

            } else
                throw ConnectorLogger.ROOT_LOGGER.failedToParseServiceXml(serviceXmlFile);
        } catch (Exception e) {
            throw ConnectorLogger.ROOT_LOGGER.failedToParseServiceXml(e, serviceXmlFile);
        } finally {
            VFSUtils.safeClose(xmlStream);
        }
        return xmlDescriptor;
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
