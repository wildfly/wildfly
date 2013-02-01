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

package org.jboss.as.controller.transform;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ModelVersionRange;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.GlobalTransformerRegistry;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationTransformerRegistry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Global transformers registry.
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author Emanuel Muckenhuber
 */
public final class TransformerRegistry {

    public static final ModelNode DISCARD_OPERATION = new ModelNode();
    static {
        DISCARD_OPERATION.get(OP).set("discard");
        DISCARD_OPERATION.get(OP_ADDR).setEmptyList();
        DISCARD_OPERATION.protect();
    }

    private final ExtensionRegistry extensionRegistry;

    private static final PathElement HOST = PathElement.pathElement(ModelDescriptionConstants.HOST);
    private static final PathElement PROFILE = PathElement.pathElement(ModelDescriptionConstants.PROFILE);
    private static final PathElement SERVER = PathElement.pathElement(ModelDescriptionConstants.RUNNING_SERVER);

    private final GlobalTransformerRegistry domain = new GlobalTransformerRegistry();
    private final GlobalTransformerRegistry subsystem = new GlobalTransformerRegistry();

    TransformerRegistry(final ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
        // Initialize the empty paths
        domain.createChildRegistry(PathAddress.pathAddress(PROFILE), ModelVersion.create(0), ResourceTransformer.DEFAULT, false);
        domain.createChildRegistry(PathAddress.pathAddress(HOST), ModelVersion.create(0), ResourceTransformer.DEFAULT, false);
        domain.createChildRegistry(PathAddress.pathAddress(HOST, SERVER), ModelVersion.create(0), ResourceTransformer.DEFAULT, false);
    }

    public ExtensionRegistry getExtensionRegistry() {
        return extensionRegistry;
    }

    /**
     * Register a subsystem transformer.
     *
     * @param name the subsystem name
     * @param range the version range
     * @param subsystemTransformer the resource transformer
     * @return the sub registry
     */
    public TransformersSubRegistration registerSubsystemTransformers(final String name, final ModelVersionRange range, final ResourceTransformer subsystemTransformer) {
        return  registerSubsystemTransformers(name, range, subsystemTransformer, OperationTransformer.DEFAULT);
    }

    /**
     * Register a subsystem transformer.
     *
     * @param name the subsystem name
     * @param range the version range
     * @param subsystemTransformer the resource transformer
     * @param operationTransformer the operation transformer
     * @return the sub registry
     */
    public TransformersSubRegistration registerSubsystemTransformers(final String name, final ModelVersionRange range, final ResourceTransformer subsystemTransformer, final OperationTransformer operationTransformer) {
        final PathAddress subsystemAddress = PathAddress.EMPTY_ADDRESS.append(PathElement.pathElement(SUBSYSTEM, name));
        for(final ModelVersion version : range.getVersions()) {
            subsystem.createChildRegistry(subsystemAddress, version, subsystemTransformer, operationTransformer);
        }
        return new TransformersSubRegistrationImpl(range, subsystem, subsystemAddress);
    }

    /**
     * Get the sub registry for the domain.
     *
     * @param range the version range
     * @return the sub registry
     */
    public TransformersSubRegistration getDomainRegistration(final ModelVersionRange range) {
        final PathAddress address = PathAddress.EMPTY_ADDRESS;
        return new TransformersSubRegistrationImpl(range, domain, address);
    }

    /**
     * Get the sub registry for the hosts.
     *
     * @param range the version range
     * @return the sub registry
     */
    public TransformersSubRegistration getHostRegistration(final ModelVersionRange range) {
        final PathAddress address = PathAddress.EMPTY_ADDRESS.append(HOST);
        return new TransformersSubRegistrationImpl(range, domain, address);
    }

    /**
     * Get the sub registry for the servers.
     *
     * @param range the version range
     * @return the sub registry
     */
    public TransformersSubRegistration getServerRegistration(final ModelVersionRange range) {
        final PathAddress address = PathAddress.EMPTY_ADDRESS.append(HOST, SERVER);
        return new TransformersSubRegistrationImpl(range, domain, address);
    }

    /**
     * Resolve the host registry.
     *
     * @param mgmtVersion the mgmt version
     * @param subsystems the subsystems
     * @return the transformer registry
     */
    public OperationTransformerRegistry resolveHost(final ModelVersion mgmtVersion, final ModelNode subsystems) {
        return resolveHost(mgmtVersion, resolveVersions(subsystems));

    }

    /**
     * Resolve the host registry.
     *
     * @param mgmtVersion the mgmt version
     * @param subsystems the subsystems
     * @return the transformer registry
     */
    public OperationTransformerRegistry resolveHost(final ModelVersion mgmtVersion, final Map<PathAddress, ModelVersion> subsystems) {
        // The domain / host / servers
        final OperationTransformerRegistry root = domain.create(mgmtVersion, Collections.<PathAddress, ModelVersion>emptyMap());
        subsystem.mergeSubtree(root, PathAddress.pathAddress(PROFILE), subsystems);
        subsystem.mergeSubtree(root, PathAddress.pathAddress(HOST, SERVER), subsystems);
        return root;
    }

    /**
     * Resolve the server registry.
     *
     * @param mgmtVersion the mgmt version
     * @param subsystems the subsystems
     * @return the transformer registry
     */
    public OperationTransformerRegistry resolveServer(final ModelVersion mgmtVersion, final ModelNode subsystems) {
        return resolveServer(mgmtVersion, resolveVersions(subsystems));
    }

    /**
     * Resolve the server registry.
     *
     * @param mgmtVersion the mgmt version
     * @param subsystems the subsystems
     * @return the transformer registry
     */
    public OperationTransformerRegistry resolveServer(final ModelVersion mgmtVersion, final Map<PathAddress, ModelVersion> subsystems) {
        // this might not be all that useful after all, since the operation to remote servers go through the host proxies anyway
        final OperationTransformerRegistry root = domain.create(mgmtVersion, Collections.<PathAddress, ModelVersion>emptyMap());
        return subsystem.mergeSubtree(root, PathAddress.pathAddress(HOST, SERVER), subsystems);
    }

    /**
     * Add a new subsystem to a given registry.
     *
     * @param registry the registry
     * @param name the subsystem name
     * @param version the version
     */
    void addSubsystem(final OperationTransformerRegistry registry, final String name, final ModelVersion version) {
        final OperationTransformerRegistry profile = registry.getChild(PathAddress.pathAddress(PROFILE));
        final OperationTransformerRegistry server = registry.getChild(PathAddress.pathAddress(HOST, SERVER));
        final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, name));
        subsystem.mergeSubtree(profile, Collections.singletonMap(address, version));
        if(server != null) {
            subsystem.mergeSubtree(server, Collections.singletonMap(address, version));
        }
    }

    public static Map<PathAddress, ModelVersion> resolveVersions(ExtensionRegistry extensionRegistry) {

        final ModelNode subsystems = new ModelNode();
        for (final String extension : extensionRegistry.getExtensionModuleNames()) {
            extensionRegistry.recordSubsystemVersions(extension, subsystems);
        }
        return resolveVersions(subsystems);
    }

    public static Map<PathAddress, ModelVersion> resolveVersions(final ModelNode subsystems) {
        final PathAddress base = PathAddress.EMPTY_ADDRESS;
        final Map<PathAddress, ModelVersion> versions = new HashMap<PathAddress, ModelVersion>();
        for(final Property property : subsystems.asPropertyList()) {
            final String name = property.getName();
            final PathAddress address = base.append(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, name));
            versions.put(address, ModelVersion.fromString(property.getValue().asString()));
        }
        return versions;
    }

    static ModelVersion convert(final String version) {
        final String[] s = version.split("\\.");
        final int length = s.length;
        if(length > 3) {
            throw new IllegalStateException();
        }
        int major = Integer.valueOf(s[0]);
        int minor = length > 1 ? Integer.valueOf(s[1]) : 0;
        int micro = length == 3 ? Integer.valueOf(s[2]) : 0;
        return ModelVersion.create(major, minor, micro);
    }

    public static ResourceDefinition loadSubsystemDefinition(final String subsystemName, final ModelVersion version) {
        return TransformationUtils.loadSubsystemDefinition(subsystemName, version);
    }

    public static Resource modelToResource(final ImmutableManagementResourceRegistration reg, final ModelNode model, boolean includeUndefined) {
        return TransformationUtils.modelToResource(PathAddress.EMPTY_ADDRESS, reg, model, includeUndefined);
    }

    public static class Factory {

        /**
         * Create a new Transformer registry.
         *
         * @param extensionRegistry the extension registry
         * @return the created transformer registry
         */
        public static TransformerRegistry create(ExtensionRegistry extensionRegistry) {
            return new TransformerRegistry(extensionRegistry);
        }

    }

    public static class TransformersSubRegistrationImpl implements TransformersSubRegistration {

        private final PathAddress current;
        private final ModelVersionRange range;
        private final GlobalTransformerRegistry registry;

        public TransformersSubRegistrationImpl(ModelVersionRange range, GlobalTransformerRegistry registry, PathAddress parent) {
            this.range = range;
            this.registry = registry;
            this.current = parent;
        }

        @Override
        public TransformersSubRegistration registerSubResource(PathElement element) {
            return registerSubResource(element, ResourceTransformer.DEFAULT, OperationTransformer.DEFAULT);
        }

        @Override
        public TransformersSubRegistration registerSubResource(PathElement element, boolean discard) {
            if(discard) {
                final PathAddress address = current.append(element);
                for(final ModelVersion version : range.getVersions()) {
                    registry.createDiscardingChildRegistry(address, version);
                }
                return new TransformersSubRegistrationImpl(range, registry, address);
            }
            return registerSubResource(element, ResourceTransformer.DEFAULT, OperationTransformer.DEFAULT);
        }

        @Override
        public TransformersSubRegistration registerSubResource(PathElement element, OperationTransformer operationTransformer) {
            return registerSubResource(element, ResourceTransformer.DEFAULT, operationTransformer);
        }

        @Override
        public TransformersSubRegistration registerSubResource(PathElement element, ResourceTransformer resourceTransformer) {
            return registerSubResource(element, resourceTransformer, OperationTransformer.DEFAULT);
        }

        @Override
        public TransformersSubRegistration registerSubResource(PathElement element, CombinedTransformer transformer) {
            return registerSubResource(element, transformer, transformer);
        }

        @Override
        public TransformersSubRegistration registerSubResource(PathElement element, ResourceTransformer resourceTransformer, OperationTransformer operationTransformer) {
            return registerSubResource(element, PathAddressTransformer.DEFAULT, resourceTransformer, operationTransformer);
        }

        @Override
        public TransformersSubRegistration registerSubResource(PathElement element, PathAddressTransformer pathAddressTransformer, ResourceTransformer resourceTransformer, OperationTransformer operationTransformer) {
            final PathAddress address = current.append(element);
            for(final ModelVersion version : range.getVersions()) {
                registry.createChildRegistry(address, version, pathAddressTransformer, resourceTransformer, operationTransformer);
            }
            return new TransformersSubRegistrationImpl(range, registry, address);
        }

        @Override
        public void discardOperations(String... operationNames) {
            for(final ModelVersion version : range.getVersions()) {
                for(final String operationName : operationNames) {
                    registry.discardOperation(current, version, operationName);
                }
            }
        }

        @Override
        public void registerOperationTransformer(String operationName, OperationTransformer transformer) {
            for(final ModelVersion version : range.getVersions()) {
                registry.registerTransformer(current, version, operationName, transformer);
            }
        }
    }

}
