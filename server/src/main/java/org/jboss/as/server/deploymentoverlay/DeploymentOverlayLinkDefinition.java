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
public class DeploymentOverlayLinkDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition DEPLOYMENT =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.DEPLOYMENT, ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    static final SimpleAttributeDefinition REGULAR_EXPRESSION =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.REGULAR_EXPRESSION, ModelType.BOOLEAN, false)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode().set(false))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    static final SimpleAttributeDefinition DEPLOYMENT_OVERLAY =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = { DEPLOYMENT, DEPLOYMENT_OVERLAY, REGULAR_EXPRESSION };

    public static AttributeDefinition[] attributes() {
        return ATTRIBUTES.clone();
    }

    public DeploymentOverlayLinkDefinition(DeploymentOverlayPriority priority) {
        super(DeploymentOverlayModel.DEPLOYMENT_OVERRIDE_LINK_PATH,
                CommonDescriptions.getResourceDescriptionResolver(ModelDescriptionConstants.DEPLOYMENT_OVERLAY_LINK),
                new DeploymentOverlayLinkAdd(priority),
                new DeploymentOverlayLinkRemove(priority));
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attr, null);
        }
    }
}
