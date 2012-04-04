package org.jboss.as.modcluster;

import org.jboss.as.controller.transform.AbstractSubsystemTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class ModClusterModelTransformer11 extends AbstractSubsystemTransformer {
    public ModClusterModelTransformer11() {
        super(1, 1);
    }

    @Override
    public ModelNode transformModel(TransformationContext context, final ModelNode model) {
        ModelNode subsystem = model.clone();
        ModelNode config = subsystem.get(CommonAttributes.MOD_CLUSTER_CONFIG, CommonAttributes.CONFIGURATION);
        config.get("domain").set(config.get(CommonAttributes.LOAD_BALANCING_GROUP));
        config.remove(CommonAttributes.LOAD_BALANCING_GROUP);
        ModelNode dynamic = config.get(CommonAttributes.DYNAMIC_LOAD_PROVIDER, CommonAttributes.CONFIGURATION);
        config.remove(CommonAttributes.DYNAMIC_LOAD_PROVIDER);
        dynamic.get(CommonAttributes.LOAD_METRIC).set(getLoadMetricList(dynamic, CommonAttributes.LOAD_METRIC));
        dynamic.get(CommonAttributes.CUSTOM_LOAD_METRIC).set(getLoadMetricList(dynamic, CommonAttributes.CUSTOM_LOAD_METRIC));

        config.get(CommonAttributes.DYNAMIC_LOAD_PROVIDER).set(dynamic);
        String factor = config.get(CommonAttributes.SIMPLE_LOAD_PROVIDER_FACTOR).asString();
        config.remove(CommonAttributes.SIMPLE_LOAD_PROVIDER_FACTOR);
        config.get(CommonAttributes.SIMPLE_LOAD_PROVIDER_FACTOR, CommonAttributes.FACTOR).set(factor);
        config.remove(CommonAttributes.CONNECTOR);
        return subsystem;
    }

    private List<ModelNode> getLoadMetricList(ModelNode dynamic, final String loadMetric) {
        List<ModelNode> list = new LinkedList<ModelNode>();
        if (!dynamic.hasDefined(loadMetric)) {
            return list;
        }
        for (Property property : dynamic.get(loadMetric).asPropertyList()) {
            ModelNode metric = property.getValue();
            for (Property p : metric.asPropertyList()) {
                if (p.getValue().isDefined() && !p.getName().equals(CommonAttributes.PROPERTY)) {
                    metric.get(p.getName()).set(p.getValue().asString());
                }
            }
            if (metric.hasDefined(CommonAttributes.PROPERTY)) {
                metric.get(CommonAttributes.PROPERTY).set(metric.get(CommonAttributes.PROPERTY).asList());

            }
            list.add(metric);
        }
        return list;
    }
}
