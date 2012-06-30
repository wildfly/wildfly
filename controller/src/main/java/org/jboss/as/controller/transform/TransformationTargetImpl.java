package org.jboss.as.controller.transform;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.ControllerLogger;
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
    private final Map<String, String> subsystemVersions = Collections.synchronizedMap(new HashMap<String, String>());
    private final OperationTransformerRegistry operationTransformers;

    private TransformationTargetImpl(final TransformerRegistry transformerRegistry, final ModelVersion version, final ModelNode subsystemVersions, final OperationTransformerRegistry transformers) {
        this.version = version;
        this.transformerRegistry = transformerRegistry;
        this.extensionRegistry = transformerRegistry.getExtensionRegistry();
        for (Property p : subsystemVersions.asPropertyList()) {
            this.subsystemVersions.put(p.getName(), p.getValue().asString());
        }
        this.operationTransformers = transformers;
    }

    public static TransformationTargetImpl create(final TransformerRegistry transformerRegistry, final ModelVersion version, final ModelNode subsystems, final TransformationTargetType type) {
        final OperationTransformerRegistry registry;
        switch (type) {
            case SERVER:
                registry = transformerRegistry.getDomainTransformers().resolveServer(version, subsystems);
                break;
            default:
                registry = transformerRegistry.getDomainTransformers().resolveHost(version, subsystems);
        }
        return new TransformationTargetImpl(transformerRegistry, version, subsystems, registry);
    }

    @Deprecated
    public static TransformationTargetImpl create(TransformerRegistry transformerRegistry, final int majorManagementVersion, final int minorManagementVersion,
                                                  int microManagementVersion, final ModelNode subsystemVersions) {
        return create(transformerRegistry, ModelVersion.create(majorManagementVersion, minorManagementVersion, microManagementVersion), subsystemVersions, TransformationTargetType.HOST);
    }

    @Override
    public ModelVersion getVersion() {
        return version;
    }

    @Override
    public String getSubsystemVersion(String subsystemName) {
        return subsystemVersions.get(subsystemName);
    }

    @Override
    public SubsystemInformation getSubsystemInformation(String subsystemName) {
        return extensionRegistry.getSubsystemInfo(subsystemName);
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
    public SubsystemTransformer getSubsystemTransformer(String subsystemName) {
        if (!subsystemVersions.containsKey(subsystemName)) {
            return null;
        }
        SubsystemInformation info = getSubsystemInformation(subsystemName);

        String[] version = getSubsystemVersion(subsystemName).split("\\.");
        int major = Integer.parseInt(version[0]);
        int minor = Integer.parseInt(version[1]);
        int micro = version.length == 3 ? Integer.parseInt(version[2]) : 0;

        if (info.getManagementInterfaceMajorVersion() == major && info.getManagementInterfaceMinorVersion() == minor) {
            return null; //no need to transform
        }
        SubsystemTransformer t = transformerRegistry.getSubsystemTransformer(subsystemName, major, minor, micro);
        if (t == null) {
            ControllerLogger.ROOT_LOGGER.transformerNotFound(subsystemName, major, minor);
            //return defaultSubsystemTransformer?
        }
        return t;
    }

    public boolean isTransformationNeeded() {
        //return  !(major==org.jboss.as.version.Version.MANAGEMENT_MAJOR_VERSION&& minor == org.jboss.as.version.Version.MANAGEMENT_MINOR_VERSION); //todo dependencies issue
        final int major = version.getMajor();
        final int minor = version.getMinor();
        return !(major == 1 && minor == 2);
    }

    @Override
    public void addSubsystemVersion(String subsystemName, int majorVersion, int minorVersion) {
        StringBuilder sb = new StringBuilder(String.valueOf(majorVersion)).append('.').append(minorVersion);
        this.subsystemVersions.put(subsystemName, sb.toString());
        transformerRegistry.getDomainTransformers().addSubsystem(operationTransformers, subsystemName, ModelVersion.create(majorVersion, minorVersion));
    }
}
