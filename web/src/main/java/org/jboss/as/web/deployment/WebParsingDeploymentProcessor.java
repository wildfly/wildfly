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

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.metadata.parser.servlet.WebMetaDataParser;
import org.jboss.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.metadata.parser.util.XMLResourceResolver;
import org.jboss.metadata.parser.util.XMLSchemaValidator;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.vfs.VirtualFile;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Jean-Frederic Clere
 * @author Thomas.Diesler@jboss.com
 */
public class WebParsingDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String WEB_XML = "WEB-INF/web.xml";
    private final boolean schemaValidation;

    public WebParsingDeploymentProcessor() {
        String property = SecurityActions.getSystemProperty(XMLSchemaValidator.PROPERTY_SCHEMA_VALIDATION, "false");
        this.schemaValidation = Boolean.parseBoolean(property);
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return; // Skip non web deployments
        }
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final VirtualFile alternateDescriptor = deploymentRoot.getAttachment(org.jboss.as.ee.structure.Attachments.ALTERNATE_WEB_DEPLOYMENT_DESCRIPTOR);
        // Locate the descriptor
        final VirtualFile webXml;
        if (alternateDescriptor != null) {
            webXml = alternateDescriptor;
        } else {
            webXml = deploymentRoot.getRoot().getChild(WEB_XML);
        }
        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        assert warMetaData != null;
        if (webXml.exists()) {
            InputStream is = null;
            try {
                is = webXml.openStream();
                final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                inputFactory.setXMLResolver(NoopXMLResolver.create());
                final XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);

                MetaDataElementParser.DTDInfo dtdInfo = new MetaDataElementParser.DTDInfo();
                WebMetaData webMetaData = WebMetaDataParser.parse(xmlReader, dtdInfo);

                if (schemaValidation && webMetaData.getSchemaLocation() != null) {
                    XMLSchemaValidator validator = new XMLSchemaValidator(new XMLResourceResolver());
                    InputStream xmlInput = webXml.openStream();
                    try {
                        validator.validate(webMetaData.getSchemaLocation(), xmlInput);
                    } catch (SAXException e) {
                        throw new DeploymentUnitProcessingException("Failed to validate " + webXml, e);
                    } finally {
                        xmlInput.close();
                    }
                }
                warMetaData.setWebMetaData(webMetaData);

            } catch (XMLStreamException e) {
                throw new DeploymentUnitProcessingException("Failed to parse " + webXml + " at [" + e.getLocation().getLineNumber() + "," + e.getLocation().getColumnNumber() + "]");
            } catch (IOException e) {
                throw new DeploymentUnitProcessingException("Failed to parse " + webXml, e);
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
    public void undeploy(final DeploymentUnit context) {
    }
}
