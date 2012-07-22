package org.jboss.as.controller.transform;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ResourceDefinition;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public abstract class AbstractSubsystemTransformer extends AbstractResourceModelTransformer implements SubsystemTransformer {

    protected AbstractSubsystemTransformer(final ResourceDefinitionLoader loader) {
        super(loader);
    }

    protected AbstractSubsystemTransformer(final String subsystemName) {
        this(new ResourceDefinitionLoader() {
            @Override
            public ResourceDefinition load(TransformationTarget target) {
                final ModelVersion version = target.getSubsystemVersion(subsystemName);
                return TransformationUtils.loadSubsystemDefinition(subsystemName, version);
            }
        });
    }

    protected AbstractSubsystemTransformer(final ResourceDefinition definition) {
        this(new ResourceDefinitionLoader() {
            @Override
            public ResourceDefinition load(TransformationTarget target) {
                return definition;
            }
        });
    }

}
