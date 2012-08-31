package org.jboss.as.server.deploymentoverlay;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayPriority;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Links a deployment overlay to a deployment
 * @author Stuart Douglas
 */
public class DeploymentOverlayDeploymentDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition REGULAR_EXPRESSION =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.REGULAR_EXPRESSION, ModelType.BOOLEAN, false)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode().set(false))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();


    private static final AttributeDefinition[] ATTRIBUTES = { REGULAR_EXPRESSION };

    public static AttributeDefinition[] attributes() {
        return ATTRIBUTES.clone();
    }

    public DeploymentOverlayDeploymentDefinition(DeploymentOverlayPriority priority) {
        super(DeploymentOverlayModel.DEPLOYMENT_OVERRIDE_DEPLOYMENT_PATH,
                CommonDescriptions.getResourceDescriptionResolver(ModelDescriptionConstants.DEPLOYMENT_OVERLAY + "." + ModelDescriptionConstants.DEPLOYMENT),
                new DeploymentOverlayDeploymentAdd(priority),
                new DeploymentOverlayDeploymentRemove(priority));
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attr, null);
        }
    }
}
