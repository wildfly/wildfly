package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Base class for cache resources which require common cache attributes, clustered cache attributes
 * and shared cache attributes.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class SharedCacheResource extends ClusteredCacheResource {

    public SharedCacheResource(PathElement pathElement, ResourceDescriptionResolver descriptionResolver, AbstractAddStepHandler addHandler, OperationStepHandler removeHandler) {
        super(pathElement, descriptionResolver, addHandler, removeHandler);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        // no attributes
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);

        resourceRegistration.registerSubModel(new StateTransferResource());
    }
}
