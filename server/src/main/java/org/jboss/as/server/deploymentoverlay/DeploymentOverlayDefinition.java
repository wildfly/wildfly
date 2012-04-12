package org.jboss.as.server.deploymentoverlay;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author Stuart Douglas
 */
public class DeploymentOverlayDefinition extends SimpleResourceDefinition {

    public static final DeploymentOverlayDefinition INSTANCE = new DeploymentOverlayDefinition();

    private static final AttributeDefinition[] ATTRIBUTES = { };

    public static final Map<String, AttributeDefinition> ATTRIBUTE_MAP;

    static {
        Map<String, AttributeDefinition> map = new LinkedHashMap<String, AttributeDefinition>();
        for(AttributeDefinition attr : ATTRIBUTES) {
            map.put(attr.getName(), attr);
        }

        ATTRIBUTE_MAP = Collections.unmodifiableMap(map);
    }

    public static AttributeDefinition[] attributes() {
        return ATTRIBUTES.clone();
    }

    private DeploymentOverlayDefinition() {
        super(DeploymentOverlayModel.DEPLOYMENT_OVERRIDE_PATH,
                DeploymentOverlayModel.getResourceDescriptionResolver(ModelDescriptionConstants.DEPLOYMENT_OVERLAY),
                DeploymentOverlayAdd.INSTANCE,
                DeploymentOverlayRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTE_MAP.values()) {
            resourceRegistration.registerReadOnlyAttribute(attr, null);
        }
    }
}
