package org.jboss.as.server.deploymentoverlay;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ChainedParameterValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.ServerMessages;
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
                    .setValidator(
                            new ChainedParameterValidator( new StringLengthValidator(1, Integer.MAX_VALUE, false, true),
                            new WildcardValidator()))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    static final SimpleAttributeDefinition DEPLOYMENT_OVERLAY =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = { DEPLOYMENT, DEPLOYMENT_OVERLAY };

    public static AttributeDefinition[] attributes() {
        return ATTRIBUTES.clone();
    }

    public DeploymentOverlayLinkDefinition(DeploymentOverlayPriority priority) {
        super(DeploymentOverlayModel.DEPLOYMENT_OVERRIDE_LINK_PATH,
                DeploymentOverlayModel.getResourceDescriptionResolver(ModelDescriptionConstants.DEPLOYMENT_OVERLAY_LINK),
                new DeploymentOverlayLinkAdd(priority),
                new DeploymentOverlayLinkRemove(priority));
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attr, null);
        }
    }

    private static class WildcardValidator implements ParameterValidator {

        @Override
        public void validateParameter(final String parameterName, final ModelNode value) throws OperationFailedException {
            if (value.isDefined() && value.getType() != ModelType.EXPRESSION) {
                String str = value.asString();
                for(int i = 1; i < str.length() -1; ++i) {
                    if(str.charAt(i) == '*') {
                        throw ServerMessages.MESSAGES.wildcardOnlyAllowedAtStartOrEnd(str);
                    }
                }
            }
        }

        @Override
        public void validateResolvedParameter(final String parameterName, final ModelNode value) throws OperationFailedException {
            validateParameter(parameterName, value);
        }
    }
}
