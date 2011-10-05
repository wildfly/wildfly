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
package org.jboss.as.appclient.deployment;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.appclient.parser.spec.ApplicationClientMetaDataParser;
import org.jboss.metadata.appclient.spec.ApplicationClientMetaData;
import org.jboss.metadata.parser.util.NoopXmlResolver;
import org.jboss.vfs.VirtualFile;

/**
 * @author Stuart Douglas
 */
public class ApplicationClientParsingDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String APP_XML = "META-INF/application-client.xml";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.APPLICATION_CLIENT, deploymentUnit)) {
            return;
        }
        final VirtualFile deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        final VirtualFile appXml = deploymentRoot.getChild(APP_XML);
        if (appXml.exists()) {
            InputStream is = null;
            try {
                is = appXml.openStream();
                final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                inputFactory.setXMLResolver(NoopXmlResolver.create());
                XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);
                ApplicationClientMetaData data = new ApplicationClientMetaDataParser().parse(xmlReader);
                deploymentUnit.putAttachment(AppClientAttachments.APPLICATION_CLIENT_META_DATA, data);
                DeploymentDescriptorEnvironment environment = new DeploymentDescriptorEnvironment("java:module/env",data.getEnvironmentRefsGroupMetaData() );
                deploymentUnit.putAttachment(org.jboss.as.ee.component.Attachments.MODULE_DEPLOYMENT_DESCRIPTOR_ENVIRONMENT, environment);

            } catch (XMLStreamException e) {
                throw new DeploymentUnitProcessingException("Failed to parse " + appXml + " at [" + e.getLocation().getLineNumber() + "," +  e.getLocation().getColumnNumber() + "]");
            } catch (IOException e) {
                throw new DeploymentUnitProcessingException("Failed to parse " + appXml, e);
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

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
