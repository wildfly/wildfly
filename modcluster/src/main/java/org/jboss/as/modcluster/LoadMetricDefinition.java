package org.jboss.as.modcluster;

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class LoadMetricDefinition extends SimpleResourceDefinition {

    protected static final LoadMetricDefinition INSTANCE = new LoadMetricDefinition();

    static final SimpleAttributeDefinition TYPE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.TYPE, ModelType.STRING, false)
            .addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new EnumValidator<LoadMetricEnum>(LoadMetricEnum.class, false, false))
            .build();

    static final SimpleAttributeDefinition WEIGHT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.WEIGHT, ModelType.INT, true)
            .addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(LoadMetric.DEFAULT_WEIGHT))
            .build();
    static final SimpleAttributeDefinition CAPACITY = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CAPACITY, ModelType.DOUBLE, true)
            .addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(LoadMetric.DEFAULT_CAPACITY))
            .build();
    static final SimpleAttributeDefinition PROPERTY = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PROPERTY, ModelType.PROPERTY, true)
            .addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = {
            TYPE, WEIGHT, CAPACITY, PROPERTY
    };

    private LoadMetricDefinition() {
        super(ModClusterExtension.LOAD_METRIC,
                ModClusterExtension.getResourceDescriptionResolver(CommonAttributes.CONFIGURATION, CommonAttributes.DYNAMIC_LOAD_PROVIDER, CommonAttributes.LOAD_METRIC),
                LoadMetricAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler()
        );
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (SimpleAttributeDefinition def : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
        }
    }

}
