package org.jboss.as.controller.transform;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import java.util.List;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
class DefaultSubsystemTransformer implements ResourceTransformer {
    private ManagementResourceRegistration getSubsystemRegistration(final String subsystemName, int majorVersion, int minorVersion) {

        ResourceDefinition rd = TransformerRegistry.loadSubsystemDefinition(subsystemName, majorVersion, minorVersion);
        return ManagementResourceRegistration.Factory.create(rd);
    }

    private List<TransformRule> getMappingRules(final ImmutableManagementResourceRegistration currentDefinition, final String subsystemName, int targetMajorVersion, int targetMinorVersion) {
        ManagementResourceRegistration targetDefinition = getSubsystemRegistration(subsystemName, targetMajorVersion, targetMinorVersion);
        if (targetDefinition == null) {
            return null;
        }

        return ModelMatcher.getRules(currentDefinition, targetDefinition);
    }

    @Override
    public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) {
        final ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
        childContext.processChildren(resource);
    }

}
