/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.controller.xml.XMLElementSchema;
import org.jboss.as.pojo.descriptor.BeanDeploymentSchema;
import org.jboss.as.pojo.descriptor.KernelDeploymentXmlDescriptor;
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

    private final XMLMapper xmlMapper = XMLElementSchema.createXMLMapper(EnumSet.allOf(BeanDeploymentSchema.class));
    private final XMLInputFactory inputFactory = XMLInputFactory.newInstance();

    public KernelDeploymentParsingProcessor() {
        setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        setIfSupported(inputFactory, XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        //NoopXMLResolver resolver not in deps, neither XMLInputFactoryUtil
    }

    private void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
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
}
