/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.structure.SpecDescriptorPropertyReplacement;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.parser.servlet.WebFragmentMetaDataParser;
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.metadata.web.spec.WebFragmentMetaData;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author Remy Maucherat
 */
public class WebFragmentParsingDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String WEB_FRAGMENT_XML = "META-INF/web-fragment.xml";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return; // Skip non web deployments
        }
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        assert warMetaData != null;
        Map<String, WebFragmentMetaData> webFragments = warMetaData.getWebFragmentsMetaData();
        if (webFragments == null) {
            webFragments = new HashMap<String, WebFragmentMetaData>();
            warMetaData.setWebFragmentsMetaData(webFragments);
        }
        List<ResourceRoot> resourceRoots = deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS);
        for (ResourceRoot resourceRoot : resourceRoots) {
            if (resourceRoot.getRoot().getName().toLowerCase(Locale.ENGLISH).endsWith(".jar")) {
                VirtualFile webFragment = resourceRoot.getRoot().getChild(WEB_FRAGMENT_XML);
                if (webFragment.exists() && webFragment.isFile()) {
                    InputStream is = null;
                    try {
                        is = webFragment.openStream();
                        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                        inputFactory.setXMLResolver(NoopXMLResolver.create());
                        XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);

                        WebFragmentMetaData webFragmentMetaData = WebFragmentMetaDataParser.parse(xmlReader, SpecDescriptorPropertyReplacement.propertyReplacer(deploymentUnit));
                        webFragments.put(resourceRoot.getRootName(), webFragmentMetaData);
                        /*Log message to inform that distributable is not set in web-fragment.xml while it is set in web.xml*/
                        if (warMetaData.getWebMetaData() != null && warMetaData.getWebMetaData().getDistributable()!= null && webFragmentMetaData.getDistributable() == null)
                            UndertowLogger.ROOT_LOGGER.distributableDisabledInFragmentXml(deploymentUnit.getName(),resourceRoot.getRootName());
                    } catch (XMLStreamException e) {
                        throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.failToParseXMLDescriptor(webFragment.toString(), e.getLocation().getLineNumber(), e.getLocation().getColumnNumber()));
                    } catch (IOException e) {
                        throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.failToParseXMLDescriptor(webFragment.toString()), e);
                    } finally {
                        try {
                            if (is != null) {
                                is.close();
                            }
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                }
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
