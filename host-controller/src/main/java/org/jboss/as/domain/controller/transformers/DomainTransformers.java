/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.domain.controller.transformers;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.SubsystemInformation;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.AddNameFromAddressResourceTransformer;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformerRegistry;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Global transformation rules for the domain, host and server-config model.
 *
 * @author Emanuel Muckenhuber
 */
public class DomainTransformers {

    /** Dummy version for ignored subsystems. */
    static final ModelVersion IGNORED_SUBSYSTEMS = ModelVersion.create(-1);

    private static final String JSF_SUBSYSTEM = "jsf";
    private static final PathElement JSF_EXTENSION = PathElement.pathElement(ModelDescriptionConstants.EXTENSION, "org.jboss.as.jsf");

    //AS 7.1.2.Final
    private static final ModelVersion VERSION_1_2 = ModelVersion.create(1, 2, 0);
    //AS 7.1.3.Final
    private static final ModelVersion VERSION_1_3 = ModelVersion.create(1, 3, 0);
    //AS 7.2.0.Final
    private static final ModelVersion VERSION_1_4 = ModelVersion.create(1, 4, 0);
    /**
     * Initialize the domain registry.
     *
     * @param registry the domain registry
     */
    public static void initializeDomainRegistry(final TransformerRegistry registry) {

        initializeDomainRegistry(registry, VERSION_1_2);
        initializeDomainRegistry(registry, VERSION_1_3);
        initializeDomainRegistry(registry, VERSION_1_4);
    }

    private static void initializeDomainRegistry(TransformerRegistry registry, ModelVersion modelVersion) {
        TransformersSubRegistration domain = registry.getDomainRegistration(modelVersion);
        if (modelVersion == VERSION_1_2 || modelVersion == VERSION_1_3) {
            // Discard all operations to the newly introduced jsf extension
            domain.registerSubResource(JSF_EXTENSION, IGNORED_EXTENSIONS);
            // Ignore the jsf subsystem as well
            final String slotAttributeName = "default-jsf-impl-slot";
            final String slotDefaultValue = "main";
            registry.registerSubsystemTransformers(JSF_SUBSYSTEM, IGNORED_SUBSYSTEMS, new ResourceTransformer() {
                @Override
                public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {

                    ModelNode model = resource.getModel();
                    if (model.hasDefined(slotAttributeName)) {
                        ModelNode slot = model.get(slotAttributeName);
                        if (!slotDefaultValue.equals(slot.asString())) {
                            context.getLogger().logWarning(address, slotAttributeName, MESSAGES.invalidJSFSlotValue(slot.asString()));
                        }
                    }
                    Set<String> attributes = new HashSet<String>();
                    for (Property prop : resource.getModel().asPropertyList()) {
                        attributes.add(prop.getName());
                    }
                    attributes.remove(slotAttributeName);
                    if (!attributes.isEmpty()) {
                        context.getLogger().logWarning(address, ControllerMessages.MESSAGES.attributesAreNotUnderstoodAndWillBeIgnored(), attributes);
                    }
                }
            });
            TransformersSubRegistration jsfSubsystem = domain.registerSubResource(PathElement.pathElement(SUBSYSTEM, JSF_SUBSYSTEM));
            jsfSubsystem.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new OperationTransformer() {
                @Override
                public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
                    final String name = operation.require(NAME).asString();
                    final ModelNode value = operation.get(ModelDescriptionConstants.VALUE);
                    if (!slotAttributeName.equals(name)) {
                        context.getLogger().logWarning(address, ControllerMessages.MESSAGES.attributesAreNotUnderstoodAndWillBeIgnored(), name);
                        return DISCARD.transformOperation(context, address, operation);
                    }
                    if (value.isDefined() && value.equals(slotDefaultValue)) {
                        return DISCARD.transformOperation(context, address, operation);
                    } else {
                        OperationRejectionPolicy rejectionPolicy = new OperationRejectionPolicy() {
                            @Override
                            public boolean rejectOperation(ModelNode preparedResult) {
                                return true;
                            }

                            @Override
                            public String getFailureDescription() {
                                return MESSAGES.invalidJSFSlotValue(value.asString());
                            }
                        };
                        return new TransformedOperation(operation, rejectionPolicy, OperationResultTransformer.ORIGINAL_RESULT);
                    }
                }
            });
            jsfSubsystem.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, new OperationTransformer() {
                @Override
                public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
                    String attributeName = operation.require(NAME).asString();
                    if (!slotAttributeName.equals(attributeName)) {
                        return DEFAULT.transformOperation(context, address, operation);
                    } else {
                        context.getLogger().logWarning(address, ControllerMessages.MESSAGES.attributesAreNotUnderstoodAndWillBeIgnored(), attributeName);
                        return DISCARD.transformOperation(context, address, operation);
                    }
                }
            });

            PathsTransformers.registerTransformers120(domain);
            DeploymentTransformers.registerTransformers120(domain);
            SystemPropertyTransformers.registerTransformers120(domain);
            SocketBindingGroupTransformers.registerTransformers120(domain);
            ServerGroupTransformers.registerTransformers120(domain);

            //Add the domain interface and path name. This is from a read attribute handler but in < 1.4.0 it existed in the model
            domain.registerSubResource(PathElement.pathElement(INTERFACE), AddNameFromAddressResourceTransformer.INSTANCE);
            domain.registerSubResource(PathElement.pathElement(PATH), AddNameFromAddressResourceTransformer.INSTANCE);

        } else if (modelVersion == VERSION_1_4) {
            //TODO not sure if these should be handled here for 1.4.0 or if it is better in the tests?
            //Add the domain interface name. This is currently from a read attribute handler but in < 1.4.0 it existed in the model
            domain.registerSubResource(PathElement.pathElement(INTERFACE), AddNameFromAddressResourceTransformer.INSTANCE);
            domain.registerSubResource(PathElement.pathElement(PATH), AddNameFromAddressResourceTransformer.INSTANCE);
        }
    }

    private static final ResourceTransformer IGNORED_EXTENSIONS = new IgnoreExtensionResourceTransformer();

    /**
     * Special resource transformer automatically ignoring all subsystems registered by an extension.
     */
    static class IgnoreExtensionResourceTransformer implements ResourceTransformer {

        @Override
        public void transformResource(final ResourceTransformationContext context, final PathAddress address, final Resource resource) throws OperationFailedException {
            // we just ignore this resource  - so don't add it: context.addTransformedResource(...)
            final PathElement element = address.getLastElement();

            final TransformationTarget target = context.getTarget();
            final ExtensionRegistry registry = target.getExtensionRegistry();

            final Map<String, SubsystemInformation> subsystems = registry.getAvailableSubsystems(element.getValue());
            if(subsystems != null) {
                for(final Map.Entry<String, SubsystemInformation> subsystem : subsystems.entrySet()) {
                    final String name = subsystem.getKey();
                    target.addSubsystemVersion(name, IGNORED_SUBSYSTEMS);
                }
            }
        }
    }
}
