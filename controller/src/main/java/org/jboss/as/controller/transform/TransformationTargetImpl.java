package org.jboss.as.controller.transform;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.SubsystemInformation;
import org.jboss.as.controller.registry.OperationTransformerRegistry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class TransformationTargetImpl implements TransformationTarget {

    private final ModelVersion version;
    private final ExtensionRegistry extensionRegistry;
    private final TransformerRegistry transformerRegistry;
    private final Map<String, ModelVersion> subsystemVersions = Collections.synchronizedMap(new HashMap<String, ModelVersion>());
    private final OperationTransformerRegistry operationTransformers;

    private TransformationTargetImpl(final TransformerRegistry transformerRegistry, final ModelVersion version, final Map<PathAddress, ModelVersion> subsystemVersions, final OperationTransformerRegistry transformers) {
        this.version = version;
        this.transformerRegistry = transformerRegistry;
        this.extensionRegistry = transformerRegistry.getExtensionRegistry();
        for (Map.Entry<PathAddress, ModelVersion> p : subsystemVersions.entrySet()) {
            final String name = p.getKey().getLastElement().getValue();
            this.subsystemVersions.put(name, p.getValue());
        }
        this.operationTransformers = transformers;
    }

    public static TransformationTargetImpl create(final TransformerRegistry transformerRegistry, final ModelVersion version, final ModelNode subsystems, final TransformationTargetType type) {
        return create(transformerRegistry, version, TransformerRegistry.resolveVersions(subsystems), type);
    }

    public static TransformationTargetImpl create(final TransformerRegistry transformerRegistry, final ModelVersion version, final Map<PathAddress, ModelVersion> subsystems, final TransformationTargetType type) {
        final OperationTransformerRegistry registry;
        switch (type) {
            case SERVER:
                registry = transformerRegistry.resolveServer(version, subsystems);
                break;
            default:
                registry = transformerRegistry.resolveHost(version, subsystems);
        }
        return new TransformationTargetImpl(transformerRegistry, version, subsystems, registry);
    }

    @Deprecated
    public static TransformationTargetImpl create(final TransformerRegistry transformerRegistry, final int majorManagementVersion, final int minorManagementVersion,
                                                  int microManagementVersion, final ModelNode subsystemVersions) {
        return create(transformerRegistry, ModelVersion.create(majorManagementVersion, minorManagementVersion, microManagementVersion), subsystemVersions, TransformationTargetType.HOST);
    }

    @Override
    public ModelVersion getVersion() {
        return version;
    }

    @Override
    public ModelVersion getSubsystemVersion(String subsystemName) {
        return subsystemVersions.get(subsystemName);
    }

    public SubsystemInformation getSubsystemInformation(String subsystemName) {
        return extensionRegistry.getSubsystemInfo(subsystemName);
    }

    @Override
    public ResourceTransformer resolveTransformer(final PathAddress address) {
        OperationTransformerRegistry.ResourceTransformerEntry entry = operationTransformers.resolveResourceTransformer(address);
        if(entry == null) {
            return ResourceTransformer.DEFAULT;
        }
        return entry.getTransformer();
    }

    @Override
    public OperationTransformer resolveTransformer(final PathAddress address, final String operationName) {
        if(address.size() == 0) {
            // TODO use operationTransformers registry to register this operations.
            if(ModelDescriptionConstants.COMPOSITE.equals(operationName)) {
                return new CompositeOperationTransformer();
            }
        }
        final OperationTransformerRegistry.OperationTransformerEntry entry = operationTransformers.resolveOperationTransformer(address, operationName);
        return entry.getTransformer();
    }

    @Override
    public void addSubsystemVersion(String subsystemName, int majorVersion, int minorVersion) {
        addSubsystemVersion(subsystemName, ModelVersion.create(majorVersion, minorVersion));
    }

    @Override
    public void addSubsystemVersion(final String subsystemName, final ModelVersion version) {
        this.subsystemVersions.put(subsystemName, version);
        transformerRegistry.addSubsystem(operationTransformers, subsystemName, version);
    }

}
