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

    static final String WAR_EXTENSION = ".war";
    static final String WAB_EXTENSION = ".wab";

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        String depName = depUnit.getName().toLowerCase(Locale.ENGLISH);
        if(depName.endsWith(WAR_EXTENSION) || depName.endsWith(WAB_EXTENSION)) {
            MountExplodedMarker.setMountExploded(depUnit);
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
