/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.util.Locale;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.MountExplodedMarker;

/**
 * Processor that marks a deployment as exploded.
 *
 * @author Thomas.Diesler@jboss.com
 * @since  05-Oct-2011
 */
public class DeploymentRootExplodedMountProcessor implements DeploymentUnitProcessor {

    private static final String WAR_EXTENSION = ".war";

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        String depName = depUnit.getName().toLowerCase(Locale.ENGLISH);
        if (depName.endsWith(WAR_EXTENSION)) {
            MountExplodedMarker.setMountExploded(depUnit);
        }
    }
}
