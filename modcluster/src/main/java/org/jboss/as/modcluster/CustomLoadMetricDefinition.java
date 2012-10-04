package org.jboss.as.modcluster;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class CustomLoadMetricDefinition extends SimpleResourceDefinition {

    protected static final CustomLoadMetricDefinition INSTANCE = new CustomLoadMetricDefinition();

    static final SimpleAttributeDefinition CLASS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CLASS, ModelType.STRING, false)
            .addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();


    static final AttributeDefinition[] ATTRIBUTES = {
            CLASS, LoadMetricDefinition.WEIGHT, LoadMetricDefinition.CAPACITY, LoadMetricDefinition.PROPERTY
    };

    private CustomLoadMetricDefinition() {
        super(ModClusterExtension.CUSTOM_LOAD_METRIC,
                ModClusterExtension.getResourceDescriptionResolver(CommonAttributes.CONFIGURATION, CommonAttributes.DYNAMIC_LOAD_PROVIDER, CommonAttributes.LOAD_METRIC),
                CustomLoadMetricAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler()
        );
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition def : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
        }
    }

}
