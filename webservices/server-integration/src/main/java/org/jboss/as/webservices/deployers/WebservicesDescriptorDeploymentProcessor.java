/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.webservices.deployers;

import java.io.IOException;
import java.net.URL;

import org.jboss.as.ee.structure.JBossDescriptorPropertyReplacement;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.metadata.WebservicesPropertyReplaceFactory;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.vfs.VirtualFile;
import org.jboss.wsf.spi.metadata.webservices.WebserviceDescriptionMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * DUP for parsing webservices.xml
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WebservicesDescriptorDeploymentProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final ResourceRoot deploymentRoot = unit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final URL webservicesDescriptorURL = getWebServicesDescriptorURL(deploymentRoot);

        if (webservicesDescriptorURL != null) {
            final WebservicesPropertyReplaceFactory webservicesFactory = new WebservicesPropertyReplaceFactory(
                    webservicesDescriptorURL, JBossDescriptorPropertyReplacement.propertyReplacer(unit));
            final WebservicesMetaData webservicesMD = webservicesFactory.load(webservicesDescriptorURL);
            unit.putAttachment(WSAttachmentKeys.WEBSERVICES_METADATA_KEY, webservicesMD);
            if (hasJaxRpcMapping(webservicesMD)) {
                throw WSLogger.ROOT_LOGGER.jaxRpcNotSupported();
            }
        }
    }

    public void undeploy(final DeploymentUnit unit) {
        // does nothing
    }

    private URL getWebServicesDescriptorURL(final ResourceRoot deploymentRoot) throws DeploymentUnitProcessingException {
        VirtualFile wsdd = deploymentRoot.getRoot().getChild("WEB-INF/webservices.xml");

        if (!wsdd.exists()) {
            wsdd = deploymentRoot.getRoot().getChild("META-INF/webservices.xml");
        }

        try {
            return wsdd.exists() ? wsdd.toURL() : null;
        } catch (IOException e) {
            throw WSLogger.ROOT_LOGGER.cannotGetURLForDescriptor(e, wsdd.getPathName());
        }
    }

    private boolean hasJaxRpcMapping(WebservicesMetaData webservicesMD) {
        for (WebserviceDescriptionMetaData wsdmd : webservicesMD.getWebserviceDescriptions()) {
            if (wsdmd.getJaxrpcMappingFile() != null) {
                return true;
            }
        }
        return false;
    }

}
