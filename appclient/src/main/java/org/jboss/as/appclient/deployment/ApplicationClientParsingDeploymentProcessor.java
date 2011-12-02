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
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.metadata.appclient.jboss.JBossClientMetaData;
import org.jboss.metadata.appclient.parser.jboss.JBossClientMetaDataParser;
import org.jboss.metadata.appclient.parser.spec.ApplicationClientMetaDataParser;
import org.jboss.metadata.appclient.spec.AppClientEnvironmentRefsGroupMetaData;
import org.jboss.metadata.appclient.spec.ApplicationClientMetaData;
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.vfs.VirtualFile;

import static org.jboss.as.appclient.logging.AppClientMessages.MESSAGES;

/**
 * @author Stuart Douglas
 */
public class ApplicationClientParsingDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String APP_XML = "META-INF/application-client.xml";
    private static final String JBOSS_CLIENT_XML = "META-INF/jboss-client.xml";

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.APPLICATION_CLIENT, deploymentUnit)) {
            return;
        }
        final ApplicationClientMetaData appClientMD = parseAppClient(deploymentUnit);
        final JBossClientMetaData jbossClientMD = parseJBossClient(deploymentUnit);
        final JBossClientMetaData merged;
        if (appClientMD == null && jbossClientMD == null) {
            return;
        } else if (appClientMD == null) {
            merged = jbossClientMD;
        } else {
            merged = new JBossClientMetaData();
            merged.setEnvironmentRefsGroupMetaData(new AppClientEnvironmentRefsGroupMetaData());
            merged.merge(jbossClientMD, appClientMD);
        }
        if(merged.isMetadataComplete()) {
            MetadataCompleteMarker.setMetadataComplete(deploymentUnit, true);
        }
        deploymentUnit.putAttachment(AppClientAttachments.APPLICATION_CLIENT_META_DATA, merged);
        final DeploymentDescriptorEnvironment environment = new DeploymentDescriptorEnvironment("java:module/env/", merged.getEnvironmentRefsGroupMetaData());
        deploymentUnit.putAttachment(org.jboss.as.ee.component.Attachments.MODULE_DEPLOYMENT_DESCRIPTOR_ENVIRONMENT, environment);
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

    private ApplicationClientMetaData parseAppClient(DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final VirtualFile alternateDescriptor = deploymentRoot.getAttachment(org.jboss.as.ee.structure.Attachments.ALTERNATE_CLIENT_DEPLOYMENT_DESCRIPTOR);
        // Locate the descriptor
        final VirtualFile descriptor;
        if (alternateDescriptor != null) {
            descriptor = alternateDescriptor;
        } else {
            descriptor = deploymentRoot.getRoot().getChild(APP_XML);
        }
        if (descriptor.exists()) {
            InputStream is = null;
            try {
                is = descriptor.openStream();
                ApplicationClientMetaData data = new ApplicationClientMetaDataParser().parse(getXMLStreamReader(is));
                return data;
            } catch (XMLStreamException e) {
                throw MESSAGES.failedToParseXml(e, descriptor, e.getLocation().getLineNumber(), e.getLocation().getColumnNumber());
            } catch (IOException e) {
                throw new DeploymentUnitProcessingException("Failed to parse " + descriptor, e);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
        } else {
            return null;
        }
    }

    private JBossClientMetaData parseJBossClient(DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        final VirtualFile appXml = deploymentRoot.getChild(JBOSS_CLIENT_XML);
        if (appXml.exists()) {
            InputStream is = null;
            try {
                is = appXml.openStream();
                JBossClientMetaData data = new JBossClientMetaDataParser().parse(getXMLStreamReader(is));
                return data;
            } catch (XMLStreamException e) {
                throw MESSAGES.failedToParseXml(e, appXml, e.getLocation().getLineNumber(), e.getLocation().getColumnNumber());

            } catch (IOException e) {
                throw MESSAGES.failedToParseXml(e, appXml);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
        } else {
            return null;
        }
    }

    private XMLStreamReader getXMLStreamReader(InputStream is) throws XMLStreamException {
        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setXMLResolver(NoopXMLResolver.create());
        XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);
        return xmlReader;
    }
}
