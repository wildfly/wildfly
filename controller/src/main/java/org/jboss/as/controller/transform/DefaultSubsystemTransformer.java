package org.jboss.as.controller.transform;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @deprecated not in use yet
 */
@Deprecated
class DefaultSubsystemTransformer implements ResourceTransformer {
    /*
    private List<TransformRule> getMappingRules(final ImmutableManagementResourceRegistration currentDefinition, final String subsystemName, int targetMajorVersion, int targetMinorVersion) {
        ManagementResourceRegistration targetDefinition = getSubsystemRegistration(subsystemName, targetMajorVersion, targetMinorVersion);
        if (targetDefinition == null) {
            return null;
        }
        return ModelMatcher.getRules(currentDefinition, targetDefinition);
    }*/

    @Override
    public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
        final ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
        childContext.processChildren(resource);
    }

}
