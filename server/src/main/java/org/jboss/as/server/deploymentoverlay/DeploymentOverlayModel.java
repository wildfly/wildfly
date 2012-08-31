package org.jboss.as.server.deploymentoverlay;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;

/**
 * @author Stuart Douglas
 */
public class DeploymentOverlayModel {

    private static final String RESOURCE_NAME = DeploymentOverlayModel.class.getPackage().getName() + ".LocalDescriptions";

    protected static final PathElement CONTENT_PATH = PathElement.pathElement(ModelDescriptionConstants.CONTENT);
    protected static final PathElement DEPLOYMENT_OVERRIDE_PATH = PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT_OVERLAY);
    protected static final PathElement DEPLOYMENT_OVERRIDE_DEPLOYMENT_PATH = PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT);

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, DeploymentOverlayModel.class.getClassLoader(), true, false);
    }
}
