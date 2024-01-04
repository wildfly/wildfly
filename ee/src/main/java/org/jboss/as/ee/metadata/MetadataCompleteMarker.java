/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.metadata;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Marker class used to set/get the metadata-complete status of the deployment.
 *
 * @author Stuart Douglas
 */
public class MetadataCompleteMarker {

    private static final AttachmentKey<Boolean> KEY = AttachmentKey.create(Boolean.class);

    public static void setMetadataComplete(final DeploymentUnit deploymentUnit, final boolean value) {
        deploymentUnit.putAttachment(KEY, value);
    }

    public static boolean isMetadataComplete(final DeploymentUnit deploymentUnit) {
        final Boolean marker = deploymentUnit.getAttachment(KEY);
        return marker != null && marker;
    }

    private MetadataCompleteMarker() {
    }
}
