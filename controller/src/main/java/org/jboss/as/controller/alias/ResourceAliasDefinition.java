package org.jboss.as.controller.alias;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * A {@ResourceDefinition} for a resource that is just an alias for another resource. It delegates everything
 * to the {@link AliasedResourceDefinition} for the resource being aliased.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ResourceAliasDefinition implements ResourceDefinition {

    private final AliasedResourceDefinition delegate;
    private final PathElement alias;

    public ResourceAliasDefinition(PathElement path, AliasedResourceDefinition delegate) {
        this.alias = path;
        this.delegate = delegate;
    }

    @Override
    public PathElement getPathElement() {
        return alias;
    }

    @Override
    public DescriptionProvider getDescriptionProvider(ImmutableManagementResourceRegistration resourceRegistration) {
        return delegate.getAliasDescriptionProvider(resourceRegistration, alias);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        delegate.registerAliasOperations(resourceRegistration, alias);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        delegate.registerAliasAttributes(resourceRegistration, alias);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        delegate.registerAliasChildren(resourceRegistration, alias);
    }
}
