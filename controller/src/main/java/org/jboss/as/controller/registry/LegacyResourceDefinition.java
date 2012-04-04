package org.jboss.as.controller.registry;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class LegacyResourceDefinition implements ResourceDefinition {
    private Map<String, AttributeAccess> attributes = new HashMap<String, AttributeAccess>();
    private List<ResourceDefinition> children = new LinkedList<ResourceDefinition>();
    private final PathAddress address;
    private final ModelNode description;

    public LegacyResourceDefinition(ModelNode modelDescription) {
        this.description = modelDescription.get(ModelDescriptionConstants.MODEL_DESCRIPTION);
        ModelNode attributes = description.has(ModelDescriptionConstants.ATTRIBUTES) ? description.get(ModelDescriptionConstants.ATTRIBUTES) : new ModelNode();
        address = PathAddress.pathAddress(modelDescription.get(ModelDescriptionConstants.ADDRESS));

        if (attributes.isDefined()) {
            for (Property property : attributes.asPropertyList()) {
                String name = property.getName();
                SimpleAttributeDefinition def = SimpleAttributeDefinitionBuilder.create(name, property.getValue()).build();
                this.attributes.put(name, new AttributeAccess(
                        AttributeAccess.AccessType.READ_ONLY, AttributeAccess.Storage.CONFIGURATION, null, null, def, null)
                );
            }
        }
        ModelNode children = modelDescription.get(ModelDescriptionConstants.CHILDREN);
        if (!children.isDefined()) {
            return;
        }
        for (ModelNode child : children.asList()) {
            this.children.add(new LegacyResourceDefinition(child));
        }
        description.remove(ModelDescriptionConstants.CHILDREN);
    }

    /**
     * Gets the path element that describes how to navigate to this resource from its parent resource, or {@code null}
     * if this is a definition of a root resource.
     *
     * @return the path element, or {@code null} if this is a definition of a root resource.
     */
    @Override
    public PathElement getPathElement() {
        return address.getLastElement();
    }

    /**
     * Gets a {@link org.jboss.as.controller.descriptions.DescriptionProvider} for the given resource.
     *
     * @param resourceRegistration the resource. Cannot be {@code null}
     * @return the description provider. Will not be {@code null}
     */
    @Override
    public DescriptionProvider getDescriptionProvider(ImmutableManagementResourceRegistration resourceRegistration) {
        return new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return description;
            }
        };
    }

    /**
     * Register operations associated with this resource.
     *
     * @param resourceRegistration a {@link org.jboss.as.controller.registry.ManagementResourceRegistration} created from this definition
     */
    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {

    }

    /**
     * Register operations associated with this resource.
     *
     * @param resourceRegistration a {@link org.jboss.as.controller.registry.ManagementResourceRegistration} created from this definition
     */
    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeAccess attr : attributes.values()) {
            resourceRegistration.registerReadOnlyAttribute(attr.getAttributeDefinition(), null);
        }
    }

    /**
     * Register child resources associated with this resource.
     *
     * @param resourceRegistration a {@link org.jboss.as.controller.registry.ManagementResourceRegistration} created from this definition
     */
    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        for (ResourceDefinition rd : children) {
            resourceRegistration.registerSubModel(rd);
        }
    }
}

