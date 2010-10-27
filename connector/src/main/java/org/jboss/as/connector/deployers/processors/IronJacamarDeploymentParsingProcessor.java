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

package org.jboss.as.connector.deployers.processors;

import static org.jboss.as.deployment.attachment.VirtualFileAttachment.getVirtualFileAttachment;

import java.io.InputStream;

import org.jboss.as.connector.metadata.xmldescriptors.IronJacamarXmlDescriptor;
import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.jca.common.api.metadata.ironjacamar.IronJacamar;
import org.jboss.jca.common.metadata.ironjacamar.IronJacamarParser;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * DeploymentUnitProcessor responsible for parsing a iron-jacamar.xml descriptor
 * and attaching the corresponding IronJacamar metadata. It take care also to
 * register this metadata into IronJacamar0s MetadataRepository
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 */
public class IronJacamarDeploymentParsingProcessor implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.PARSE_DESCRIPTORS.plus(501L);

    private final InjectedValue<MetadataRepository> mdr = new InjectedValue<MetadataRepository>();

    /**
     * Construct a new instance.
     */
    public IronJacamarDeploymentParsingProcessor() {
        super();

    }

    /**
     * Process a deployment for iron-jacamar.xml files. Will parse the xml file
     * and attach metadata discovered durring processing.
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
            serviceXmlFile = deploymentRoot.getChild("/META-INF/ironjacamar.xml");
        }

        if (serviceXmlFile == null || !serviceXmlFile.exists())
            return;

        InputStream xmlStream = null;
        IronJacamar result = null;
        try {
            xmlStream = serviceXmlFile.openStream();
            result = (new IronJacamarParser()).parse(xmlStream);
            if (result != null) {
                IronJacamarXmlDescriptor xmlDescriptor = new IronJacamarXmlDescriptor(result);
                context.putAttachment(IronJacamarXmlDescriptor.ATTACHMENT_KEY, xmlDescriptor);
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
    public Injector<MetadataRepository> getMdrInjector() {
        return mdr;
    }
}
