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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

import static java.util.jar.Attributes.Name.*;

/**
 * A processor which adds class path entries for each manifest entry.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ManifestExtensionListProcessor implements DeploymentUnitProcessor {

    /** {@inheritDoc} */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final Manifest manifest = phaseContext.getAttachment(Attachments.MANIFEST);
        if (manifest == null) {
            // no class path to process!
            return;
        }
        final Attributes mainAttributes = manifest.getMainAttributes();
        final String extensionListString = mainAttributes.getValue(EXTENSION_LIST);
        if (extensionListString == null) {
            // no entry
            return;
        }
        final String[] items = extensionListString.split("\\s+");
        for (String item : items) {
            final String extensionName = mainAttributes.getValue(item + "-" + EXTENSION_NAME);
            if (extensionName == null) {
                throw new DeploymentUnitProcessingException("Missing required manifest attribute: " + item + "-" + EXTENSION_NAME);
            }
            final String specificationVersion = mainAttributes.getValue(item + "-" + SPECIFICATION_VERSION);
            final String implementationVersion = mainAttributes.getValue(item + "-" + IMPLEMENTATION_VERSION);
            final String implementationVendorId = mainAttributes.getValue(item + "-" + IMPLEMENTATION_VENDOR_ID);
            final String implementationUrl = mainAttributes.getValue(item + "-" + IMPLEMENTATION_URL);
            if (implementationUrl == null) {
                throw new DeploymentUnitProcessingException("Missing required manifest attribute: " + item + "-" + IMPLEMENTATION_URL);
            }
            try {
                phaseContext.addToAttachmentList(Attachments.EXTENSION_LIST_ENTRIES, new ExtensionListEntry(item, extensionName, specificationVersion, implementationVersion, implementationVendorId, new URI(implementationUrl)));
            } catch (URISyntaxException e) {
                throw new DeploymentUnitProcessingException("Invalid value given for manifest attribute: " + item + "-" + IMPLEMENTATION_URL, e);
            }
        }
    }

    /** {@inheritDoc} */
    public void undeploy(final DeploymentUnit context) {
    }
}
