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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.deployment.Attachments;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentPhaseContext;
import org.jboss.as.metadata.parser.jbossweb.JBossWebMetaDataParser;
import org.jboss.as.metadata.parser.util.NoopXmlResolver;
import static org.jboss.as.web.deployment.WarDeploymentMarker.isWarDeployment;
import org.jboss.vfs.VirtualFile;

/**
 * @author Jean-Frederic Clere
 */
public class JBossWebParsingDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String JBOSS_WEB_XML = "WEB-INF/jboss-web.xml";

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        if(!isWarDeployment(phaseContext)) {
            return; // Skip non web deployments
        }
        final VirtualFile deploymentRoot = phaseContext.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final VirtualFile jbossWebXml = deploymentRoot.getChild(JBOSS_WEB_XML);
        WarMetaData warMetaData = phaseContext.getAttachment(WarMetaData.ATTACHMENT_KEY);
        assert warMetaData != null;
        if (jbossWebXml.exists()) {
            InputStream is = null;
            try {
                is = jbossWebXml.openStream();
                final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                inputFactory.setXMLResolver(NoopXmlResolver.create());
                XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);
                warMetaData.setJbossWebMetaData(JBossWebMetaDataParser.parse(xmlReader));
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException("Failed to parse " + jbossWebXml, e);
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
