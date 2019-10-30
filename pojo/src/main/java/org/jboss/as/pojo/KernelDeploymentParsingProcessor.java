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

package org.jboss.as.pojo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.pojo.descriptor.KernelDeploymentXmlDescriptor;
import org.jboss.as.pojo.descriptor.KernelDeploymentXmlDescriptorParser;
import org.jboss.as.pojo.descriptor.LegacyKernelDeploymentXmlDescriptorParser;
import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.staxmapper.XMLMapper;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.util.SuffixMatchFilter;

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
        final KernelDeploymentXmlDescriptorParser parser = new KernelDeploymentXmlDescriptorParser();
        xmlMapper.registerRootElement(new QName(KernelDeploymentXmlDescriptorParser.NAMESPACE, "deployment"), parser);
        // old MC parser -- just a warning / info atm
        final LegacyKernelDeploymentXmlDescriptorParser legacy = new LegacyKernelDeploymentXmlDescriptorParser();
        xmlMapper.registerRootElement(new QName(LegacyKernelDeploymentXmlDescriptorParser.MC_NAMESPACE_1_0, "deployment"), legacy);
        xmlMapper.registerRootElement(new QName(LegacyKernelDeploymentXmlDescriptorParser.MC_NAMESPACE_2_0, "deployment"), legacy);
    }

    /**
     * Process a deployment for jboss-beans.xml files.
     * Will parse the xml file and attach a configuration discovered during processing.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     *
     */
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final VirtualFile deploymentRoot = unit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        parseDescriptors(unit, deploymentRoot);
        final List<ResourceRoot> resourceRoots = unit.getAttachmentList(Attachments.RESOURCE_ROOTS);
        for (ResourceRoot root : resourceRoots)
            parseDescriptors(unit, root.getRoot());
    }

    /**
     * Find and parse -jboss-beans.xml files.
     *
     * @param unit the deployment unit
     * @param root the root
     * @throws DeploymentUnitProcessingException
     *          for any error
     */
    protected void parseDescriptors(DeploymentUnit unit, VirtualFile root) throws DeploymentUnitProcessingException {
        if (root == null || root.exists() == false)
            return;

        Collection<VirtualFile> beans;
        final String name = root.getName();
        if (name.endsWith("jboss-beans.xml")) {
            beans = Collections.singleton(root);
        } else {
            VirtualFileFilter filter = new SuffixMatchFilter("jboss-beans.xml");
            beans = new ArrayList<VirtualFile>();
            try {
                // try plain .jar/META-INF
                VirtualFile metainf = root.getChild("META-INF");
                if (metainf.exists())
                    beans.addAll(metainf.getChildren(filter));

                // allow for WEB-INF/*-jboss-beans.xml
                VirtualFile webinf = root.getChild("WEB-INF");
                if (webinf.exists()) {
                    beans.addAll(webinf.getChildren(filter));

                    // allow WEB-INF/classes/META-INF
                    metainf = webinf.getChild("classes/META-INF");
                    if (metainf.exists())
                        beans.addAll(metainf.getChildren(filter));
                }
            } catch (IOException e) {
                throw new DeploymentUnitProcessingException(e);
            }
        }
        for (VirtualFile beansXmlFile : beans)
            parseDescriptor(unit, beansXmlFile);
    }

    /**
     * Parse -jboss-beans.xml file.
     *
     * @param unit         the deployment unit
     * @param beansXmlFile the beans xml file
     * @throws DeploymentUnitProcessingException
     *          for any error
     */
    protected void parseDescriptor(DeploymentUnit unit, VirtualFile beansXmlFile) throws DeploymentUnitProcessingException {
        if (beansXmlFile == null || beansXmlFile.exists() == false)
            return;

        InputStream xmlStream = null;
        try {
            xmlStream = beansXmlFile.openStream();
            final XMLStreamReader reader = inputFactory.createXMLStreamReader(xmlStream);
            final ParseResult<KernelDeploymentXmlDescriptor> result = new ParseResult<KernelDeploymentXmlDescriptor>();
            xmlMapper.parseDocument(result, reader);
            final KernelDeploymentXmlDescriptor xmlDescriptor = result.getResult();
            if (xmlDescriptor != null)
                unit.addToAttachmentList(KernelDeploymentXmlDescriptor.ATTACHMENT_KEY, xmlDescriptor);
            else
                throw PojoLogger.ROOT_LOGGER.failedToParse(beansXmlFile);
        } catch (DeploymentUnitProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw PojoLogger.ROOT_LOGGER.parsingException(beansXmlFile, e);
        } finally {
            VFSUtils.safeClose(xmlStream);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
