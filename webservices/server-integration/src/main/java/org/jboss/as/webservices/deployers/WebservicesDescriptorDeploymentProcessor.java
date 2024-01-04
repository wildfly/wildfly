/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
