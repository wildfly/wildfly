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

package org.jboss.as.service;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ee.structure.JBossDescriptorPropertyReplacement;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.as.service.descriptor.JBossServiceXmlDescriptor;
import org.jboss.as.service.descriptor.JBossServiceXmlDescriptorParser;
import org.jboss.as.service.descriptor.ParseResult;
import org.jboss.logging.Messages;
import org.jboss.staxmapper.XMLMapper;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;

/**
 * DeploymentUnitProcessor responsible for parsing a jboss-service.xml descriptor and attaching the corresponding
 * JBossServiceXmlDescriptor.
 *
 * @author John E. Bailey
 */
public class ServiceDeploymentParsingProcessor implements DeploymentUnitProcessor {
    static final String SERVICE_DESCRIPTOR_PATH = "META-INF/jboss-service.xml";
    static final String SERVICE_DESCRIPTOR_SUFFIX = "-service.xml";

    private static final String JAR_EXTENSION = ".jar";

    private final XMLInputFactory inputFactory = XMLInputFactory.newInstance();

    private static final SuffixMatchFilter CHILD_ARCHIVE_FILTER = new SuffixMatchFilter(JAR_EXTENSION,
            VisitorAttributes.RECURSE_LEAVES_ONLY);

    private static Closeable NO_OP_CLOSEABLE = new Closeable() {
        public void close() throws IOException {
            // NO-OP
        }
    };

    /**
     * Construct a new instance.
     */
    public ServiceDeploymentParsingProcessor() {
    }

    /**
     * Process a deployment for jboss-service.xml files. Will parse the xml file and attach an configuration discovered durring
     * processing.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final VirtualFile deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        final ResourceRoot resourceRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

        if (deploymentRoot == null || !deploymentRoot.exists())
            return;

        VirtualFile serviceXmlFile = null;
        if (deploymentRoot.isDirectory()) {
            serviceXmlFile = deploymentRoot.getChild(SERVICE_DESCRIPTOR_PATH);
        } else if (deploymentRoot.getName().toLowerCase(Locale.ENGLISH).endsWith(SERVICE_DESCRIPTOR_SUFFIX)) {
            serviceXmlFile = deploymentRoot;
        }
        if (serviceXmlFile == null || !serviceXmlFile.exists())
            return;

        final XMLMapper xmlMapper = XMLMapper.Factory.create();
        final JBossServiceXmlDescriptorParser jBossServiceXmlDescriptorParser = new JBossServiceXmlDescriptorParser(
                JBossDescriptorPropertyReplacement.propertyReplacer(phaseContext.getDeploymentUnit()));
        xmlMapper.registerRootElement(new QName("urn:jboss:service:7.0", "server"), jBossServiceXmlDescriptorParser);
        xmlMapper.registerRootElement(new QName(null, "server"), jBossServiceXmlDescriptorParser);

        InputStream xmlStream = null;
        try {
            xmlStream = serviceXmlFile.openStream();
            final XMLStreamReader reader = inputFactory.createXMLStreamReader(xmlStream);
            final ParseResult<JBossServiceXmlDescriptor> result = new ParseResult<JBossServiceXmlDescriptor>();
            xmlMapper.parseDocument(result, reader);
            final JBossServiceXmlDescriptor xmlDescriptor = result.getResult();
            if (xmlDescriptor != null)
                phaseContext.getDeploymentUnit().putAttachment(JBossServiceXmlDescriptor.ATTACHMENT_KEY, xmlDescriptor);
            else
                throw SarMessages.MESSAGES.failedXmlParsing(serviceXmlFile);
        } catch (Exception e) {
            throw SarMessages.MESSAGES.failedXmlParsing(e, serviceXmlFile);
        } finally {
            VFSUtils.safeClose(xmlStream);
        }
        try {
            final List<VirtualFile> childArchives = deploymentRoot.getChildren(CHILD_ARCHIVE_FILTER);

            for (final VirtualFile child : childArchives) {
                final Closeable closable = child.isFile() ? VFS.mountZip(child, child, TempFileProviderService.provider())
                        : NO_OP_CLOSEABLE;
                final MountHandle mountHandle = new MountHandle(closable);
                final ResourceRoot childResource = new ResourceRoot(child, mountHandle);
                ModuleRootMarker.mark(childResource);
                deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, childResource);
                resourceRoot.addToAttachmentList(Attachments.INDEX_IGNORE_PATHS, child.getPathNameRelativeTo(deploymentRoot));
            }
        } catch (IOException e) {
            SarMessages.MESSAGES.failedToProcessSarChild(e, deploymentRoot);
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
