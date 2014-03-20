package org.jboss.as.controller;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A persistent resource definition. This needs to be combined with {@link PersistentResourceXMLDescription} to
 * simplify the process of creating parsers and persisters.
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public abstract class PersistentResourceDefinition extends SimpleResourceDefinition {

    protected PersistentResourceDefinition(PathElement pathElement, ResourceDescriptionResolver descriptionResolver) {
        super(pathElement, descriptionResolver);
    }

    protected PersistentResourceDefinition(PathElement pathElement, ResourceDescriptionResolver descriptionResolver, OperationStepHandler addHandler, OperationStepHandler removeHandler) {
        super(pathElement, descriptionResolver, addHandler, removeHandler);
    }

    protected PersistentResourceDefinition(PathElement pathElement, ResourceDescriptionResolver descriptionResolver, OperationStepHandler addHandler, OperationStepHandler removeHandler, OperationEntry.Flag addRestartLevel, OperationEntry.Flag removeRestartLevel) {
        super(pathElement, descriptionResolver, addHandler, removeHandler, addRestartLevel, removeRestartLevel);
    }


    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        for (PersistentResourceDefinition child : getChildren()) {
            resourceRegistration.registerSubModel(child);
        }
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(getAttributes());
        for (AttributeDefinition attr : getAttributes()) {
            resourceRegistration.registerReadWriteAttribute(attr, null, handler);
        }
    }

    protected List<? extends PersistentResourceDefinition> getChildren() {
        return Collections.emptyList();
    }

    public abstract Collection<AttributeDefinition> getAttributes();
}
