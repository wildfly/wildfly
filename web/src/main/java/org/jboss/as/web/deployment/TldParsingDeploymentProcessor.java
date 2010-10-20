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
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.attachment.VirtualFileAttachment;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.metadata.parser.jsp.TldMetaDataParser;
import org.jboss.as.web.deployment.helpers.DeploymentStructure;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.spec.TldMetaData;
import org.jboss.vfs.VirtualFile;

/**
 * @author Remy Maucherat
 */
public class TldParsingDeploymentProcessor implements DeploymentUnitProcessor {

    public static final long PRIORITY = DeploymentPhases.PARSE_DESCRIPTORS.plus(330L);

    private static final String TLD = ".tld";
    private static final String META_INF = "META-INF";
    private static final String WEB_INF = "WEB-INF";
    private static final String CLASSES = "classes";
    private static final String LIB = "lib";
    private static final String IMPLICIT_TLD = "implicit.tld";

    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = VirtualFileAttachment.getVirtualFileAttachment(context);
        TldsMetaData tldsMetaData = context.getAttachment(TldsMetaData.ATTACHMENT_KEY);
        if (tldsMetaData == null) {
            tldsMetaData = new TldsMetaData();
            context.putAttachment(TldsMetaData.ATTACHMENT_KEY, tldsMetaData);
        }
        Map<String, TldMetaData> tlds = new HashMap<String, TldMetaData>();
        tldsMetaData.setTlds(tlds);
        // TLDs are located in WEB-INF or any subdir (except the top level "classes" and "lib")
        // and in JARs from WEB-INF/lib, in META-INF or any subdir
        DeploymentStructure structure = context.getAttachment(DeploymentStructure.ATTACHMENT_KEY);
        assert structure != null;
        assert structure.getEntries() != null;
        for (DeploymentStructure.ClassPathEntry resourceRoot : structure.getEntries()) {
            if (resourceRoot.getRoot().getLowerCaseName().endsWith(".jar")) {
                VirtualFile webFragment = resourceRoot.getRoot().getChild(META_INF);
                if (webFragment.exists() && webFragment.isDirectory()) {
                    processTlds(deploymentRoot, webFragment.getChildren(), tlds);
                }
            }
        }
        VirtualFile webInf = deploymentRoot.getChild(WEB_INF);
        if (webInf.exists() && webInf.isDirectory()) {
            for (VirtualFile file : webInf.getChildren()) {
                if (file.isFile() && file.getName().endsWith(TLD)) {
                    tlds.put(file.getPathNameRelativeTo(deploymentRoot), parseTLD(file));
                } else if (file.isDirectory() && !CLASSES.equals(file.getName()) && !LIB.equals(file.getName())) {
                    processTlds(deploymentRoot, file.getChildren(), tlds);
                }
            }
        }
    }

    private void processTlds(VirtualFile root, List<VirtualFile> files, Map<String, TldMetaData> tlds)
    throws DeploymentUnitProcessingException {
        for (VirtualFile file : files) {
            if (file.isFile() && file.getName().endsWith(TLD)) {
                tlds.put(file.getPathNameRelativeTo(root), parseTLD(file));
            } else if (file.isDirectory()) {
                processTlds(root, file.getChildren(), tlds);
            }
        }
    }

    private TldMetaData parseTLD(VirtualFile tld)
    throws DeploymentUnitProcessingException {
        if (IMPLICIT_TLD.equals(tld.getName())) {
            // Implicit TLDs are different from regular TLDs
            return new TldMetaData();
        }
        InputStream is = null;
        long time = System.currentTimeMillis();
        try {
            is = tld.openStream();
            final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);
            return TldMetaDataParser.parse(xmlReader);
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException("Failed to parse " + tld, e);
        } finally {
            Logger.getLogger("org.jboss.web.TldParsingDeploymentProcessor").info("parse " + tld.getName() + ": " + (System.currentTimeMillis() - time) + "ms");
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
