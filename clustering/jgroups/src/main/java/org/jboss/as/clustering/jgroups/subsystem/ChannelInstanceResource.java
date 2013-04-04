package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * Resource representing a run-time only channel instance.
 *
 * Some points to note:
 * - user cannot add/remove channels, so no need for add/remove handlers
 * - requires a backing custom resource ChannelInstanceCustomResource to define the children
 * - need a read-only handler to allow read access to attributes, which are obtained by the handler
 *   from the underlying services as and when required; they are never made persistent
 * - no write handler required
 * -
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ChannelInstanceResource extends SimpleResourceDefinition {

    public static final PathElement CHANNEL_PATH = PathElement.pathElement(MetricKeys.CHANNEL);
    private final boolean runtimeRegistration;

    // metrics
    static final SimpleAttributeDefinition STATE =
            new SimpleAttributeDefinitionBuilder(MetricKeys.STATE, ModelType.STRING, true)
                    .setStorageRuntime()
                    .build();
    static final SimpleAttributeDefinition VIEW =
            new SimpleAttributeDefinitionBuilder(MetricKeys.VIEW, ModelType.STRING, true)
                    .setStorageRuntime()
                    .build();

    static final AttributeDefinition[] CHANNEL_METRICS = {STATE, VIEW};

    public ChannelInstanceResource(boolean runtimeRegistration) {
        super(CHANNEL_PATH,
                JGroupsExtension.getResourceDescriptionResolver(MetricKeys.CHANNEL),
                null,
                null);
        this.runtimeRegistration = runtimeRegistration;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // register any metrics and the read-only handler
        if (runtimeRegistration) {
            for (AttributeDefinition attr : CHANNEL_METRICS) {
                resourceRegistration.registerMetric(attr, ChannelMetricsHandler.INSTANCE);
            }
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }
}
