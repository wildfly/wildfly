/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.module;

import static java.util.jar.Attributes.Name.EXTENSION_LIST;
import static java.util.jar.Attributes.Name.EXTENSION_NAME;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_URL;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_VENDOR_ID;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION;
import static java.util.jar.Attributes.Name.SPECIFICATION_VERSION;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.Services;

/**
 * A processor which adds class path entries for each manifest entry.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Stuart Douglas
 */
public final class ManifestExtensionListProcessor implements DeploymentUnitProcessor {

    /** {@inheritDoc} */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final List<ResourceRoot> resourceRoots = DeploymentUtils.allResourceRoots(deploymentUnit);
        for (ResourceRoot resourceRoot : resourceRoots) {
            final Manifest manifest = resourceRoot.getAttachment(Attachments.MANIFEST);
            if (manifest == null) {
                // no class path to process!
                continue;
            }
            final Attributes mainAttributes = manifest.getMainAttributes();
            final String extensionListString = mainAttributes.getValue(EXTENSION_LIST);
            if (extensionListString == null) {
                // no entry
                continue;
            }
            final String[] items = extensionListString.split("\\s+");
            boolean added = false;
            for (String item : items) {
                final String extensionName = mainAttributes.getValue(item + "-" + EXTENSION_NAME);
                if (extensionName == null) {
                    ServerLogger.DEPLOYMENT_LOGGER.extensionMissingManifestAttribute(item, item, EXTENSION_NAME);
                    continue;
                }
                final String specificationVersion = mainAttributes.getValue(item + "-" + SPECIFICATION_VERSION);
                final String implementationVersion = mainAttributes.getValue(item + "-" + IMPLEMENTATION_VERSION);
                final String implementationVendorId = mainAttributes.getValue(item + "-" + IMPLEMENTATION_VENDOR_ID);
                final String implementationUrl = mainAttributes.getValue(item + "-" + IMPLEMENTATION_URL);
                URI implementationUri = null;
                if (implementationUrl == null) {
                    ServerLogger.DEPLOYMENT_LOGGER.debugf("Extension %s is missing the required manifest attribute %s-%s", item, item, IMPLEMENTATION_URL);
                } else {
                    try {
                        implementationUri = new URI(implementationUrl);
                    } catch (URISyntaxException e) {
                        ServerLogger.DEPLOYMENT_LOGGER.invalidExtensionURI(item, e);
                    }
                }
                resourceRoot.addToAttachmentList(Attachments.EXTENSION_LIST_ENTRIES, new ExtensionListEntry(item,
                        extensionName, specificationVersion, implementationVersion, implementationVendorId, implementationUri));
                added = true;
            }
            if (added) {
                // Require the extension list before we proceed
                phaseContext.addToAttachmentList(Attachments.NEXT_PHASE_DEPS, Services.JBOSS_DEPLOYMENT_EXTENSION_INDEX);
            }
        }
    }

    /** {@inheritDoc} */
    public void undeploy(final DeploymentUnit context) {
    }
}
