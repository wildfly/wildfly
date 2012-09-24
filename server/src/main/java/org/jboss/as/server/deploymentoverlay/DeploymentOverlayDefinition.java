package org.jboss.as.server.deploymentoverlay;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.DeploymentFileRepository;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayPriority;

/**
 * @author Stuart Douglas
 */
public class DeploymentOverlayDefinition extends SimpleResourceDefinition {

    private static final AttributeDefinition[] ATTRIBUTES = { };

    public static AttributeDefinition[] attributes() {
        return ATTRIBUTES.clone();
    }

    private final DeploymentOverlayPriority priority;
    private final ContentRepository contentRepo;
    private final DeploymentFileRepository fileRepository;


    public DeploymentOverlayDefinition(DeploymentOverlayPriority priority, ContentRepository contentRepo, DeploymentFileRepository fileRepository) {
        super(DeploymentOverlayModel.DEPLOYMENT_OVERRIDE_PATH,
                CommonDescriptions.getResourceDescriptionResolver(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, false),
                DeploymentOverlayAdd.INSTANCE,
                DeploymentOverlayRemove.INSTANCE);
        this.priority = priority;
        this.contentRepo = contentRepo;
        this.fileRepository = fileRepository;
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attr, null);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        if (contentRepo != null) {
            resourceRegistration.registerSubModel(new ContentDefinition(contentRepo, fileRepository));
        }
        if (priority != null){
            resourceRegistration.registerSubModel(new DeploymentOverlayDeploymentDefinition(priority));
        }
    }
}
