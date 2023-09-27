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
import org.jboss.as.webservices.metadata.JBossWebservicesPropertyReplaceFactory;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.vfs.VirtualFile;
import org.jboss.wsf.spi.metadata.webservices.JBossWebservicesMetaData;

/**
 * DUP for parsing jboss-webservices.xml
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class JBossWebservicesDescriptorDeploymentProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final ResourceRoot deploymentRoot = unit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final URL jbossWebservicesDescriptorURL = getJBossWebServicesDescriptorURL(deploymentRoot);

        if (jbossWebservicesDescriptorURL != null) {
            final JBossWebservicesPropertyReplaceFactory webservicesFactory = new JBossWebservicesPropertyReplaceFactory(
                    jbossWebservicesDescriptorURL, JBossDescriptorPropertyReplacement.propertyReplacer(unit));
            final JBossWebservicesMetaData jbossWebservicesMD = webservicesFactory.load(jbossWebservicesDescriptorURL);
            unit.putAttachment(WSAttachmentKeys.JBOSS_WEBSERVICES_METADATA_KEY, jbossWebservicesMD);
        }
    }

    private URL getJBossWebServicesDescriptorURL(final ResourceRoot deploymentRoot) throws DeploymentUnitProcessingException {
        VirtualFile jwsdd = deploymentRoot.getRoot().getChild("WEB-INF/jboss-webservices.xml");

        if (!jwsdd.exists()) {
            jwsdd = deploymentRoot.getRoot().getChild("META-INF/jboss-webservices.xml");
        }

        try {
            return jwsdd.exists() ? jwsdd.toURL() : null;
        } catch (IOException e) {
            throw WSLogger.ROOT_LOGGER.cannotGetURLForDescriptor(e, jwsdd.getPathName());
        }
    }

}
