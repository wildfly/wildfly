package org.jboss.as.server.deploymentoverlay;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.DeploymentFileRepository;
import org.jboss.dmr.ModelType;

/**
 * @author Stuart Douglas
 */
public class ContentDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition CONTENT =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.CONTENT, ModelType.BYTES, false)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = {CONTENT};

    public static AttributeDefinition[] attributes() {
        return ATTRIBUTES.clone();
    }

    public ContentDefinition(final ContentRepository contentRepository, final DeploymentFileRepository remoteRepository) {
        super(DeploymentOverlayModel.CONTENT_PATH,
                CommonDescriptions.getResourceDescriptionResolver(ModelDescriptionConstants.DEPLOYMENT_OVERLAY + "." + ModelDescriptionConstants.CONTENT, false),
                new ContentAdd(contentRepository, remoteRepository),
                ContentRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attr, null);
        }
    }
}
