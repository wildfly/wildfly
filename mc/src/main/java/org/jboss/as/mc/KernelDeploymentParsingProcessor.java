/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.mc;

import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.mc.descriptor.KernelDeploymentXmlDescriptor;
import org.jboss.as.mc.descriptor.KernelDeploymentXmlDescriptorParser;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.staxmapper.XMLMapper;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * DeploymentUnitProcessor responsible for parsing a jboss-beans.xml
 * descriptor and attaching the corresponding KernelDeploymentXmlDescriptor.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class KernelDeploymentParsingProcessor implements DeploymentUnitProcessor {

    private final XMLMapper xmlMapper = XMLMapper.Factory.create();
    private final XMLInputFactory inputFactory = XMLInputFactory.newInstance();

    public KernelDeploymentParsingProcessor() {
        xmlMapper.registerRootElement(new QName("urn:jboss:mc:7.0", "deployment"), new KernelDeploymentXmlDescriptorParser());
    }

    /**
     * Process a deployment for jboss-beans.xml files.
     * Will parse the xml file and attach an configuration discovered during processing.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = phaseContext.getDeploymentUnit().getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();

        if(deploymentRoot == null || deploymentRoot.exists() == false)
            return;

        final String deploymentRootName = deploymentRoot.getLowerCaseName();
        VirtualFile beansXmlFile = null;
        if(deploymentRootName.endsWith(".jar")) {
            beansXmlFile = deploymentRoot.getChild("META-INF/jboss-beans.xml");
        } else if(deploymentRootName.endsWith("jboss-beans.xml")) {
            beansXmlFile = deploymentRoot;
        }
        if(beansXmlFile == null || beansXmlFile.exists() == false)
            return;

        InputStream xmlStream = null;
        try {
            xmlStream = beansXmlFile.openStream();
            final XMLStreamReader reader = inputFactory.createXMLStreamReader(xmlStream);
            final ParseResult<KernelDeploymentXmlDescriptor> result = new ParseResult<KernelDeploymentXmlDescriptor>();
            xmlMapper.parseDocument(result, reader);
            final KernelDeploymentXmlDescriptor xmlDescriptor = result.getResult();
            if(xmlDescriptor != null)
                phaseContext.getDeploymentUnit().putAttachment(KernelDeploymentXmlDescriptor.ATTACHMENT_KEY, xmlDescriptor);
            else
                throw new DeploymentUnitProcessingException("Failed to parse MC beans xml [" + beansXmlFile + "]");
        } catch(Exception e) {
            throw new DeploymentUnitProcessingException("Failed to parse MC beans xml [" + beansXmlFile + "]", e);
        } finally {
            VFSUtils.safeClose(xmlStream);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
