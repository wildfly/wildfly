package org.jboss.as.controller.transform;

import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public interface ResourceTransformer {
    /**
     * Transforms model based on current context and original model
     * @param context - context on where this is executed
     * @param model - original model that needs to be transformed
     * @return transformed model
     */
    ModelNode transformModel(TransformationContext context, ModelNode model);
}
