package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.transform.AbstractSubsystemTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class InfinispanSubsystemTransformer_1_3 extends AbstractSubsystemTransformer {

    public InfinispanSubsystemTransformer_1_3() {
        super(1, 3);
    }

    @Override
    public ModelNode transformModel(TransformationContext context, ModelNode model) {
        for (Property p : model.get(ModelKeys.CACHE_CONTAINER).asPropertyList()) {
            ModelNode container = p.getValue();
            container.remove(ModelKeys.MODULE);
            for (Property cache : container.asPropertyList()) {
                transformCache(cache.getValue());
            }
            model.get(ModelKeys.CACHE_CONTAINER, p.getName()).set(container);
        }
        return model;
    }

    private void transformCache(final ModelNode cache) {
       cache.remove(ModelKeys.INDEXING_PROPERTIES);
    }
}
