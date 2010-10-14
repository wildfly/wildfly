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

package org.jboss.as.connector.deployers;

import org.jboss.jca.common.api.metadata.ra.Connector;
import org.jboss.jca.common.metadata.ra.RaParser;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.msc.value.Value;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.jboss.as.connector.descriptor.ConnectorXmlDescriptor;
import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.logging.Logger;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import static org.jboss.as.deployment.attachment.VirtualFileAttachment.getVirtualFileAttachment;

/**
 * DeploymentUnitProcessor responsible for parsing a standard jca xml descriptor
 * and attaching the corresponding metadata. It take care also to register this
 * metadata into IronJacamar's MetadataRepository
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public class RaDeploymentParsingProcessor implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.PARSE_DESCRIPTORS.plus(500L);
    private static final Logger log = Logger.getLogger("org.jboss.as.deployment.service");

    private final Value<MetadataRepository> mdr;

    /**
     * Construct a new instance.
     */
    public RaDeploymentParsingProcessor(Value<MetadataRepository> mdr) {
        super();
        this.mdr = mdr;
    }

    /**
     * Process a deployment for standard ra deployment files. Will parse the xml
     * file and attach an configuration discovered durring processing.
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    @Override
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = getVirtualFileAttachment(context);

        if (deploymentRoot == null || !deploymentRoot.exists())
            return;

        final String deploymentRootName = deploymentRoot.getName();
        VirtualFile serviceXmlFile = null;
        if (deploymentRootName.endsWith(".rar")) {
            serviceXmlFile = deploymentRoot.getChild("/META-INF/ra.xml");
        }

        if (serviceXmlFile == null || !serviceXmlFile.exists())
            return;

        InputStream xmlStream = null;
        Connector result = null;
        try {
            xmlStream = serviceXmlFile.openStream();
            result = (new RaParser()).parse(xmlStream);
            File root = new File(deploymentRoot.asDirectoryURI());
            URL url = deploymentRoot.asFileURL();
            String deploymentName = deploymentRoot.getName().substring(0, deploymentRoot.getName().indexOf(".rar"));
            if (result != null) {
                ConnectorXmlDescriptor xmlDescriptor = new ConnectorXmlDescriptor(result, root, url, deploymentName);
                // TODO register into mdr
                context.putAttachment(ConnectorXmlDescriptor.ATTACHMENT_KEY, xmlDescriptor);
            } else
                throw new DeploymentUnitProcessingException("Failed to parse service xml [" + serviceXmlFile + "]");
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException("Failed to parse service xml [" + serviceXmlFile + "]", e);
        } finally {
            VFSUtils.safeClose(xmlStream);
        }
    }

    /**
     * @return the mdr
     */
    public Value<MetadataRepository> getMdr() {
        return mdr;
    }
}
