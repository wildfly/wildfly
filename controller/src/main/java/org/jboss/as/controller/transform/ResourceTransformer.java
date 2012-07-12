package org.jboss.as.controller.transform;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;

/**
 * The resource transformer.
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public interface ResourceTransformer {

    /**
     * Transform a resource.
     *
     * @param context the resource transformation context
     * @param address the path address
     * @param resource the resource to transform
     * @throws OperationFailedException
     */
    void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException;

    ResourceTransformer DEFAULT = new ResourceTransformer() {
        @Override
        public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException  {
            if (resource.isProxy() || resource.isRuntime()) {
                return;
            }
            final ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
            childContext.processChildren(resource);
        }
    };

    ResourceTransformer DISCARD = new ResourceTransformer() {
        @Override
        public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) {
            // nothing
        }
    };

}
