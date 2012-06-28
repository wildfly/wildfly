package org.jboss.as.controller.transform;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.SubsystemInformation;
import org.jboss.as.controller.registry.OperationTransformerRegistry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class TransformationTargetImpl implements TransformationTarget {

    private final ModelVersion version;
    private final ExtensionRegistry extensionRegistry;
    private final TransformerRegistry transformerRegistry;
    private final Map<String, String> subsystemVersions = Collections.synchronizedMap(new HashMap<String, String>());
    private final OperationTransformerRegistry operationTransformers;

    private TransformationTargetImpl(final ModelVersion version, final ModelNode subsystemVersions) {
        this.version = version;
        this.transformerRegistry = TransformerRegistry.getInstance();
        this.extensionRegistry = transformerRegistry.getExtensionRegistry();
        for (Property p : subsystemVersions.asPropertyList()) {
            this.subsystemVersions.put(p.getName(), p.getValue().asString());
        }
        this.operationTransformers = transformerRegistry.getSubsystemTransformers().resolve(version, subsystemVersions);
    }

    public static TransformationTargetImpl create(final int majorManagementVersion, final int minorManagementVersion,
                                                  int microManagementVersion, final ModelNode subsystemVersions) {
        return new TransformationTargetImpl(ModelVersion.create(majorManagementVersion, minorManagementVersion, microManagementVersion), subsystemVersions);
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
            if(ModelDescriptionConstants.COMPOSITE.equals(operationName)) {
                return new CompositeOperationTransformer();
            }
        } else if (address.size() > 1) {
            if(ModelDescriptionConstants.PROFILE.equals(address.getElement(0).getKey())) {
                final OperationTransformerRegistry.OperationTransformerEntry entry = operationTransformers.resolveOperationTransformer(address.subAddress(1), operationName);
                return entry.getTransformer();
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
        // Merge a new subsystem
        operationTransformers.mergeSubsystem(transformerRegistry.getSubsystemTransformers(), subsystemName, ModelVersion.create(majorVersion, minorVersion));
    }
}
