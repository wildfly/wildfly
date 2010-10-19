/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.web.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.metadata.parser.servlet.WebFragmentMetaDataParser;
import org.jboss.as.web.deployment.helpers.DeploymentStructure;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.spec.WebFragmentMetaData;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;

/**
 * @author Remy Maucherat
 */
public class WebFragmentParsingDeploymentProcessor implements DeploymentUnitProcessor {

    public static final long PRIORITY = DeploymentPhases.PARSE_DESCRIPTORS.plus(310L);

    private static final String WEB_FRAGMENT_XML = "META-INF/web-fragment.xml";

    public static final VirtualFileFilter DEFAULT_WEB_INF_LIB_FILTER = new SuffixMatchFilter(".jar", VisitorAttributes.DEFAULT);

    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        WarMetaData warMetaData = context.getAttachment(WarMetaData.ATTACHMENT_KEY);
        assert warMetaData != null;
        Map<String, WebFragmentMetaData> webFragments = warMetaData.getWebFragmentsMetaData();
        if (webFragments == null) {
            webFragments = new HashMap<String, WebFragmentMetaData>();
            warMetaData.setWebFragmentsMetaData(webFragments);
        }
        DeploymentStructure structure = context.getAttachment(DeploymentStructure.ATTACHMENT_KEY);
        assert structure != null;
        assert structure.getEntries() != null;
        for (DeploymentStructure.ClassPathEntry resourceRoot : structure.getEntries()) {
            if (resourceRoot.getRoot().isFile()) {
                VirtualFile webFragment = resourceRoot.getRoot().getChild(WEB_FRAGMENT_XML);
                if (webFragment.exists() && webFragment.isFile()) {
                    InputStream is = null;
                    long time = System.currentTimeMillis();
                    try {
                        is = webFragment.openStream();
                        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                        XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);
                        webFragments.put(resourceRoot.getName(), WebFragmentMetaDataParser.parse(xmlReader));
                    } catch (Exception e) {
                        throw new DeploymentUnitProcessingException("Failed to parse " + webFragment, e);
                    } finally {
                        Logger.getLogger("org.jboss.web").info("parse " + (System.currentTimeMillis() - time));
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

}
