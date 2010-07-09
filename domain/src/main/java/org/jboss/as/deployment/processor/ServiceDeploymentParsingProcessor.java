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

package org.jboss.as.deployment.processor;

import org.jboss.as.deployment.AttachmentKey;
import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.descriptor.JBossServiceXmlDescriptor;
import org.jboss.as.deployment.descriptor.JBossServiceXmlDescriptorParser;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.model.ParseResult;
import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLMapper;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import java.io.InputStream;

import static org.jboss.as.deployment.attachment.VirtualFileAttachment.getVirtualFileAttachment;

/**
 * DeploymentUnitProcessor responsible for parsing a jboss-service.xml descriptor and attaching the corresponding JBossServiceXmlDescriptor.
 *
 * @author John E. Bailey
 */
public class ServiceDeploymentParsingProcessor implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.PARSE_DESCRIPTORS.plus(100L);
    private static final AttachmentKey<JBossServiceXmlDescriptor> DESCRIPTOR_ATTACHMENT_KEY = new AttachmentKey<JBossServiceXmlDescriptor>(JBossServiceXmlDescriptor.class);

    private final XMLMapper xmlMapper = XMLMapper.Factory.create();
    private final XMLInputFactory inputFactory = XMLInputFactory.newInstance();

    public ServiceDeploymentParsingProcessor() {
        xmlMapper.registerRootElement(new QName("service"), new JBossServiceXmlDescriptorParser());
    }

    @Override
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = getVirtualFileAttachment(context);

        if(deploymentRoot == null || !deploymentRoot.exists())
            return;

        final String deploymentRootName = deploymentRoot.getName();
        VirtualFile serviceXmlFile = null;
        if(deploymentRootName.endsWith(".jar") || deploymentRootName.endsWith(".sar")) {
            serviceXmlFile = deploymentRoot.getChild("META-INF/jboss-service.xml");
        } else if(deploymentRootName.endsWith("-service.xml")) {
            serviceXmlFile = deploymentRoot;
        }
        if(serviceXmlFile == null || !serviceXmlFile.exists())
            return;

        InputStream xmlStream = null;
        try {
            xmlStream = serviceXmlFile.openStream();
            final XMLStreamReader reader = inputFactory.createXMLStreamReader(xmlStream);
            final ParseResult<JBossServiceXmlDescriptor> result = new ParseResult<JBossServiceXmlDescriptor>();
            xmlMapper.parseDocument(result, reader);
            final JBossServiceXmlDescriptor xmlDescriptor = result.getResult();
            if(xmlDescriptor != null)
                context.putAttachment(DESCRIPTOR_ATTACHMENT_KEY, xmlDescriptor);
            else
                throw new DeploymentUnitProcessingException("Failed to parse service xml [" + serviceXmlFile + "]", null);
        } catch(Exception e) {
            throw new DeploymentUnitProcessingException("Failed to parse service xml [" + serviceXmlFile + "]", e, new Location(e.getStackTrace()[0].getFileName(), e.getStackTrace()[0].getLineNumber(), -1, null));
        } finally {
            VFSUtils.safeClose(xmlStream);
        }
    }
}
