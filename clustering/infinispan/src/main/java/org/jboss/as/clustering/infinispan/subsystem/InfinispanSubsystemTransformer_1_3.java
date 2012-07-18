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
        super("infinispan");
    }

    @Override
    public ModelNode transformModel(TransformationContext context, ModelNode model) {
        for (Property p : model.get(ModelKeys.CACHE_CONTAINER).asPropertyList()) {
            transformCache(model, p.getName(), ModelKeys.DISTRIBUTED_CACHE);
            transformCache(model, p.getName(), ModelKeys.REPLICATED_CACHE);
            transformCache(model, p.getName(), ModelKeys.INVALIDATION_CACHE);
            transformCache(model, p.getName(), ModelKeys.LOCAL_CACHE);
        }
        return model;
    }

    private void transformCache(final ModelNode model, final String containerName, final String cacheType) {
        ModelNode cacheHolder = model.get(ModelKeys.CACHE_CONTAINER, containerName, cacheType);
        if (!cacheHolder.isDefined()) { return; }
        for (Property c : cacheHolder.asPropertyList()) {
            ModelNode cache = c.getValue();

            if (cache.has(ModelKeys.INDEXING_PROPERTIES)) {
                cache.remove(ModelKeys.INDEXING_PROPERTIES);
            }
            model.get(ModelKeys.CACHE_CONTAINER, containerName, cacheType, c.getName()).set(cache);
        }
    }
}
